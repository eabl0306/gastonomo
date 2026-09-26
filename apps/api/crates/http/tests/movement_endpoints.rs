//! Tests de integracion de los endpoints de movimientos.
//!
//! Atraviesan el router entero: middleware de access token, handler, caso de uso y
//! DTO, con dobles en memoria en lugar de PostgreSQL.

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

use gastonomo_domain::CategoryKind;
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};
use support::Harness;

/// Sesion de un usuario recien registrado, con una categoria de gasto sembrada.
struct Session {
    token: String,
    user_id: String,
    expense_category: String,
    income_category: String,
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
    let user_id = body["user"]["id"].as_str().unwrap().to_owned();
    Session {
        token: body["accessToken"].as_str().unwrap().to_owned(),
        expense_category: harness.seed_category(&user_id, "Alimentación", CategoryKind::Out),
        income_category: harness.seed_category(&user_id, "Salario", CategoryKind::In),
        user_id,
    }
}

async fn post(harness: &Harness, session: &Session, body: &Value) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::post("http://localhost/v1/movements")
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .json(body)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

async fn get(harness: &Harness, session: &Session, query: &str) -> (Option<StatusCode>, Value) {
    let mut response = TestClient::get(format!("http://localhost/v1/movements{query}"))
        .add_header("authorization", format!("Bearer {}", session.token), true)
        .send(&harness.service)
        .await;
    let status = response.status_code;
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload)
}

fn expense(session: &Session) -> Value {
    json!({
        "occurredOn": "2026-09-15",
        "kind": "out",
        "categoryId": session.expense_category,
        "note": "Mercado",
        "amount": "186400",
        "currency": "COP",
    })
}

#[tokio::test]
async fn creating_a_movement_answers_201_with_the_four_amount_fields_as_strings() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let (status, body) = post(&harness, &session, &expense(&session)).await;

    assert_eq!(status, Some(StatusCode::CREATED));
    assert_eq!(body["occurredOn"], "2026-09-15");
    assert_eq!(body["kind"], "out");
    assert_eq!(body["title"], "Mercado");
    assert_eq!(body["amount"], "186400.0000");
    assert_eq!(body["currency"], "COP");
    assert_eq!(body["base"], "186400.0000");
    assert_eq!(body["baseCurrency"], "COP");
    assert_eq!(body["category"]["name"], "Alimentación");
    assert_eq!(body["category"]["icon"], "restaurant");
    assert_eq!(body["category"]["color"], "#cf5f35");
    assert!(body["loanId"].is_null());
    assert!(
        body["createdAt"]
            .as_str()
            .is_some_and(|stamp| stamp.ends_with('Z'))
    );
}

#[tokio::test]
async fn a_movement_in_another_currency_keeps_the_equivalence_it_was_given() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let (status, body) = post(
        &harness,
        &session,
        &json!({
            "occurredOn": "2026-09-05",
            "kind": "in",
            "categoryId": session.income_category,
            "note": "Diseño web · cliente EE. UU.",
            "amount": "140",
            "currency": "USD",
            "base": "450000",
            "baseCurrency": "COP",
        }),
    )
    .await;

    assert_eq!(status, Some(StatusCode::CREATED));
    assert_eq!(body["amount"], "140.0000");
    assert_eq!(body["currency"], "USD");
    assert_eq!(body["base"], "450000.0000");
    assert_eq!(body["baseCurrency"], "COP");
}

#[tokio::test]
async fn without_a_note_the_title_is_the_category_name() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let (_, body) = post(
        &harness,
        &session,
        &json!({
            "occurredOn": "2026-09-15",
            "kind": "out",
            "categoryId": session.expense_category,
            "amount": "32000",
            "currency": "COP",
        }),
    )
    .await;

    assert_eq!(body["title"], "Alimentación");
}

#[tokio::test]
async fn the_list_comes_sorted_by_date_descending() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;
    for day in ["2026-09-01", "2026-09-20", "2026-09-15"] {
        let mut body = expense(&session);
        body["occurredOn"] = json!(day);
        post(&harness, &session, &body).await;
    }

    let (status, page) = get(&harness, &session, "").await;

    assert_eq!(status, Some(StatusCode::OK));
    let dates: Vec<&str> = page["items"]
        .as_array()
        .unwrap()
        .iter()
        .map(|item| item["occurredOn"].as_str().unwrap())
        .collect();
    assert_eq!(dates, ["2026-09-20", "2026-09-15", "2026-09-01"]);
    assert!(page["nextCursor"].is_null());
}

#[tokio::test]
async fn the_home_screen_asks_for_the_four_latest() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;
    for day in 1..=6 {
        let mut body = expense(&session);
        body["occurredOn"] = json!(format!("2026-09-0{day}"));
        post(&harness, &session, &body).await;
    }

    let (_, page) = get(&harness, &session, "?limit=4").await;

    assert_eq!(page["items"].as_array().unwrap().len(), 4);
    assert!(page["nextCursor"].is_string());
}

