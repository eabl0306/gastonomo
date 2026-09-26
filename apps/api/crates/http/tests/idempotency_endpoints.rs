//! Idempotencia de los tres POST que crean dinero.
//!
//! El caso que hay que impedir es este: el usuario guarda un gasto, pierde
//! cobertura antes de ver la respuesta y la aplicacion reintenta con la misma
//! `Idempotency-Key`. Sin proteccion quedan dos gastos y el diseno no tiene pantalla
//! para borrar uno.

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
use gastonomo_http::{IDEMPOTENCY_HEADER, REPLAY_HEADER};
use salvo::prelude::StatusCode;
use salvo::test::{ResponseExt as _, TestClient};
use serde_json::{Value, json};
use support::Harness;

struct Fixture {
    harness: Harness,
    token: String,
    category: String,
}

async fn fixture(email: &str) -> Fixture {
    let harness = Harness::new();
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
    let token = body["accessToken"]
        .as_str()
        .expect("access token")
        .to_owned();
    let user_id = body["user"]["id"].as_str().expect("user id");
    let category = harness.seed_category(user_id, "Mercado", CategoryKind::Out);
    Fixture {
        harness,
        token,
        category,
    }
}

async fn post(
    fixture: &Fixture,
    path: &str,
    key: Option<&str>,
    body: &Value,
) -> (Option<StatusCode>, Value, bool) {
    let mut request = TestClient::post(format!("http://localhost{path}"))
        .bearer_auth(fixture.token.clone())
        .json(body);
    if let Some(key) = key {
        request = request.add_header(IDEMPOTENCY_HEADER, key, true);
    }
    let mut response = request.send(&fixture.harness.service).await;
    let status = response.status_code;
    let replayed = response.headers().contains_key(REPLAY_HEADER);
    let payload = response.take_json::<Value>().await.unwrap_or(Value::Null);
    (status, payload, replayed)
}

fn a_movement(category: &str) -> Value {
    json!({
        "occurredOn": "2026-09-15",
        "kind": "out",
        "categoryId": category,
        "note": "Mercado",
        "amount": "186400",
        "currency": "COP",
    })
}

#[tokio::test]
async fn the_same_key_twice_writes_one_movement_and_repeats_the_answer() {
    let fixture = fixture("reintento@correo.com").await;
    let body = a_movement(&fixture.category);

    let (first_status, first, first_replayed) =
        post(&fixture, "/v1/movements", Some("intento-1"), &body).await;
    let (second_status, second, second_replayed) =
        post(&fixture, "/v1/movements", Some("intento-1"), &body).await;

    assert_eq!(first_status, Some(StatusCode::CREATED));
    assert_eq!(second_status, Some(StatusCode::CREATED));
    assert!(!first_replayed, "el primer intento no es una repeticion");
    assert!(second_replayed, "el segundo si lo es");
    assert_eq!(
        first, second,
        "el reintento recibe el mismo movimiento, con el mismo id"
    );
    assert_eq!(
        fixture.harness.movements.all().len(),
        1,
        "solo se guardo un movimiento"
    );
}

#[tokio::test]
async fn without_the_header_nothing_changes_and_two_posts_are_two_movements() {
    let fixture = fixture("sinclave@correo.com").await;
    let body = a_movement(&fixture.category);

    let (first_status, _, _) = post(&fixture, "/v1/movements", None, &body).await;
    let (second_status, _, second_replayed) = post(&fixture, "/v1/movements", None, &body).await;

    assert_eq!(first_status, Some(StatusCode::CREATED));
    assert_eq!(second_status, Some(StatusCode::CREATED));
    assert!(!second_replayed);
    assert_eq!(fixture.harness.movements.all().len(), 2);
    assert_eq!(
        fixture.harness.idempotency.count(),
        0,
        "sin cabecera no se toca el almacen"
    );
}

#[tokio::test]
async fn two_different_keys_are_two_movements() {
    let fixture = fixture("dosclaves@correo.com").await;
    let body = a_movement(&fixture.category);

    post(&fixture, "/v1/movements", Some("intento-1"), &body).await;
    post(&fixture, "/v1/movements", Some("intento-2"), &body).await;

    assert_eq!(fixture.harness.movements.all().len(), 2);
}

