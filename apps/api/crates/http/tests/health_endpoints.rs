//! Tests de integracion del router HTTP contra dobles de test.
//!
//! Usan `salvo::test::TestClient`, que atraviesa el router entero sin abrir
//! ningun puerto: middleware, handler, caso de uso y DTO incluidos.

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

use std::sync::Arc;

use gastonomo_application::HealthProbe;
use gastonomo_application::testing::FakeHealthProbe;
use salvo::Service;
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt, TestClient};
use support::Harness;

fn service(probes: Vec<Arc<dyn HealthProbe>>) -> Service {
    Harness::with_probes(probes).service
}

#[tokio::test]
async fn liveness_is_always_ok_and_reports_no_checks() {
    let service = service(vec![Arc::new(FakeHealthProbe::failing("database", "down"))]);
    let mut response = TestClient::get("http://localhost/health/live")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::OK));
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], "ok");
    assert_eq!(body["version"], "0.1.0");
    assert_eq!(body["checks"].as_array().map(Vec::len), Some(0));
}

#[tokio::test]
async fn readiness_is_ok_when_every_probe_answers() {
    let service = service(vec![Arc::new(FakeHealthProbe::healthy("database"))]);
    let mut response = TestClient::get("http://localhost/health/ready")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::OK));
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], "ok");
    assert_eq!(body["checks"][0]["name"], "database");
    assert_eq!(body["checks"][0]["status"], "ok");
    assert!(body["checks"][0]["latencyMs"].is_number());
}

#[tokio::test]
async fn readiness_returns_503_when_a_probe_fails() {
    let service = service(vec![Arc::new(FakeHealthProbe::failing(
        "database",
        "connection refused",
    ))]);
    let mut response = TestClient::get("http://localhost/health/ready")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::SERVICE_UNAVAILABLE));
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["status"], "degraded");
    assert_eq!(body["checks"][0]["status"], "error");
    assert_eq!(body["checks"][0]["detail"], "connection refused");
}

#[tokio::test]
async fn readiness_is_degraded_when_only_one_of_many_probes_fails() {
    let service = service(vec![
        Arc::new(FakeHealthProbe::healthy("cache")),
        Arc::new(FakeHealthProbe::failing("database", "timeout")),
    ]);
    let mut response = TestClient::get("http://localhost/health/ready")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::SERVICE_UNAVAILABLE));
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["checks"].as_array().map(Vec::len), Some(2));
    assert_eq!(body["status"], "degraded");
}

#[tokio::test]
async fn every_response_carries_a_request_id() {
    let service = service(Vec::new());
    let response = TestClient::get("http://localhost/health/live")
        .send(&service)
        .await;

    assert!(response.headers().get("x-request-id").is_some());
}

#[tokio::test]
async fn an_incoming_request_id_is_echoed_back() {
    let service = service(Vec::new());
    let response = TestClient::get("http://localhost/health/live")
        .add_header("x-request-id", "abc-123", true)
        .send(&service)
        .await;

    let echoed = response
        .headers()
        .get("x-request-id")
        .and_then(|value| value.to_str().ok());
    assert_eq!(echoed, Some("abc-123"));
}

#[tokio::test]
async fn the_openapi_document_is_served() {
    let service = service(Vec::new());
    let mut response = TestClient::get("http://localhost/openapi.json")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::OK));
    let body: serde_json::Value = response.take_json().await.expect("json body");
    assert_eq!(body["info"]["title"], "Gastonomo API");
    assert!(body["paths"]["/health/ready"]["get"].is_object());
}

#[tokio::test]
async fn an_unknown_route_is_not_found() {
    let service = service(Vec::new());
    let response = TestClient::get("http://localhost/health/unknown")
        .send(&service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::NOT_FOUND));
}
