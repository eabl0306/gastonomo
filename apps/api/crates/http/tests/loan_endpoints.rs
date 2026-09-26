//! Tests de integracion de los endpoints de prestamos.
//!
//! Atraviesan el router entero: middleware de access token, handler, caso de uso y
//! DTO, con dobles en memoria en lugar de PostgreSQL. La atomicidad de las dos
//! escrituras se prueba contra una base de datos de verdad en
//! `gastonomo-infrastructure`, que es donde vive la transaccion.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
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

/// Sesion abierta: token y cuenta.
struct Session {
    token: String,
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
    let body = response.take_json::<Value>().await.expect("session body");
    Session {
        token: body["accessToken"]
            .as_str()
            .expect("the session carries an access token")
            .to_owned(),
    }
}

async fn post(
    harness: &Harness,
    session: &Session,
    path: &str,
    body: &Value,
) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post(format!("http://localhost{path}"))
        .bearer_auth(session.token.clone())
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

async fn get(harness: &Harness, session: &Session, path: &str) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::get(format!("http://localhost{path}"))
        .bearer_auth(session.token.clone())
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

fn a_loan() -> Value {
    json!({
        "person": "Rosa",
        "note": "Teléfono",
        "direction": "lent",
        "openedOn": "2026-06-28",
        "amount": "600000.00",
        "currency": "COP",
        "base": "600000.00",
        "baseCurrency": "COP",
    })
}

fn a_payment() -> Value {
    json!({
        "paidOn": "2026-07-29",
        "label": "Intereses",
        "amount": "200000.00",
        "currency": "COP",
        "base": "200000.00",
        "baseCurrency": "COP",
    })
}

#[tokio::test]
async fn creating_a_loan_answers_201_with_the_loan_and_no_payments() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;

    let (status, body) = post(&harness, &session, "/v1/loans", &a_loan()).await;

    assert_eq!(status, Some(StatusCode::CREATED));
    assert_eq!(body["person"], "Rosa");
    assert_eq!(body["note"], "Teléfono");
    assert_eq!(body["direction"], "lent");
    assert_eq!(body["openedOn"], "2026-06-28");
    assert_eq!(body["amount"], "600000.0000");
    assert_eq!(body["currency"], "COP");
    assert_eq!(body["base"], "600000.0000");
    assert_eq!(body["baseCurrency"], "COP");
    assert_eq!(body["payments"], json!([]));
    assert!(body["id"].is_string());
    assert!(
        body["createdAt"]
            .as_str()
            .is_some_and(|stamp| stamp.ends_with('Z'))
    );
    assert!(
        body.get("balance").is_none(),
        "the balance belongs to the client, not to the API"
    );
}

#[tokio::test]
async fn creating_a_loan_also_writes_its_movement() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;
    let (_, body) = post(&harness, &session, "/v1/loans", &a_loan()).await;

    let movements = harness.loans.movements();
    assert_eq!(movements.len(), 1);
    assert_eq!(movements[0].title(), "Préstamo · Rosa");
    assert_eq!(movements[0].category_name(), "Préstamo");
    assert_eq!(movements[0].kind().as_str(), "out");
    assert_eq!(movements[0].loan_id().to_string(), body["id"]);
}

#[tokio::test]
async fn an_amount_that_is_not_a_number_answers_422_with_the_field() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;

    let (status, body) = post(
        &harness,
        &session,
        "/v1/loans",
        &json!({
            "person": "",
            "direction": "lent",
            "openedOn": "2026-06-28",
            "amount": "mucho",
            "currency": "COP",
            "base": "600000",
            "baseCurrency": "COP",
        }),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    let fields: Vec<&str> = body["errors"]
        .as_array()
        .expect("errors should be an array")
        .iter()
        .filter_map(|entry| entry["field"].as_str())
        .collect();
    assert_eq!(fields, vec!["person", "amount"]);
}

#[tokio::test]
async fn the_list_filters_by_direction_like_the_pills() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;
    post(&harness, &session, "/v1/loans", &a_loan()).await;
    let mut borrowed = a_loan();
    borrowed["direction"] = json!("borrowed");
    borrowed["person"] = json!("Mamá");
    post(&harness, &session, "/v1/loans", &borrowed).await;

    let (status, all) = get(&harness, &session, "/v1/loans").await;
    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(all["items"].as_array().expect("items").len(), 2);

    let (_, lent) = get(&harness, &session, "/v1/loans?direction=lent").await;
    let items = lent["items"].as_array().expect("items");
    assert_eq!(items.len(), 1);
    assert_eq!(items[0]["person"], "Rosa");

    let (status, problem) = get(&harness, &session, "/v1/loans?direction=given").await;
    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "direction");
}