#[tokio::test]
async fn the_same_key_with_another_body_is_a_conflict_and_not_a_silent_loss() {
    let fixture = fixture("reutilizada@correo.com").await;
    let first = a_movement(&fixture.category);
    let mut second = first.clone();
    second["amount"] = json!("999999");

    post(&fixture, "/v1/movements", Some("intento-1"), &first).await;
    let (status, body, _) = post(&fixture, "/v1/movements", Some("intento-1"), &second).await;

    assert_eq!(status, Some(StatusCode::CONFLICT));
    assert_eq!(body["code"], "idempotency_key_reused");
    assert_eq!(
        fixture.harness.movements.all().len(),
        1,
        "el segundo cuerpo no se guarda, y el cliente se entera"
    );
}

#[tokio::test]
async fn a_rejected_body_does_not_burn_the_key() {
    let fixture = fixture("rechazado@correo.com").await;
    let mut broken = a_movement(&fixture.category);
    broken["amount"] = json!("no-es-un-numero");

    let (rejected, _, _) = post(&fixture, "/v1/movements", Some("intento-1"), &broken).await;
    let (accepted, _, replayed) = post(
        &fixture,
        "/v1/movements",
        Some("intento-1"),
        &a_movement(&fixture.category),
    )
    .await;

    assert_eq!(rejected, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(
        accepted,
        Some(StatusCode::CREATED),
        "corregir el importe con la misma clave tiene que poder guardarse"
    );
    assert!(!replayed);
    assert_eq!(fixture.harness.movements.all().len(), 1);
}

#[tokio::test]
async fn an_empty_key_is_rejected_on_the_header() {
    let fixture = fixture("clavevacia@correo.com").await;

    let (status, body, _) = post(
        &fixture,
        "/v1/movements",
        Some("   "),
        &a_movement(&fixture.category),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    assert_eq!(body["errors"][0]["field"], "Idempotency-Key");
    assert_eq!(fixture.harness.movements.all().len(), 0);
}

#[tokio::test]
async fn a_loan_and_a_payment_are_protected_too() {
    let fixture = fixture("prestamo@correo.com").await;
    let loan_body = json!({
        "person": "Rosa",
        "direction": "lent",
        "openedOn": "2026-06-28",
        "amount": "600000",
        "currency": "COP",
    });

    let (first_status, loan, _) = post(&fixture, "/v1/loans", Some("prestamo-1"), &loan_body).await;
    let (second_status, repeated, replayed) =
        post(&fixture, "/v1/loans", Some("prestamo-1"), &loan_body).await;

    assert_eq!(first_status, Some(StatusCode::CREATED));
    assert_eq!(second_status, Some(StatusCode::CREATED));
    assert!(replayed);
    assert_eq!(loan["id"], repeated["id"]);

    let loan_id = loan["id"].as_str().expect("loan id");
    let payment = json!({"paidOn": "2026-07-29", "amount": "200000", "currency": "COP"});
    let path = format!("/v1/loans/{loan_id}/payments");
    post(&fixture, &path, Some("abono-1"), &payment).await;
    let (status, after, replayed) = post(&fixture, &path, Some("abono-1"), &payment).await;

    assert_eq!(status, Some(StatusCode::CREATED));
    assert!(replayed);
    assert_eq!(
        after["payments"].as_array().map(Vec::len),
        Some(1),
        "el reintento no anade un segundo abono"
    );
}

#[tokio::test]
async fn the_same_key_on_two_different_operations_does_not_collide() {
    let fixture = fixture("mismaclave@correo.com").await;

    let (movement, _, _) = post(
        &fixture,
        "/v1/movements",
        Some("una-sola-clave"),
        &a_movement(&fixture.category),
    )
    .await;
    let (loan, _, replayed) = post(
        &fixture,
        "/v1/loans",
        Some("una-sola-clave"),
        &json!({
            "person": "Rosa",
            "direction": "lent",
            "openedOn": "2026-06-28",
            "amount": "600000",
            "currency": "COP",
        }),
    )
    .await;

    assert_eq!(movement, Some(StatusCode::CREATED));
    assert_eq!(loan, Some(StatusCode::CREATED));
    assert!(!replayed, "son dos operaciones distintas, no un reintento");
}
