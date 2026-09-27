//! Cliente de los endpoints de categorias, compartido por las dos baterias.
//!
//! Vive en su propio modulo porque las dos baterias (la lista y el archivado)
//! atacan los mismos cinco endpoints y las ayudas no caben repetidas.

#![allow(
    dead_code,
    reason = "each integration test binary compiles this module and uses only the part it needs"
)]
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_application::port::Clock as _;
use gastonomo_domain::UserId;
use gastonomo_domain::category::CategoryId;
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};

use super::support::Harness;

/// Sesion de un usuario recien registrado, con sus 12 predeterminadas sembradas.
pub struct Session {
    pub token: String,
    pub user_id: String,
}

pub async fn sign_up(harness: &Harness, email: &str) -> Session {
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
    let user_id = body["user"]["id"].as_str().unwrap().to_owned();
    // El doble de siembra del registro no escribe categorias: el harness las pone
    // aqui, igual que hace PostgresUserProvisioning contra la base de verdad.
    harness
        .categories
        .seed_defaults(UserId::parse(&user_id).unwrap(), harness.clock.now());
    Session {
        token: body["accessToken"].as_str().unwrap().to_owned(),
        user_id,
    }
}

pub async fn list(harness: &Harness, session: &Session) -> (Option<StatusCode>, Value) {
    list_at(harness, session, "http://localhost/v1/categories").await
}

pub async fn list_including_archived(
    harness: &Harness,
    session: &Session,
) -> (Option<StatusCode>, Value) {
    list_at(
        harness,
        session,
        "http://localhost/v1/categories?includeArchived=true",
    )
    .await
}

pub async fn list_at(
    harness: &Harness,
    session: &Session,
    url: &str,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::get(url)
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

pub async fn create(
    harness: &Harness,
    session: &Session,
    body: &Value,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post("http://localhost/v1/categories")
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

pub async fn patch(
    harness: &Harness,
    session: &Session,
    id: &str,
    body: &Value,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::patch(format!("http://localhost/v1/categories/{id}"))
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

pub async fn delete(harness: &Harness, session: &Session, id: &str) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::delete(format!("http://localhost/v1/categories/{id}"))
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

pub async fn restore(
    harness: &Harness,
    session: &Session,
    id: &str,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post(format!("http://localhost/v1/categories/{id}/restore"))
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

/// Deja la categoria usada por un movimiento, que es lo que la hace archivable.
pub fn given_movements(harness: &Harness, id: &str, movements: u32) {
    harness
        .categories
        .set_usage(CategoryId::parse(id).unwrap(), movements);
}

pub fn entry_of<'a>(listed: &'a Value, id: &str) -> Option<&'a Value> {
    listed["items"]
        .as_array()
        .unwrap()
        .iter()
        .find(|entry| entry["id"] == id)
}

pub fn new_category() -> Value {
    json!({"name": "Mascotas", "kind": "out", "icon": "pets", "color": "#2f7fb5"})
}
