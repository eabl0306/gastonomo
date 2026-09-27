//! Tests de integracion de `GET /v1/changes`.
//!
//! Atraviesan el router entero con un feed en memoria. Lo que se prueba aqui es la
//! forma de la respuesta y el cursor opaco; que la numeracion no se salte nada se
//! prueba contra PostgreSQL, en `postgres_changes.rs`.

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

use gastonomo_application::port::changes::ChangeBatch;
use gastonomo_application::use_case::changes::{DEFAULT_CHANGE_LIMIT, MAX_CHANGE_LIMIT};
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
    let body = response.take_json::<Value>().await.unwrap();
    body["accessToken"].as_str().unwrap().to_owned()
}

async fn changes(harness: &Harness, token: &str, query: &str) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::get(format!("http://localhost/v1/changes{query}"))
        .add_header("authorization", format!("Bearer {token}"), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

#[tokio::test]
async fn a_cold_start_answers_the_five_collections_and_a_cursor() {
    let harness = Harness::new();
    let token = sign_up(&harness, "frio@correo.com").await;
    harness.changes.set_batch(ChangeBatch::empty(42));

    let (status, body) = changes(&harness, &token, "").await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["hasMore"], false);
    for collection in ["movements", "loans", "loanPayments", "categories"] {
        assert!(body[collection].is_array(), "falta {collection}");
    }
    // Los ajustes no salen si no cambiaron: un campo vacio obligaria al cliente a
    // distinguir "no cambio" de "cambio a los valores por defecto".
    assert!(body.get("settings").is_none());
}

#[tokio::test]
async fn the_cursor_is_opaque_and_comes_back_as_the_number_it_was() {
    let harness = Harness::new();
    let token = sign_up(&harness, "cursor@correo.com").await;
    harness.changes.set_batch(ChangeBatch::empty(42));

    let (_, body) = changes(&harness, &token, "").await;
    let cursor = body["cursor"].as_str().unwrap().to_owned();

    assert_ne!(cursor, "42", "un cursor legible invita a fabricarlo a mano");
    let (status, _) = changes(&harness, &token, &format!("?since={cursor}")).await;
    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(
        harness.changes.calls().last().copied(),
        Some((Some(42), DEFAULT_CHANGE_LIMIT)),
        "el cursor que vuelve tiene que leerse como el numero que era"
    );
}

#[tokio::test]
async fn a_cursor_that_is_not_ours_is_rejected_instead_of_read_as_a_cold_start() {
    let harness = Harness::new();
    let token = sign_up(&harness, "inventado@correo.com").await;

    let (status, body) = changes(&harness, &token, "?since=42").await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "since");
    assert!(
        harness.changes.calls().is_empty(),
        "un cursor roto no puede acabar en una descarga entera"
    );
}

#[tokio::test]
async fn the_batch_size_is_capped_without_failing() {
    let harness = Harness::new();
    let token = sign_up(&harness, "tope@correo.com").await;

    changes(&harness, &token, "?limit=99999").await;

    assert_eq!(
        harness.changes.calls().last().map(|call| call.1),
        Some(MAX_CHANGE_LIMIT)
    );
}

#[tokio::test]
async fn a_batch_size_that_is_not_a_number_is_a_422() {
    let harness = Harness::new();
    let token = sign_up(&harness, "letras@correo.com").await;

    let (status, body) = changes(&harness, &token, "?limit=muchos").await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "limit");
}

#[tokio::test]
async fn without_a_token_there_is_no_feed() {
    let harness = Harness::new();

    let response = TestClient::get("http://localhost/v1/changes")
        .send(&harness.service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
}
