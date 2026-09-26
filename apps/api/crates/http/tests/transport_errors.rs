//! Tests de los errores que no llegan a un handler.
//!
//! La ruta que no existe y el metodo que no esta permitido los resuelve Salvo, no
//! un caso de uso. Sin el catcher del contrato salian en HTML, y un cliente de esta
//! API siempre espera JSON.

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

use gastonomo_http::PROBLEM_CONTENT_TYPE;
use salvo::http::header::CONTENT_TYPE;
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt, TestClient};
use support::Harness;

fn content_type(response: &salvo::Response) -> String {
    response
        .headers()
        .get(CONTENT_TYPE)
        .and_then(|value| value.to_str().ok())
        .unwrap_or_default()
        .to_owned()
}

#[tokio::test]
async fn an_unknown_route_answers_problem_json_and_not_html() {
    let service = Harness::new().service;
    let mut response = TestClient::get("http://localhost/v1/no-existe")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::NOT_FOUND));
    assert_eq!(content_type(&response), PROBLEM_CONTENT_TYPE);
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], 404);
    assert_eq!(body["type"], "https://gastonomo.app/problems/not-found");
    assert!(body["detail"].as_str().is_some_and(|text| !text.is_empty()));
}

#[tokio::test]
async fn an_unknown_route_stays_json_even_when_the_client_asks_for_html() {
    let service = Harness::new().service;
    let mut response = TestClient::get("http://localhost/no-existe")
        .add_header("accept", "text/html", true)
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::NOT_FOUND));
    assert_eq!(content_type(&response), PROBLEM_CONTENT_TYPE);
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], 404);
}

#[tokio::test]
async fn a_method_that_the_route_does_not_accept_answers_problem_json() {
    let service = Harness::new().service;
    let mut response = TestClient::delete("http://localhost/health/live")
        .send(&service)
        .await;

    let status = response.status_code.expect("status");
    assert!(
        status == StatusCode::METHOD_NOT_ALLOWED || status == StatusCode::NOT_FOUND,
        "salvo answers 404 or 405, but never HTML: got {status}"
    );
    assert_eq!(content_type(&response), PROBLEM_CONTENT_TYPE);
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], status.as_u16());
}

#[tokio::test]
async fn a_problem_written_by_a_handler_is_not_overwritten_by_the_catcher() {
    let service = Harness::new().service;
    let mut response = TestClient::get("http://localhost/v1/settings")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
    assert_eq!(content_type(&response), PROBLEM_CONTENT_TYPE);
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["type"], "https://gastonomo.app/problems/unauthorized");
}