#[tokio::test]
async fn the_cursor_of_a_page_leads_to_the_next_one() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;
    for day in 1..=3 {
        let mut body = expense(&session);
        body["occurredOn"] = json!(format!("2026-09-0{day}"));
        post(&harness, &session, &body).await;
    }

    let (_, first) = get(&harness, &session, "?limit=2").await;
    let cursor = first["nextCursor"].as_str().unwrap().to_owned();
    let (_, second) = get(&harness, &session, &format!("?limit=2&cursor={cursor}")).await;

    assert_eq!(first["items"].as_array().unwrap().len(), 2);
    assert_eq!(second["items"].as_array().unwrap().len(), 1);
    assert!(second["nextCursor"].is_null());
    assert_eq!(second["items"][0]["occurredOn"], "2026-09-01");
}

#[tokio::test]
async fn the_period_and_the_kind_filters_match_the_screen() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;
    let mut august = expense(&session);
    august["occurredOn"] = json!("2026-08-25");
    post(&harness, &session, &august).await;
    post(&harness, &session, &expense(&session)).await;
    post(
        &harness,
        &session,
        &json!({
            "occurredOn": "2026-09-01",
            "kind": "in",
            "categoryId": session.income_category,
            "amount": "2800000",
            "currency": "COP",
        }),
    )
    .await;

    let (_, month) = get(&harness, &session, "?from=2026-09-01&to=2026-09-30").await;
    let (_, income) = get(&harness, &session, "?from=2026-09-01&to=2026-09-30&kind=in").await;

    assert_eq!(month["items"].as_array().unwrap().len(), 2);
    assert_eq!(income["items"].as_array().unwrap().len(), 1);
    assert_eq!(income["items"][0]["kind"], "in");
}

#[tokio::test]
async fn one_account_never_sees_the_movements_of_another() {
    let harness = Harness::new();
    let camilo = sign_up(&harness, "camilo@correo.com").await;
    let rosa = sign_up(&harness, "rosa@correo.com").await;
    post(&harness, &camilo, &expense(&camilo)).await;

    let (status, page) = get(&harness, &rosa, "").await;

    assert_eq!(status, Some(StatusCode::OK));
    assert!(page["items"].as_array().unwrap().is_empty());
    assert_ne!(camilo.user_id, rosa.user_id);
}

#[tokio::test]
async fn a_category_of_another_account_answers_422_and_writes_nothing() {
    let harness = Harness::new();
    let camilo = sign_up(&harness, "camilo@correo.com").await;
    let rosa = sign_up(&harness, "rosa@correo.com").await;

    let mut body = expense(&camilo);
    body["categoryId"] = json!(rosa.expense_category);
    let (status, problem) = post(&harness, &camilo, &body).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "categoryId");
    let (_, page) = get(&harness, &camilo, "").await;
    assert!(page["items"].as_array().unwrap().is_empty());
}

#[tokio::test]
async fn an_expense_with_an_income_category_answers_422() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let mut body = expense(&session);
    body["categoryId"] = json!(session.income_category);
    let (status, problem) = post(&harness, &session, &body).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "categoryId");
}

#[tokio::test]
async fn a_negative_amount_answers_422() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let mut body = expense(&session);
    body["amount"] = json!("-1");
    let (status, problem) = post(&harness, &session, &body).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "amount");
    assert_eq!(problem["status"], 422);
}

#[tokio::test]
async fn an_amount_sent_as_a_number_is_rejected() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let mut body = expense(&session);
    body["amount"] = json!(186_400);
    let (status, problem) = post(&harness, &session, &body).await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "body");
}

#[tokio::test]
async fn a_broken_period_answers_422_naming_the_parameter() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let (status, problem) = get(&harness, &session, "?from=2026-09-30&to=2026-09-01").await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "from");
}

#[tokio::test]
async fn a_bogus_cursor_answers_422() {
    let harness = Harness::new();
    let session = sign_up(&harness, "camilo@correo.com").await;

    let (status, problem) = get(&harness, &session, "?cursor=no-soy-un-cursor").await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(problem["errors"][0]["field"], "cursor");
}

#[tokio::test]
async fn both_endpoints_need_an_access_token() {
    let harness = Harness::new();

    let listed = TestClient::get("http://localhost/v1/movements")
        .send(&harness.service)
        .await;
    let created = TestClient::post("http://localhost/v1/movements")
        .json(&json!({}))
        .send(&harness.service)
        .await;

    assert_eq!(listed.status_code, Some(StatusCode::UNAUTHORIZED));
    assert_eq!(created.status_code, Some(StatusCode::UNAUTHORIZED));
}
