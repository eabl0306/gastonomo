//! Tests de integracion de los endpoints de autenticacion.
//!
//! Atraviesan el router entero: middleware, handler, caso de uso y DTO, con dobles
//! en memoria en lugar de PostgreSQL.

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
use support::{Harness, registration};

async fn post(harness: &Harness, path: &str, body: &Value) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post(format!("http://localhost{path}"))
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

async fn register(harness: &Harness) -> Value {
    let (status, body) = post(harness, "/v1/auth/register", &registration()).await;
    assert_eq!(status, Some(StatusCode::CREATED));
    body
}

#[tokio::test]
async fn registering_answers_201_with_the_session_and_the_profile() {
    let harness = Harness::new();
    let body = register(&harness).await;

    assert!(body["accessToken"].is_string());
    assert!(body["refreshToken"].is_string());
    assert_eq!(body["expiresIn"], 900);
    assert_eq!(body["user"]["email"], "camilo@correo.com");
    assert_eq!(body["user"]["name"], "Camilo Rojas");
    assert!(
        body["user"]["createdAt"]
            .as_str()
            .is_some_and(|stamp| stamp.ends_with('Z'))
    );
}

#[tokio::test]
async fn a_repeated_email_answers_409_with_a_stable_code() {
    let harness = Harness::new();
    register(&harness).await;
    let (status, body) = post(&harness, "/v1/auth/register", &registration()).await;

    assert_eq!(status, Some(StatusCode::CONFLICT));
    assert_eq!(body["code"], "email_taken");
    assert_eq!(body["status"], 409);
    assert_eq!(body["type"], "https://gastonomo.app/problems/conflict");
}

#[tokio::test]
async fn an_invalid_form_answers_422_with_one_entry_per_field() {
    let harness = Harness::new();
    let (status, body) = post(
        &harness,
        "/v1/auth/register",
        &json!({"name": " ", "email": "camilo", "password": "corta", "acceptedTerms": false}),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    let fields: Vec<&str> = body["errors"]
        .as_array()
        .expect("errors should be an array")
        .iter()
        .filter_map(|entry| entry["field"].as_str())
        .collect();
    assert_eq!(fields, vec!["name", "email", "password", "acceptedTerms"]);
}

#[tokio::test]
async fn an_error_body_uses_the_problem_json_content_type() {
    let harness = Harness::new();
    let response = TestClient::post("http://localhost/v1/auth/login")
        .json(&json!({"email": "nadie@correo.com", "password": "contrasena-larga"}))
        .send(&harness.service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
    let content_type = response
        .headers()
        .get("content-type")
        .and_then(|value| value.to_str().ok())
        .unwrap_or_default();
    assert!(
        content_type.starts_with("application/problem+json"),
        "got {content_type}"
    );
}

#[tokio::test]
async fn login_refresh_and_logout_walk_the_whole_session() {
    let harness = Harness::new();
    register(&harness).await;

    let (status, login) = post(
        &harness,
        "/v1/auth/login",
        &json!({"email": "camilo@correo.com", "password": "contrasena-larga"}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::OK));

    let refresh_token = login["refreshToken"].as_str().expect("a refresh token");
    let (status, refreshed) = post(
        &harness,
        "/v1/auth/refresh",
        &json!({"refreshToken": refresh_token}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::OK));
    assert_ne!(refreshed["refreshToken"], login["refreshToken"]);

    let rotated = refreshed["refreshToken"].as_str().expect("a refresh token");
    let response = TestClient::post("http://localhost/v1/auth/logout")
        .json(&json!({"refreshToken": rotated}))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::NO_CONTENT));

    let (status, _) = post(
        &harness,
        "/v1/auth/refresh",
        &json!({"refreshToken": rotated}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::UNAUTHORIZED));
}

#[tokio::test]
async fn reusing_a_rotated_refresh_token_answers_401() {
    let harness = Harness::new();
    let session = register(&harness).await;
    let first = session["refreshToken"].as_str().expect("a refresh token");

    post(
        &harness,
        "/v1/auth/refresh",
        &json!({"refreshToken": first}),
    )
    .await;
    let (status, body) = post(
        &harness,
        "/v1/auth/refresh",
        &json!({"refreshToken": first}),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNAUTHORIZED));
    // El cuerpo NO puede decir por que. Contarle a quien reutiliza un token robado
    // que se ha detectado la reutilizacion y que la familia esta revocada le avisa
    // de que le han pillado. El motivo real se queda en la traza del servidor.
    let detail = body["detail"].as_str().expect("a detail");
    assert!(
        !detail.contains("reuse") && !detail.contains("revoked"),
        "the body should not explain itself: {body}"
    );
    assert_eq!(detail, UNAUTHORIZED_DETAIL);
}

/// Unico detalle que la API da en un 401, sea cual sea el motivo.
const UNAUTHORIZED_DETAIL: &str = "El token no vale, ha caducado o ya se uso.";

/// Pide el enlace y saca el token del correo que se habria mandado.
async fn reset_token(harness: &Harness) -> String {
    let (status, _) = post(
        harness,
        "/v1/auth/password/forgot",
        &json!({"email": "camilo@correo.com"}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::ACCEPTED));
    let sent = harness.mailer.sent();
    let body = sent.last().expect("a mail").body.clone();
    body.split("token=")
        .nth(1)
        .and_then(|rest| rest.split_whitespace().next())
        .map(str::to_owned)
        .expect("the mail should carry a link with a token")
}

