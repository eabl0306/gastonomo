//! Tests de integracion del archivado de categorias.
//!
//! La regla que vigilan: **`DELETE` archiva en vez de destruir** cuando algun
//! movimiento usa la categoria, asi que un gasto viejo se sigue leyendo con la
//! categoria que tenia el dia que se registro. Solo desaparece de verdad cuando no
//! la usa nadie.

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

use category_client::{
    create, delete, entry_of, given_movements, list, list_at, list_including_archived,
    new_category, restore, sign_up,
};
use support::Harness;

#[tokio::test]
async fn a_category_nobody_used_is_deleted_for_real() {
    let harness = Harness::new();
    let session = sign_up(&harness, "borrar@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap();

    let (status, body) = delete(&harness, &session, id).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["outcome"], "deleted");
    let (_, listed) = list(&harness, &session).await;
    assert_eq!(listed["items"].as_array().unwrap().len(), 12);
    // Ni archivada: no hay historico que proteger, asi que la fila se va.
    let (_, everything) = list_including_archived(&harness, &session).await;
    assert!(entry_of(&everything, id).is_none());
}

#[tokio::test]
async fn a_category_with_movements_is_archived_instead_of_destroyed() {
    let harness = Harness::new();
    let session = sign_up(&harness, "archivar@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    given_movements(&harness, &id, 4);

    let (status, body) = delete(&harness, &session, &id).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(
        body["outcome"], "archived",
        "el cliente necesita saber cual de las dos cosas paso"
    );
    let (_, listed) = list(&harness, &session).await;
    assert!(
        entry_of(&listed, &id).is_none(),
        "deja de ofrecerse en la lista de siempre"
    );
    let (_, everything) = list_including_archived(&harness, &session).await;
    let entry = entry_of(&everything, &id).expect("sigue estando, archivada");
    assert_eq!(entry["archived"], true);
    assert_eq!(
        entry["movementCount"], 4,
        "los movimientos siguen apuntando a ella"
    );
}

#[tokio::test]
async fn archiving_twice_answers_the_same() {
    let harness = Harness::new();
    let session = sign_up(&harness, "dosveces@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    given_movements(&harness, &id, 1);

    let (first, first_body) = delete(&harness, &session, &id).await;
    let (second, second_body) = delete(&harness, &session, &id).await;

    assert_eq!(first, Some(StatusCode::OK));
    assert_eq!(second, Some(StatusCode::OK));
    assert_eq!(first_body["outcome"], "archived");
    assert_eq!(second_body["outcome"], "archived");
    let (_, everything) = list_including_archived(&harness, &session).await;
    assert_eq!(
        everything["items"]
            .as_array()
            .unwrap()
            .iter()
            .filter(|entry| entry["id"] == id.as_str())
            .count(),
        1
    );
}

#[tokio::test]
async fn an_active_category_says_it_is_not_archived() {
    let harness = Harness::new();
    let session = sign_up(&harness, "activa@correo.com").await;

    let (_, created) = create(&harness, &session, &new_category()).await;

    assert_eq!(created["archived"], false);
    let (_, listed) = list(&harness, &session).await;
    assert!(
        listed["items"]
            .as_array()
            .unwrap()
            .iter()
            .all(|entry| entry["archived"] == false)
    );
}

#[tokio::test]
async fn restoring_puts_an_archived_category_back_in_the_list() {
    let harness = Harness::new();
    let session = sign_up(&harness, "recuperar@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    given_movements(&harness, &id, 2);
    delete(&harness, &session, &id).await;

    let (status, body) = restore(&harness, &session, &id).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["archived"], false);
    assert_eq!(body["name"], "Mascotas");
    let (_, listed) = list(&harness, &session).await;
    assert!(entry_of(&listed, &id).is_some());
}

#[tokio::test]
async fn restoring_one_that_was_never_archived_answers_the_same() {
    let harness = Harness::new();
    let session = sign_up(&harness, "yaactiva@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();

    let (status, body) = restore(&harness, &session, &id).await;

    assert_eq!(status, Some(StatusCode::OK));
    assert_eq!(body["archived"], false);
}

#[tokio::test]
async fn a_default_category_cannot_be_restored_either() {
    let harness = Harness::new();
    let session = sign_up(&harness, "recuperarpredeterminada@correo.com").await;
    let (_, listed) = list(&harness, &session).await;
    let id = listed["items"][0]["id"].as_str().unwrap().to_owned();

    let (status, body) = restore(&harness, &session, &id).await;

    assert_eq!(status, Some(StatusCode::CONFLICT));
    assert_eq!(body["code"], "category_is_default");
}

#[tokio::test]
async fn restoring_a_category_of_another_account_answers_not_found() {
    let harness = Harness::new();
    let owner = sign_up(&harness, "duena@correo.com").await;
    let intruder = sign_up(&harness, "colada@correo.com").await;
    let (_, created) = create(&harness, &owner, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    given_movements(&harness, &id, 1);
    delete(&harness, &owner, &id).await;

    let (status, body) = restore(&harness, &intruder, &id).await;

    assert_eq!(status, Some(StatusCode::NOT_FOUND));
    assert_eq!(body["type"], "https://gastonomo.app/problems/not-found");
}

#[tokio::test]
async fn an_identifier_that_is_not_a_uuid_answers_not_found_on_restore_too() {
    let harness = Harness::new();
    let session = sign_up(&harness, "basurarestore@correo.com").await;

    let (status, _) = restore(&harness, &session, "no-es-un-uuid").await;

    assert_eq!(status, Some(StatusCode::NOT_FOUND));
}

#[tokio::test]
async fn only_the_exact_true_brings_the_archived_ones() {
    let harness = Harness::new();
    let session = sign_up(&harness, "flag@correo.com").await;
    let (_, created) = create(&harness, &session, &new_category()).await;
    let id = created["id"].as_str().unwrap().to_owned();
    given_movements(&harness, &id, 1);
    delete(&harness, &session, &id).await;

    for query in [
        "",
        "?includeArchived",
        "?includeArchived=1",
        "?includeArchived=TRUE",
    ] {
        let (status, listed) = list_at(
            &harness,
            &session,
            &format!("http://localhost/v1/categories{query}"),
        )
        .await;
        assert_eq!(status, Some(StatusCode::OK), "{query}");
        assert!(entry_of(&listed, &id).is_none(), "{query}");
    }
}