#[tokio::test]
async fn a_payment_answers_the_loan_with_the_payment_inside() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;
    let (_, loan) = post(&harness, &session, "/v1/loans", &a_loan()).await;
    let id = loan["id"].as_str().expect("the loan carries an id");

    let (status, body) = post(
        &harness,
        &session,
        &format!("/v1/loans/{id}/payments"),
        &a_payment(),
    )
    .await;

    assert_eq!(status, Some(StatusCode::CREATED));
    let payments = body["payments"].as_array().expect("payments");
    assert_eq!(payments.len(), 1);
    assert_eq!(payments[0]["label"], "Intereses");
    assert_eq!(payments[0]["amount"], "200000.0000");
    assert_eq!(payments[0]["loanId"], id);

    let movements = harness.loans.movements();
    assert_eq!(movements.len(), 2);
    assert_eq!(movements[1].title(), "Abono · Rosa");
    assert_eq!(movements[1].kind().as_str(), "in");
    assert!(movements[1].loan_payment_id().is_some());
}

#[tokio::test]
async fn a_payment_bigger_than_the_loan_is_accepted() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;
    let (_, loan) = post(&harness, &session, "/v1/loans", &a_loan()).await;
    let id = loan["id"].as_str().expect("the loan carries an id");
    let mut payment = a_payment();
    payment["amount"] = json!("900000.00");
    payment["base"] = json!("900000.00");

    let (status, _) = post(
        &harness,
        &session,
        &format!("/v1/loans/{id}/payments"),
        &payment,
    )
    .await;

    assert_eq!(
        status,
        Some(StatusCode::CREATED),
        "the surplus is earned interest, and the design shows it"
    );
}

#[tokio::test]
async fn the_detail_of_another_account_answers_404_and_never_403() {
    let harness = Harness::new();
    let owner = sign_up(&harness, "rosa@correo.com").await;
    let intruder = sign_up(&harness, "otra@correo.com").await;
    let (_, loan) = post(&harness, &owner, "/v1/loans", &a_loan()).await;
    let id = loan["id"].as_str().expect("the loan carries an id");

    let (detail, body) = get(&harness, &intruder, &format!("/v1/loans/{id}")).await;
    assert_eq!(detail, Some(StatusCode::NOT_FOUND));
    assert_eq!(body["status"], 404);

    let (payment, _) = post(
        &harness,
        &intruder,
        &format!("/v1/loans/{id}/payments"),
        &a_payment(),
    )
    .await;
    assert_eq!(payment, Some(StatusCode::NOT_FOUND));

    let (_, list) = get(&harness, &intruder, "/v1/loans").await;
    assert_eq!(list["items"], json!([]));
}

#[tokio::test]
async fn an_identifier_that_is_not_a_uuid_answers_404() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;

    let (status, _) = get(&harness, &session, "/v1/loans/not-a-uuid").await;
    assert_eq!(status, Some(StatusCode::NOT_FOUND));
}

#[tokio::test]
async fn without_a_token_every_loan_endpoint_answers_401() {
    let harness = Harness::new();

    let mut list = TestClient::get("http://localhost/v1/loans")
        .send(&harness.service)
        .await;
    assert_eq!(list.status_code, Some(StatusCode::UNAUTHORIZED));
    let problem = list.take_json::<Value>().await.expect("problem body");
    assert_eq!(
        problem["type"],
        "https://gastonomo.app/problems/unauthorized"
    );

    let create = TestClient::post("http://localhost/v1/loans")
        .json(&a_loan())
        .send(&harness.service)
        .await;
    assert_eq!(create.status_code, Some(StatusCode::UNAUTHORIZED));
}

#[tokio::test]
async fn an_unknown_field_in_the_body_is_rejected() {
    let harness = Harness::new();
    let session = sign_up(&harness, "rosa@correo.com").await;
    let mut body = a_loan();
    body["balance"] = json!("100");

    let (status, problem) = post(&harness, &session, "/v1/loans", &body).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "body");
}

#[tokio::test]
async fn the_openapi_document_describes_the_four_endpoints() {
    let harness = Harness::new();
    let mut response = TestClient::get("http://localhost/openapi.json")
        .send(&harness.service)
        .await;
    let document = response.take_json::<Value>().await.expect("openapi body");

    assert!(document["paths"]["/v1/loans"]["get"].is_object());
    assert!(document["paths"]["/v1/loans"]["post"].is_object());
    assert!(document["paths"]["/v1/loans/{id}"]["get"].is_object());
    assert!(document["paths"]["/v1/loans/{id}/payments"]["post"].is_object());
    assert!(document["components"]["schemas"]["LoanDto"].is_object());
    assert!(document["components"]["schemas"]["LoanListDto"].is_object());
    assert!(document["components"]["schemas"]["LoanPaymentDto"].is_object());
    assert!(document["components"]["schemas"]["CreateLoanRequestDto"].is_object());
    assert!(
        document["components"]["schemas"]["CreateLoanPaymentRequestDto"].is_object(),
        "the payment body has to be in the document too"
    );
}
