//! Tests de integracion del feed de cambios contra PostgreSQL.
//!
//! Aqui se prueba lo unico que de verdad importa del cursor: **que no se salte
//! nada**. Es la propiedad que hace que un balance cuadre, y la que una marca de
//! tiempo no puede dar.
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

use gastonomo_application::port::category::CategoryRepository as _;
use gastonomo_application::port::changes::{ChangeBatch, ChangeFeedReader as _, ChangeNumber};
use gastonomo_domain::category::{CategoryChange, CategoryName};
use gastonomo_domain::value_object::UserId;
use gastonomo_infrastructure::PgPool;
use gastonomo_infrastructure::persistence::{PostgresCategoryRepository, PostgresChangeFeed};

use account_fixture::{a_custom_category, a_movement, an_account, pool};

/// Todo el feed de la cuenta, en lotes de `limit`, sin perder nada por el camino.
///
/// Es como lo lee un cliente: pide, guarda, avanza el cursor y vuelve a pedir.
async fn drain(feed: &PostgresChangeFeed, user_id: UserId, limit: u32) -> ChangeBatch {
    let mut all = ChangeBatch::empty(0);
    let mut since: Option<ChangeNumber> = None;
    loop {
        let batch = feed
            .changes_since(user_id, since, limit)
            .await
            .expect("should read the feed");
        all.movements.extend(batch.movements);
        all.loans.extend(batch.loans);
        all.payments.extend(batch.payments);
        all.categories.extend(batch.categories);
        if batch.settings.is_some() {
            all.settings = batch.settings;
        }
        all.cursor = batch.cursor;
        if !batch.has_more {
            return all;
        }
        assert_ne!(
            since,
            Some(batch.cursor),
            "el cursor no avanza: bucle infinito"
        );
        since = Some(batch.cursor);
    }
}

#[tokio::test]
async fn a_cold_start_brings_the_twelve_defaults_and_the_settings() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());

    let batch = feed
        .changes_since(user_id, None, 100)
        .await
        .expect("should read the feed");

    assert_eq!(batch.categories.len(), 12);
    assert!(
        batch.settings.is_some(),
        "un cliente en frio necesita los ajustes"
    );
    assert!(batch.cursor > 0);
    assert!(!batch.has_more);
}

#[tokio::test]
async fn a_client_already_up_to_date_gets_nothing_and_keeps_its_cursor() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let first = feed
        .changes_since(user_id, None, 100)
        .await
        .expect("should read");

    let second = feed
        .changes_since(user_id, Some(first.cursor), 100)
        .await
        .expect("should read");

    assert!(second.categories.is_empty());
    assert!(second.settings.is_none());
    assert_eq!(second.cursor, first.cursor, "el cursor no puede retroceder");
    assert!(!second.has_more);
}

#[tokio::test]
async fn two_rows_created_in_the_same_millisecond_both_come_out() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let start = feed
        .changes_since(user_id, None, 100)
        .await
        .expect("should read");
    let categories = PostgresCategoryRepository::new(pool.clone());
    // En la misma transaccion y con el mismo `now()`: created_at es identico al
    // microsegundo, que es el caso donde un cursor por fecha se come una fila.
    let uno = a_custom_category(user_id, "Uno");
    let dos = a_custom_category(user_id, "Dos");
    categories.insert(&uno).await.expect("should insert");
    categories.insert(&dos).await.expect("should insert");

    let batch = drain(&feed, user_id, 1).await;

    let names: Vec<String> = batch
        .categories
        .iter()
        .map(|usage| usage.category.name().to_string())
        .collect();
    assert!(names.contains(&"Uno".to_owned()), "{names:?}");
    assert!(names.contains(&"Dos".to_owned()), "{names:?}");
    assert!(batch.cursor > start.cursor);
}

#[tokio::test]
async fn a_clock_that_goes_backwards_does_not_hide_a_row() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let categories = PostgresCategoryRepository::new(pool.clone());
    let reciente = a_custom_category(user_id, "Reciente");
    categories.insert(&reciente).await.expect("should insert");
    let after = feed
        .changes_since(user_id, None, 100)
        .await
        .expect("should read");

    // La siguiente fila se guarda con una fecha del pasado, como si el reloj se
    // hubiera ajustado hacia atras. Con un cursor por fecha esto no volveria a
    // salir nunca.
    let vieja = a_custom_category(user_id, "Vieja");
    categories.insert(&vieja).await.expect("should insert");
    sqlx::query("UPDATE categories SET created_at = now() - interval '2 years' WHERE id = $1")
        .bind(vieja.id().as_uuid())
        .execute(&pool)
        .await
        .expect("should move the row into the past");

    let batch = feed
        .changes_since(user_id, Some(after.cursor), 100)
        .await
        .expect("should read");

    let names: Vec<String> = batch
        .categories
        .iter()
        .map(|usage| usage.category.name().to_string())
        .collect();
    assert_eq!(names, vec!["Vieja".to_owned()]);
}

