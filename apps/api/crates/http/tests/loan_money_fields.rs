//! Los cuatro campos de importe de prestamos y abonos, vistos desde HTTP.
//!
//! Viven en su propio binario y no en `loan_endpoints.rs` porque ese fichero ya
//! roza el limite de 400 lineas de un fichero de test.

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

use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};
use support::Harness;

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
    let body = response.take_json::<Value>().await.expect("session body");
    body["accessToken"]
        .as_str()
        .expect("the session carries an access token")
        .to_owned()
}

async fn post(
    harness: &Harness,
    token: &str,
    path: &str,
    body: &Value,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post(format!("http://localhost{path}"))
        .bearer_auth(token.to_owned())
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

#[tokio::test]
async fn a_loan_and_its_payment_can_omit_the_equivalence_like_a_movement_does() {
    // El contrato de POST /v1/movements deja omitir base y baseCurrency juntos.
    // Aqui se comprueba que prestamos y abonos aceptan exactamente lo mismo: la
    // asimetria anterior obligaba a cada cliente a recordar dos reglas.
    let harness = Harness::new();
    let token = sign_up(&harness, "sinbase@correo.com").await;

    let (loan_status, loan) = post(
        &harness,
        &token,
        "/v1/loans",
        &json!({
            "person": "Rosa",
            "direction": "lent",
            "openedOn": "2026-06-28",
            "amount": "600000",
            "currency": "COP",
        }),
    )
    .await;

    assert_eq!(loan_status, Some(StatusCode::CREATED));
    assert_eq!(loan["base"], "600000.0000");
    assert_eq!(loan["baseCurrency"], "COP");
    assert_eq!(loan["note"], "", "la nota omitida se guarda vacia");

    let loan_id = loan["id"].as_str().expect("the loan carries an id");
    let (payment_status, updated) = post(
        &harness,
        &token,
        &format!("/v1/loans/{loan_id}/payments"),
        &json!({
            "paidOn": "2026-07-29",
            "amount": "200000",
            "currency": "COP",
        }),
    )
    .await;

    assert_eq!(payment_status, Some(StatusCode::CREATED));
    assert_eq!(updated["payments"][0]["base"], "200000.0000");
    assert_eq!(updated["payments"][0]["baseCurrency"], "COP");
}

#[tokio::test]
async fn half_an_equivalence_in_a_loan_is_a_validation_error_on_base() {
    let harness = Harness::new();
    let token = sign_up(&harness, "mediabase@correo.com").await;

    let (status, body) = post(
        &harness,
        &token,
        "/v1/loans",
        &json!({
            "person": "Rosa",
            "direction": "lent",
            "openedOn": "2026-06-28",
            "amount": "600000",
            "currency": "COP",
            "base": "450000",
        }),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "base");
    assert_eq!(
        body["errors"][0]["message"],
        "La equivalencia necesita importe y moneda."
    );
}
