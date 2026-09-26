//! Tests de integracion de los endpoints de ajustes.

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
    let body = response.take_json::<Value>().await.unwrap();
    body["accessToken"].as_str().unwrap().to_owned()
}

async fn read(harness: &Harness, token: &str) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::get("http://localhost/v1/settings")
        .add_header("authorization", format!("Bearer {token}"), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

async fn write(harness: &Harness, token: &str, body: &Value) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::patch("http://localhost/v1/settings")
        .add_header("authorization", format!("Bearer {token}"), true)
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

#[tokio::test]
async fn a_new_account_reads_cop_and_spanish() {
    let harness = Harness::new();
    let token = sign_up(&harness, "ajustes@correo.com").await;

    let (status, body) = read(&harness, &token).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["displayCurrency"], "COP");
    assert_eq!(body["language"], "es");
    assert_eq!(
        body.as_object().unwrap().len(),
        2,
        "solo esos dos campos viajan"
    );
}

#[tokio::test]
async fn without_a_token_there_are_no_settings() {
    let harness = Harness::new();

    let response = TestClient::get("http://localhost/v1/settings")
        .send(&harness.service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
}

#[tokio::test]
async fn changing_the_currency_answers_the_new_state_and_persists_it() {
    let harness = Harness::new();
    let token = sign_up(&harness, "moneda@correo.com").await;

    let (status, body) = write(&harness, &token, &json!({"displayCurrency": "usd"})).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["displayCurrency"], "USD");
    assert_eq!(body["language"], "es");

    let (_, stored) = read(&harness, &token).await;
    assert_eq!(stored["displayCurrency"], "USD");
}

#[tokio::test]
async fn changing_the_language_leaves_the_currency_alone() {
    let harness = Harness::new();
    let token = sign_up(&harness, "idioma@correo.com").await;

    let (status, body) = write(&harness, &token, &json!({"language": "en"})).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["language"], "en");
    assert_eq!(body["displayCurrency"], "COP");
}

#[tokio::test]
async fn a_currency_outside_the_catalog_is_unprocessable() {
    let harness = Harness::new();
    let token = sign_up(&harness, "rara@correo.com").await;

    let (status, body) = write(&harness, &token, &json!({"displayCurrency": "XYZ"})).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "displayCurrency");
    assert_eq!(body["type"], "https://gastonomo.app/problems/validation");
}

#[tokio::test]
async fn a_language_that_is_not_es_or_en_is_unprocessable() {
    let harness = Harness::new();
    let token = sign_up(&harness, "frances@correo.com").await;

    let (status, body) = write(&harness, &token, &json!({"language": "fr"})).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "language");
}

#[tokio::test]
async fn the_device_only_settings_are_not_accepted() {
    let harness = Harness::new();
    let token = sign_up(&harness, "huella@correo.com").await;

    let (status, _) = write(&harness, &token, &json!({"biometricLock": true})).await;

    assert_eq!(
        status,
        Some(StatusCode::UNPROCESSABLE_ENTITY),
        "el cerrojo del dispositivo no es un campo de la API"
    );
}

#[tokio::test]
async fn one_account_never_sees_the_settings_of_another() {
    let harness = Harness::new();
    let first = sign_up(&harness, "primera@correo.com").await;
    let second = sign_up(&harness, "segunda@correo.com").await;
    write(
        &harness,
        &first,
        &json!({"displayCurrency": "EUR", "language": "en"}),
    )
    .await;

    let (status, body) = read(&harness, &second).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["displayCurrency"], "COP");
    assert_eq!(body["language"], "es");
}