#[tokio::test]
async fn a_renamed_or_archived_category_is_a_change() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let categories = PostgresCategoryRepository::new(pool.clone());
    let mut mascotas = a_custom_category(user_id, "Mascotas");
    categories.insert(&mascotas).await.expect("should insert");
    let after = feed
        .changes_since(user_id, None, 100)
        .await
        .expect("should read");

    mascotas
        .apply(
            CategoryChange {
                name: Some(CategoryName::parse("Veterinario").expect("valid")),
                ..CategoryChange::default()
            },
            chrono::Utc::now(),
        )
        .expect("a custom category can be renamed");
    categories.update(&mascotas).await.expect("should update");

    let renamed = feed
        .changes_since(user_id, Some(after.cursor), 100)
        .await
        .expect("should read");
    assert_eq!(
        renamed
            .categories
            .iter()
            .map(|usage| usage.category.name().to_string())
            .collect::<Vec<_>>(),
        vec!["Veterinario".to_owned()]
    );

    mascotas
        .archive(chrono::Utc::now())
        .expect("a custom category can be archived");
    categories.update(&mascotas).await.expect("should update");

    let archived = feed
        .changes_since(user_id, Some(renamed.cursor), 100)
        .await
        .expect("should read");
    assert!(
        archived.categories[0].category.is_archived(),
        "archivar es un cambio, no un borrado"
    );
}

#[tokio::test]
async fn the_feed_never_crosses_accounts() {
    let Some(pool) = pool().await else { return };
    let mine = an_account(&pool).await;
    let theirs = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let categories = PostgresCategoryRepository::new(pool.clone());
    let ajena = a_custom_category(theirs, "Ajena");
    categories.insert(&ajena).await.expect("should insert");

    let batch = drain(&feed, mine, 100).await;

    assert!(
        batch
            .categories
            .iter()
            .all(|usage| usage.category.user_id() == mine)
    );
}

#[tokio::test]
async fn a_movement_arrives_with_its_category_and_its_four_amounts() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let categories = PostgresCategoryRepository::new(pool.clone());
    let mascotas = a_custom_category(user_id, "Mascotas");
    categories.insert(&mascotas).await.expect("should insert");
    a_movement(&pool, user_id, mascotas.id().as_uuid()).await;

    let batch = drain(&feed, user_id, 5).await;

    assert_eq!(batch.movements.len(), 1);
    let movement = &batch.movements[0];
    assert_eq!(movement.category().name(), "Mascotas");
    assert_eq!(movement.amount().amount().to_storage_string(), "140.0000");
    assert_eq!(movement.amount().base().to_storage_string(), "140.0000");
}

#[tokio::test]
async fn paging_does_not_lose_a_row_between_batches() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let feed = PostgresChangeFeed::new(pool.clone());
    let categories = PostgresCategoryRepository::new(pool.clone());
    for index in 0..7 {
        let category = a_custom_category(user_id, &format!("Propia {index}"));
        categories.insert(&category).await.expect("should insert");
        a_movement(&pool, user_id, category.id().as_uuid()).await;
    }

    // De uno en uno, que es el caso que mas veces cruza la frontera del lote.
    let batch = drain(&feed, user_id, 1).await;

    assert_eq!(batch.categories.len(), 12 + 7);
    assert_eq!(batch.movements.len(), 7);
    assert_eq!(count_of(&pool, user_id).await, batch.cursor);
}

/// El ultimo numero repartido a la cuenta: el cursor tiene que llegar hasta ahi.
async fn count_of(pool: &PgPool, user_id: UserId) -> ChangeNumber {
    sqlx::query_scalar::<_, ChangeNumber>(
        "SELECT last_seq FROM user_change_counters WHERE user_id = $1",
    )
    .bind(user_id.as_uuid())
    .fetch_one(pool)
    .await
    .expect("should read the counter")
}