/// El caso que da nombre a todo esto: un token que no existe y uno ya gastado
/// tienen que ser indistinguibles desde fuera.
///
/// Si se distinguieran, probar tokens a ciegas diria cuando se acierta con uno
/// que existe, que es la mitad del trabajo de robar una cuenta.
#[tokio::test]
async fn an_unknown_reset_token_and_a_spent_one_answer_the_same() {
    let harness = Harness::new();
    register(&harness).await;
    let token = reset_token(&harness).await;

    let (status, _) = post(
        &harness,
        "/v1/auth/password/reset",
        &json!({"token": token, "newPassword": "otra-contrasena-larga"}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::NO_CONTENT), "the first use works");

    // El mismo token, ya gastado.
    let (spent_status, spent) = post(
        &harness,
        "/v1/auth/password/reset",
        &json!({"token": token, "newPassword": "tercera-contrasena-larga"}),
    )
    .await;
    // Uno que no ha existido nunca.
    let (unknown_status, unknown) = post(
        &harness,
        "/v1/auth/password/reset",
        &json!({"token": "esto-no-es-un-token", "newPassword": "tercera-contrasena-larga"}),
    )
    .await;

    assert_eq!(spent_status, Some(StatusCode::UNAUTHORIZED));
    assert_eq!(unknown_status, Some(StatusCode::UNAUTHORIZED));
    assert_eq!(spent, unknown, "the two bodies have to be identical");
    assert_eq!(spent["detail"].as_str(), Some(UNAUTHORIZED_DETAIL));
}

#[tokio::test]
async fn forgot_answers_202_whether_the_account_exists_or_not() {
    let harness = Harness::new();
    register(&harness).await;

    for email in ["camilo@correo.com", "nadie@correo.com"] {
        let response = TestClient::post("http://localhost/v1/auth/password/forgot")
            .json(&json!({"email": email}))
            .send(&harness.service)
            .await;
        assert_eq!(response.status_code, Some(StatusCode::ACCEPTED));
    }
    assert_eq!(
        harness.mailer.sent().len(),
        1,
        "only the account that exists gets a mail"
    );
}

#[tokio::test]
async fn me_needs_a_bearer_token() {
    let harness = Harness::new();
    let session = register(&harness).await;

    let response = TestClient::get("http://localhost/v1/me")
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));

    let mut response = TestClient::get("http://localhost/v1/me")
        .add_header(
            "authorization",
            format!(
                "Bearer {}",
                session["accessToken"].as_str().unwrap_or_default()
            ),
            true,
        )
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::OK));
    let body = response.take_json::<Value>().await.expect("json body");
    assert_eq!(body["email"], "camilo@correo.com");
    assert_eq!(body["id"], session["user"]["id"]);
}

#[tokio::test]
async fn a_bogus_token_does_not_open_the_door() {
    let harness = Harness::new();
    for header in ["Bearer nope", "nope", "Bearer "] {
        let response = TestClient::get("http://localhost/v1/me")
            .add_header("authorization", header, true)
            .send(&harness.service)
            .await;
        assert_eq!(
            response.status_code,
            Some(StatusCode::UNAUTHORIZED),
            "{header} should not authenticate"
        );
    }
}

#[tokio::test]
async fn changing_the_password_from_settings_needs_the_token_and_the_old_password() {
    let harness = Harness::new();
    let session = register(&harness).await;
    let token = session["accessToken"].as_str().unwrap_or_default();

    let response = TestClient::post("http://localhost/v1/auth/password/change")
        .add_header("authorization", format!("Bearer {token}"), true)
        .json(&json!({
            "currentPassword": "me-la-invento",
            "newPassword": "contrasena-nueva",
        }))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));

    let response = TestClient::post("http://localhost/v1/auth/password/change")
        .add_header("authorization", format!("Bearer {token}"), true)
        .json(&json!({
            "currentPassword": "contrasena-larga",
            "newPassword": "contrasena-nueva",
            "refreshToken": session["refreshToken"].as_str().unwrap_or_default(),
        }))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::NO_CONTENT));

    let (status, _) = post(
        &harness,
        "/v1/auth/login",
        &json!({"email": "camilo@correo.com", "password": "contrasena-nueva"}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::OK));
}

#[tokio::test]
async fn the_recovery_link_changes_the_password() {
    let harness = Harness::new();
    register(&harness).await;
    post(
        &harness,
        "/v1/auth/password/forgot",
        &json!({"email": "camilo@correo.com"}),
    )
    .await;

    let sent = harness.mailer.sent();
    let token = sent[0]
        .body
        .split("token=")
        .nth(1)
        .and_then(|rest| rest.split_whitespace().next())
        .expect("the mail carries a token")
        .to_owned();

    let response = TestClient::post("http://localhost/v1/auth/password/reset")
        .json(&json!({"token": token, "newPassword": "contrasena-nueva"}))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::NO_CONTENT));

    let (status, _) = post(
        &harness,
        "/v1/auth/login",
        &json!({"email": "camilo@correo.com", "password": "contrasena-nueva"}),
    )
    .await;
    assert_eq!(status, Some(StatusCode::OK));
}
