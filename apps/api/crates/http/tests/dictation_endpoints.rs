//! Tests de integracion del endpoint de dictado.
//!
//! Atraviesan el router entero: middleware de access token, handler, caso de uso y
//! DTO, con dobles en memoria en lugar de PostgreSQL y del modelo.

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

use gastonomo_domain::value_object::UserId;
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};
use support::Harness;

struct Session {
    token: String,
    user_id: UserId,
}

async fn sign_up(harness: &Harness, email: &str) -> Session {
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
    Session {
        token: body["accessToken"].as_str().unwrap().to_owned(),
        user_id: UserId::parse(body["user"]["id"].as_str().unwrap()).unwrap(),
    }
}

async fn dictate(harness: &Harness, session: &Session, text: &str) -> (StatusCode, Value) {
    let mut response = TestClient::post("http://localhost/v1/dictation/parse")
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .json(&json!({ "text": text }))
        .send(&harness.service)
        .await;
    let status = response.status_code.unwrap();
    let body = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, body)
}

/// Sin aprobacion la capacidad **no existe**: 404 y no 403, igual que un recurso de
/// otra cuenta. Un 403 confirmaria que esta ahi.
#[tokio::test]
async fn an_account_without_approval_gets_a_not_found() {
    let harness = Harness::new();
    let session = sign_up(&harness, "sin.permiso@correo.com").await;

    let (status, body) = dictate(&harness, &session, "gaste 45 mil en almuerzo").await;

    assert_eq!(status, StatusCode::NOT_FOUND);
    assert_eq!(body["type"], "https://gastonomo.app/problems/not-found");
    assert_eq!(harness.dictation.interpreter.calls(), 0);
}

#[tokio::test]
async fn an_approved_account_gets_a_draft_to_review() {
    let harness = Harness::new();
    let session = sign_up(&harness, "con.permiso@correo.com").await;
    harness.dictation.access.allow(session.user_id);

    let (status, body) = dictate(&harness, &session, "gaste 45 mil en almuerzo").await;

    assert_eq!(status, StatusCode::OK);
    assert_eq!(body["kind"], "movement");
    assert_eq!(body["movement"]["kind"], "out");
    assert_eq!(body["missing"].as_array().unwrap().len(), 0);
}

/// El importe viaja como **texto**, igual que en todo el contrato. Un numero JSON
/// acabaria en coma flotante en cuanto el cliente lo leyera.
#[tokio::test]
async fn the_amount_travels_as_a_string() {
    let harness = Harness::new();
    let session = sign_up(&harness, "importe@correo.com").await;
    harness.dictation.access.allow(session.user_id);

    let (_, body) = dictate(&harness, &session, "gaste 45 mil").await;

    assert!(body["amount"].is_string(), "fue {}", body["amount"]);
    assert_eq!(body["amount"], "45000.0000");
}

#[tokio::test]
async fn an_empty_text_is_rejected_before_reaching_the_model() {
    let harness = Harness::new();
    let session = sign_up(&harness, "vacio@correo.com").await;
    harness.dictation.access.allow(session.user_id);

    let (status, body) = dictate(&harness, &session, "   ").await;

    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);
    assert_eq!(body["errors"][0]["field"], "text");
    assert_eq!(harness.dictation.interpreter.calls(), 0);
}

#[tokio::test]
async fn without_a_token_there_is_nothing_to_dictate() {
    let harness = Harness::new();

    let response = TestClient::post("http://localhost/v1/dictation/parse")
        .json(&json!({ "text": "gaste 45 mil" }))
        .send(&harness.service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
}

/// El endpoint **no crea nada**: confirmar es una llamada aparte. Si algun dia
/// empezara a escribir, este test lo dice.
#[tokio::test]
async fn dictating_never_creates_a_movement() {
    let harness = Harness::new();
    let session = sign_up(&harness, "sin.escribir@correo.com").await;
    harness.dictation.access.allow(session.user_id);

    let _ = dictate(&harness, &session, "gaste 45 mil en almuerzo").await;

    let mut response = TestClient::get("http://localhost/v1/movements")
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .send(&harness.service)
        .await;
    let body = response.take_json::<Value>().await.unwrap();
    assert_eq!(body["items"].as_array().unwrap().len(), 0);
}
