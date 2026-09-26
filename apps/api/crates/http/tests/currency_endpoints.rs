//! Tests de integracion de los endpoints de monedas y tasas.
//!
//! Atraviesan el router entero: middleware, handler, caso de uso y DTO, con un
//! repositorio en memoria en lugar de PostgreSQL y sin llamar a Frankfurter.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

mod support;

use std::collections::BTreeMap;

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRate, RateBulletin};
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};
use support::Harness;

fn code(raw: &str) -> CurrencyCode {
    CurrencyCode::parse(raw).unwrap()
}

fn instant(raw: &str) -> DateTime<Utc> {
    DateTime::parse_from_rfc3339(raw)
        .unwrap()
        .with_timezone(&Utc)
}

/// Pasada de ejemplo: dos monedas cotizadas, un metal y una moneda sin tasa.
fn bulletin() -> RateBulletin {
    let rates = BTreeMap::from([
        (code("COP"), ExchangeRate::parse("3316.44").unwrap()),
        (code("EUR"), ExchangeRate::parse("0.87732").unwrap()),
        (code("XAU"), ExchangeRate::parse("0.00023").unwrap()),
    ]);
    let currencies = vec![
        Currency::new(code("COP"), "Colombian Peso", "$").unwrap(),
        Currency::new(code("EUR"), "Euro", "€").unwrap(),
        Currency::new(code("USD"), "United States Dollar", "$").unwrap(),
        Currency::new(code("JPY"), "Japanese Yen", "¥").unwrap(),
    ];
    RateBulletin::new(
        code("USD"),
        NaiveDate::from_ymd_opt(2026, 9, 25).unwrap(),
        rates,
        currencies,
    )
    .unwrap()
}

/// Monta el servicio con una pasada ya guardada en el instante dado.
fn harness_with_rates(fetched_at: &str) -> Harness {
    let harness = Harness::new();
    harness
        .exchange_rates
        .seed(&bulletin(), instant(fetched_at));
    harness
}

async fn sign_up(harness: &Harness, email: &str) -> String {
    let mut response = TestClient::post("http://localhost/v1/auth/register")
        .json(&json!({
            "name": "Camilo Rojas",
            "email": email,
            "password": "contrasena-larga",
            "acceptedTerms": true,
        }))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::CREATED));
    let body = response.take_json::<Value>().await.unwrap();
    body["accessToken"].as_str().unwrap().to_owned()
}

async fn get(harness: &Harness, path: &str, token: Option<&str>) -> (Option<StatusCode>, Value) {
    let mut request = TestClient::get(format!("http://localhost{path}"));
    if let Some(token) = token {
        request = request.add_header("authorization", format!("Bearer {token}"), true);
    }
    let mut response = request.send(&harness.service).await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

#[tokio::test]
async fn the_catalog_only_lists_what_can_be_chosen() {
    let harness = harness_with_rates("2026-09-25T06:00:00Z");
    let token = sign_up(&harness, "monedas@correo.com").await;

    let (status, body) = get(&harness, "/v1/currencies", Some(&token)).await;

    assert_eq!(status, Some(StatusCode::OK));
    let items = body["items"].as_array().unwrap();
    let codes: Vec<&str> = items
        .iter()
        .map(|item| item["code"].as_str().unwrap())
        .collect();
    assert_eq!(codes, ["COP", "EUR", "USD"], "sin XAU y sin JPY");
    assert_eq!(items[1]["name"], "Euro");
    assert_eq!(items[1]["symbol"], "€");
}

#[tokio::test]
async fn the_rates_travel_as_strings_with_the_base_at_one() {
    let harness = harness_with_rates("2026-09-25T06:00:12Z");
    let token = sign_up(&harness, "tasas@correo.com").await;

    let (status, body) = get(&harness, "/v1/exchange-rates", Some(&token)).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["base"], "USD");
    assert_eq!(body["date"], "2026-09-25");
    assert_eq!(body["fetchedAt"], "2026-09-25T06:00:12Z");
    assert_eq!(body["status"], "ok");
    assert_eq!(body["rates"]["USD"], "1");
    assert_eq!(body["rates"]["COP"], "3316.44");
    assert_eq!(body["rates"]["EUR"], "0.87732");
    assert!(
        body["rates"]["COP"].is_string(),
        "un numero suelto acabaria en un double en el cliente"
    );
    assert!(
        body["rates"].get("XAU").is_none(),
        "el oro no se ofrece y tampoco se cotiza"
    );
}

#[tokio::test]
async fn rates_that_have_not_been_refreshed_for_two_days_arrive_marked_stale() {
    // El reloj del harness esta fijo en 2026-09-22T10:00:00Z.
    let harness = harness_with_rates("2026-09-19T06:00:00Z");
    let token = sign_up(&harness, "rancias@correo.com").await;

    let (status, body) = get(&harness, "/v1/exchange-rates", Some(&token)).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["status"], "stale");
    assert_eq!(body["rates"]["COP"], "3316.44", "se sirven igualmente");
}

#[tokio::test]
async fn without_a_token_neither_endpoint_answers() {
    let harness = harness_with_rates("2026-09-22T06:00:00Z");

    for path in ["/v1/currencies", "/v1/exchange-rates"] {
        let (status, body) = get(&harness, path, None).await;
        assert_eq!(status, Some(StatusCode::UNAUTHORIZED), "{path}");
        assert_eq!(body["status"], 401, "{path}");
    }
}

#[tokio::test]
async fn two_accounts_see_the_very_same_catalog_and_rates() {
    // Las tasas y el catalogo son globales: aqui no hay nada acotado por usuario,
    // asi que el test de aislamiento de los demas contextos se convierte en este.
    let harness = harness_with_rates("2026-09-22T06:00:00Z");
    let first = sign_up(&harness, "primera@correo.com").await;
    let second = sign_up(&harness, "segunda@correo.com").await;

    let (first_status, first_rates) = get(&harness, "/v1/exchange-rates", Some(&first)).await;
    let (second_status, second_rates) = get(&harness, "/v1/exchange-rates", Some(&second)).await;
    let (_, first_catalog) = get(&harness, "/v1/currencies", Some(&first)).await;
    let (_, second_catalog) = get(&harness, "/v1/currencies", Some(&second)).await;

    assert_eq!(first_status, Some(StatusCode::OK));
    assert_eq!(second_status, Some(StatusCode::OK));
    assert_eq!(first_rates, second_rates);
    assert_eq!(first_catalog, second_catalog);
}

#[tokio::test]
async fn without_a_single_pass_the_rates_are_unavailable_and_not_a_server_error() {
    let harness = Harness::new();
    let token = sign_up(&harness, "vacio@correo.com").await;

    let (status, body) = get(&harness, "/v1/exchange-rates", Some(&token)).await;

    assert_eq!(status, Some(StatusCode::SERVICE_UNAVAILABLE));
    assert_eq!(body["status"], 503);
    assert_eq!(
        body["type"], "https://gastonomo.app/problems/service-unavailable",
        "el cuerpo sigue siendo problem+json"
    );
}

#[tokio::test]
async fn an_empty_catalog_is_an_empty_list_and_not_an_error() {
    let harness = Harness::new();
    let token = sign_up(&harness, "sincatalogo@correo.com").await;

    let (status, body) = get(&harness, "/v1/currencies", Some(&token)).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["items"].as_array().unwrap().len(), 0);
}
