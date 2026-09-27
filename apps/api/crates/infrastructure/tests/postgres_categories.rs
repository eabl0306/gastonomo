//! Tests de integracion del adaptador de categorias contra PostgreSQL.
//!
//! Lo que vigilan de verdad: **archivar no toca ningun movimiento**. El borrado
//! reasignaba `category_id`, y era el ultimo camino por el que se podia cambiar
//! como se lee el historico.
//!
//! Mismas reglas que `postgres_integration.rs`: sin `GASTONOMO_TEST_DATABASE_URL`
//! cada test se salta a si mismo, y con `GASTONOMO_TEST_REQUIRE_DATABASE=1` la
//! falta de base de datos es un fallo.
//!
//! ```sh
//! just api-test-db
//! ```

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

mod account_fixture;

use chrono::Utc;
use gastonomo_application::port::category::CategoryRepository as _;
use gastonomo_domain::category::{
    Category, CategoryKind, CategoryName, DEFAULT_CATEGORIES, FALLBACK_CATEGORY_NAME, NewCategory,
};
use gastonomo_infrastructure::persistence::PostgresCategoryRepository;

use account_fixture::{a_custom_category, a_movement, an_account, category_of, pool};

#[tokio::test]
async fn a_new_account_starts_with_the_twelve_defaults_and_no_usage() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());

    let listed = categories
        .list_with_usage(user_id, false)
        .await
        .expect("should list");

    assert_eq!(listed.len(), DEFAULT_CATEGORIES.len());
    assert!(listed.iter().all(|entry| entry.category.is_default()));
    assert!(listed.iter().all(|entry| entry.movements == 0));
    let shared = listed
        .iter()
        .filter(|entry| entry.category.kind() == CategoryKind::Both)
        .count();
    assert_eq!(shared, 3);
}

#[tokio::test]
async fn a_custom_category_round_trips_and_the_name_is_unique_ignoring_case() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(user_id, "Mascotas");

    categories.insert(&category).await.expect("should insert");

    let found = categories
        .find(user_id, category.id())
        .await
        .expect("should query")
        .expect("the category exists");
    assert_eq!(found.category.name().as_str(), "Mascotas");
    assert_eq!(found.category.icon().as_str(), "pets");
    assert_eq!(found.category.color().as_str(), "#2f7fb5");
    assert!(!found.category.is_default());
    assert_eq!(found.movements, 0);

    let repeated = categories
        .insert(&a_custom_category(user_id, "MASCOTAS"))
        .await;
    let Err(gastonomo_application::error::AppError::Conflict { code, .. }) = repeated else {
        unreachable!("a repeated name must be a conflict")
    };
    assert_eq!(code, "category_name_taken");
}

#[tokio::test]
async fn the_usage_count_only_counts_the_movements_of_that_category() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");
    a_movement(&pool, user_id, category.id().as_uuid()).await;
    a_movement(&pool, user_id, category.id().as_uuid()).await;

    let found = categories
        .find(user_id, category.id())
        .await
        .expect("should query")
        .expect("the category exists");
    assert_eq!(found.movements, 2);

    let otros = categories
        .find_by_name(user_id, FALLBACK_CATEGORY_NAME)
        .await
        .expect("should query")
        .expect("Otros is seeded");
    let otros_usage = categories
        .find(user_id, otros.id())
        .await
        .expect("should query")
        .expect("Otros exists");
    assert_eq!(otros_usage.movements, 0);
}

#[tokio::test]
async fn archiving_a_used_category_leaves_every_movement_where_it_was() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let mut category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");
    let movement = a_movement(&pool, user_id, category.id().as_uuid()).await;
    let before = category_of(&pool, movement).await;

    category
        .archive(Utc::now())
        .expect("custom categories archive");
    categories.update(&category).await.expect("should archive");

    // Esta es la garantia entera: la fila de movements no se toca. Antes, borrar
    // ejecutaba `UPDATE movements SET category_id` y el gasto pasaba a leerse con
    // otra categoria.
    assert_eq!(category_of(&pool, movement).await, before);
    let stored = categories
        .find(user_id, category.id())
        .await
        .expect("should query")
        .expect("the category is still there");
    assert!(stored.category.is_archived());
}

#[tokio::test]
async fn the_default_listing_leaves_the_archived_out() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let mut category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");
    a_movement(&pool, user_id, category.id().as_uuid()).await;
    category
        .archive(Utc::now())
        .expect("custom categories archive");
    categories.update(&category).await.expect("should archive");

    let active = categories
        .list_with_usage(user_id, false)
        .await
        .expect("should list");
    let all = categories
        .list_with_usage(user_id, true)
        .await
        .expect("should list");

    assert!(
        active
            .iter()
            .all(|found| found.category.id() != category.id())
    );
    assert!(all.iter().any(|found| found.category.id() == category.id()));
}

#[tokio::test]
async fn a_category_no_movement_uses_is_deleted_for_real() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");

    categories
        .delete_unused(user_id, category.id())
        .await
        .expect("should delete");

    assert!(
        categories
            .find(user_id, category.id())
            .await
            .expect("should query")
            .is_none()
    );
}

#[tokio::test]
async fn a_category_with_movements_is_never_deleted_for_real() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");
    a_movement(&pool, user_id, category.id().as_uuid()).await;

    // La guarda esta en el WHERE y no solo en el caso de uso: entre la comprobacion
    // y el borrado puede llegar un movimiento.
    let deleted = categories.delete_unused(user_id, category.id()).await;

    assert!(deleted.is_err());
    assert!(
        categories
            .find(user_id, category.id())
            .await
            .expect("should query")
            .is_some()
    );
}

#[tokio::test]
async fn a_default_category_cannot_be_updated_nor_removed_in_the_database() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let salario = categories
        .find_by_name(user_id, "Salario")
        .await
        .expect("should query")
        .expect("Salario is seeded");
    let renamed = Category::rehydrate(
        salario.id(),
        NewCategory {
            user_id,
            name: CategoryName::parse("Sueldo").expect("valid"),
            kind: salario.kind(),
            icon: salario.icon().clone(),
            color: salario.color().clone(),
            created_at: salario.created_at(),
        },
        false,
        Utc::now(),
        None,
    );

    let update = categories.update(&renamed).await;
    assert!(update.is_err(), "the WHERE clause guards is_default");

    let delete = categories.delete_unused(user_id, salario.id()).await;
    assert!(delete.is_err(), "a default row is never deleted");
    assert!(
        categories
            .find(user_id, salario.id())
            .await
            .expect("should query")
            .is_some()
    );
}

#[tokio::test]
async fn a_category_of_another_account_is_invisible() {
    let Some(pool) = pool().await else { return };
    let owner = an_account(&pool).await;
    let intruder = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(owner, "Mascotas");
    categories.insert(&category).await.expect("should insert");

    assert!(
        categories
            .find(intruder, category.id())
            .await
            .expect("should query")
            .is_none()
    );
    let stolen = categories.delete_unused(intruder, category.id()).await;
    assert!(stolen.is_err());
    assert!(
        categories
            .find(owner, category.id())
            .await
            .expect("should query")
            .is_some()
    );
}
