//! Que el limite de peticiones este ENGANCHADO a las rutas que dice el contrato.
//!
//! Existe porque no lo comprobaba nadie. Los tests de `rate_limit.rs` prueban la
//! logica de conteo, pero ninguno pasaba por el router, asi que se podia borrar un
//! `.hoop(limit)` de cualquier endpoint y la bateria entera seguia en verde.
//!
//! El limite se cuenta en el middleware, antes del handler, asi que el cuerpo de
//! la peticion da igual: lo que se mide aqui es el cableado, no la validacion.

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

/// Los cuatro endpoints publicos que tocan credenciales y llevan limite.
const LIMITED: [&str; 4] = [
    "/v1/auth/register",
    "/v1/auth/login",
    "/v1/auth/password/forgot",
    "/v1/auth/password/reset",
];

/// Publicos tambien, pero sin limite a proposito: exigen un refresh token valido
/// en el cuerpo, asi que no son una puerta abierta.
const UNLIMITED: [&str; 2] = ["/v1/auth/refresh", "/v1/auth/logout"];

async fn status_of(harness: &Harness, path: &str) -> Option<StatusCode> {
    TestClient::post(format!("http://localhost{path}"))
        .json(&json!({}))
        .send(&harness.service)
        .await
        .status_code
}

#[tokio::test]
async fn every_limited_auth_endpoint_answers_429_past_its_limit() {
    for path in LIMITED {
        let harness = Harness::with_rate_limit(3);
        for attempt in 1..=3 {
            let status = status_of(&harness, path).await;
            assert_ne!(
                status,
                Some(StatusCode::TOO_MANY_REQUESTS),
                "{path} ran out of quota on attempt {attempt}, before the limit"
            );
        }
        assert_eq!(
            status_of(&harness, path).await,
            Some(StatusCode::TOO_MANY_REQUESTS),
            "{path} has no rate limit attached"
        );
    }
}

#[tokio::test]
async fn the_endpoints_without_a_limit_never_answer_429() {
    let harness = Harness::with_rate_limit(3);
    for path in UNLIMITED {
        for _ in 0..10 {
            assert_ne!(
                status_of(&harness, path).await,
                Some(StatusCode::TOO_MANY_REQUESTS),
                "{path} is not supposed to be limited"
            );
        }
    }
}

/// La ventana es por cliente **y ruta**. Si fuera solo por cliente, colgar el
/// limite en un cuarto endpoint le quitaria cupo a los otros tres.
#[tokio::test]
async fn one_endpoint_running_out_does_not_spend_another_ones_quota() {
    let harness = Harness::with_rate_limit(3);
    for _ in 0..4 {
        status_of(&harness, "/v1/auth/password/reset").await;
    }
    assert_eq!(
        status_of(&harness, "/v1/auth/password/reset").await,
        Some(StatusCode::TOO_MANY_REQUESTS),
        "the reset quota should be spent by now"
    );

    assert_ne!(
        status_of(&harness, "/v1/auth/login").await,
        Some(StatusCode::TOO_MANY_REQUESTS),
        "login should still have its own quota"
    );
}

/// El 429 sale como problem+json, no como la pagina por defecto de Salvo.
#[tokio::test]
async fn the_429_is_a_problem_json_with_its_own_type() {
    let harness = Harness::with_rate_limit(1);
    status_of(&harness, "/v1/auth/login").await;

    let mut response = TestClient::post("http://localhost/v1/auth/login")
        .json(&json!({}))
        .send(&harness.service)
        .await;
    assert_eq!(response.status_code, Some(StatusCode::TOO_MANY_REQUESTS));
    let body = response.take_json::<Value>().await.unwrap_or(Value::Null);
    assert_eq!(
        body["type"].as_str(),
        Some("https://gastonomo.app/problems/too-many-requests")
    );
    assert!(
        body["detail"].as_str().is_some_and(|d| !d.is_empty()),
        "the body should explain the wait: {body}"
    );
}
