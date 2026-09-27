//! Tests de integracion de los endpoints de categorias: listar, crear y editar.
//!
//! Atraviesan el router entero: middleware de access token, handler, caso de uso y
//! DTO, con dobles en memoria en lugar de PostgreSQL. El archivado y la
//! recuperacion estan en `category_archiving.rs`.

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

mod category_client;
mod support;

use salvo::prelude::StatusCode;
use salvo::test::TestClient;
use serde_json::json;

use category_client::{create, delete, list, new_category, patch, sign_up};
use support::Harness;

#[tokio::test]
async fn the_list_carries_the_twelve_defaults_with_their_usage_text_data() {
    let harness = Harness::new();
    let session = sign_up(&harness, "lista@correo.com").await;

    let (status, body) = list(&harness, &session).await;

    assert_eq!(status, Some(StatusCode::OK));
    let categories = body["items"].as_array().unwrap();
    assert_eq!(categories.len(), 12);
    assert_eq!(categories[0]["isDefault"], true);
    assert_eq!(categories[0]["movementCount"], 0);
    let shared = categories
        .iter()
        .filter(|entry| entry["kind"] == "both")
        .count();
    assert_eq!(
        shared, 3,
        "Prestamo, Abono prestamo y Otros sirven los dos grids"
    );
}

#[tokio::test]
async fn without_a_token_there_is_no_list() {
    let harness = Harness::new();

    let response = TestClient::get("http://localhost/v1/categories")
        .send(&harness.service)
        .await;

    assert_eq!(response.status_code, Some(StatusCode::UNAUTHORIZED));
}

#[tokio::test]
async fn a_created_category_comes_back_with_its_tile_and_zero_movements() {
    let harness = Harness::new();
    let session = sign_up(&harness, "crear@correo.com").await;

    let (status, body) = create(&harness, &session, &new_category()).await;

    assert_eq!(status, Some(StatusCode::CREATED));
    assert_eq!(body["name"], "Mascotas");
    assert_eq!(body["kind"], "out");
    assert_eq!(body["icon"], "pets");
    assert_eq!(body["color"], "#2f7fb5");
    assert_eq!(body["isDefault"], false);
    assert_eq!(body["movementCount"], 0);
    assert!(body["id"].as_str().is_some());

    let (_, listed) = list(&harness, &session).await;
    assert_eq!(listed["items"].as_array().unwrap().len(), 13);
}

#[tokio::test]
async fn a_repeated_name_is_a_conflict_with_a_stable_code() {
    let harness = Harness::new();
    let session = sign_up(&harness, "repetida@correo.com").await;

    let (status, body) = create(
        &harness,
        &session,
        &json!({"name": "  salario  ", "kind": "out", "icon": "pets", "color": "#2f7fb5"}),
    )
    .await;

    assert_eq!(status, Some(StatusCode::CONFLICT));
    assert_eq!(body["code"], "category_name_taken");
    assert_eq!(body["status"], 409);
    assert_eq!(body["type"], "https://gastonomo.app/problems/conflict");
}

#[tokio::test]
async fn an_icon_outside_the_sheet_is_rejected_field_by_field() {
    let harness = Harness::new();
    let session = sign_up(&harness, "icono@correo.com").await;

    let (status, body) = create(
        &harness,
        &session,
        &json!({"name": "Mascotas", "kind": "both", "icon": "rocket_launch", "color": "#123456"}),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
    let fields: Vec<&str> = body["errors"]
        .as_array()
        .unwrap()
        .iter()
        .map(|entry| entry["field"].as_str().unwrap())
        .collect();
    assert!(fields.contains(&"icon"));
    assert!(fields.contains(&"color"));
}

#[tokio::test]
async fn an_unknown_field_in_the_body_is_rejected() {
    let harness = Harness::new();
    let session = sign_up(&harness, "extra@correo.com").await;

    let (status, _) = create(
        &harness,
        &session,
        &json!({"name": "Mascotas", "kind": "out", "icon": "pets", "color": "#2f7fb5",
                "isDefault": true}),
    )
    .await;

    assert_eq!(status, Some(StatusCode::UNPROCESSABLE_ENTITY));
}

#[tokio::test]
async fn editing_changes_only_what_the_patch_brings() {
    let harness = Harness::new();
    let session = sign_up(&harness, "editar@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap();

    let (status, body) = patch(&harness, &session, id, &json!({"name": "Veterinario"})).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["name"], "Veterinario");
    assert_eq!(body["icon"], "pets");
    assert_eq!(body["color"], "#2f7fb5");
}

#[tokio::test]
async fn a_default_category_answers_conflict_on_edit_and_on_delete() {
    let harness = Harness::new();
    let session = sign_up(&harness, "predeterminada@correo.com").await;
    let (_, listed) = list(&harness, &session).await;
    let id = listed["items"][0]["id"].as_str().unwrap().to_owned();

    let (edit_status, edit_body) =
        patch(&harness, &session, &id, &json!({"name": "Otra cosa"})).await;
    assert_eq!(edit_status, Some(StatusCode::CONFLICT));
    assert_eq!(edit_body["code"], "category_is_default");

    let (delete_status, delete_body) = delete(&harness, &session, &id).await;
    assert_eq!(delete_status, Some(StatusCode::CONFLICT));
    assert_eq!(delete_body["code"], "category_is_default");
}

#[tokio::test]
async fn an_identifier_that_is_not_a_uuid_answers_not_found() {
    let harness = Harness::new();
    let session = sign_up(&harness, "basura@correo.com").await;

    let (status, _) = delete(&harness, &session, "no-es-un-uuid").await;

    assert_eq!(status, Some(StatusCode::NOT_FOUND));
}

#[tokio::test]
async fn a_category_of_another_account_answers_not_found_and_never_403() {
    let harness = Harness::new();
    let owner = sign_up(&harness, "dueno@correo.com").await;
    let intruder = sign_up(&harness, "intruso@correo.com").await;
    let (_, created) = create(&harness, &owner, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    assert_ne!(owner.user_id, intruder.user_id);

    let (read_status, listed) = list(&harness, &intruder).await;
    assert_eq!(read_status, Some(StatusCode::OK));
    assert!(
        !listed["items"]
            .as_array()
            .unwrap()
            .iter()
            .any(|entry| entry["id"] == id.as_str())
    );

    let (edit_status, edit_body) =
        patch(&harness, &intruder, &id, &json!({"name": "Robada"})).await;
    assert_eq!(edit_status, Some(StatusCode::NOT_FOUND));
    assert_eq!(
        edit_body["type"],
        "https://gastonomo.app/problems/not-found"
    );

    let (delete_status, _) = delete(&harness, &intruder, &id).await;
    assert_eq!(delete_status, Some(StatusCode::NOT_FOUND));

    let (_, still_there) = list(&harness, &owner).await;
    assert!(
        still_there["items"]
            .as_array()
            .unwrap()
            .iter()
            .any(|entry| entry["id"] == id.as_str())
    );
}
