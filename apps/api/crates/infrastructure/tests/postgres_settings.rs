//! Tests de integracion de los adaptadores de ajustes y del catalogo de monedas
//! contra PostgreSQL.
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
use gastonomo_application::port::settings::{
    DisplayCurrencyCatalog as _, UserSettingsRepository as _,
};
use gastonomo_domain::settings::{Language, UserSettings};
use gastonomo_domain::value_object::CurrencyCode;
use gastonomo_infrastructure::persistence::{
    PostgresDisplayCurrencyCatalog, PostgresUserSettingsRepository,
};

use account_fixture::{an_account, pool};

#[tokio::test]
async fn the_settings_of_a_new_account_are_cop_and_spanish() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let settings = PostgresUserSettingsRepository::new(pool.clone());

    let found = settings
        .find(user_id)
        .await
        .expect("should query")
        .expect("the registration seeds the row");

    assert_eq!(found.display_currency().as_str(), "COP");
    assert_eq!(found.language(), Language::Spanish);
}

#[tokio::test]
async fn saving_the_settings_twice_updates_the_same_row() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let repository = PostgresUserSettingsRepository::new(pool.clone());
    let mut settings = UserSettings::defaults_for(user_id, Utc::now());

    settings.set_display_currency(CurrencyCode::parse("EUR").expect("valid"), Utc::now());
    settings.set_language(Language::English, Utc::now());
    repository.save(&settings).await.expect("should save");
    repository.save(&settings).await.expect("should upsert");

    let found = repository
        .find(user_id)
        .await
        .expect("should query")
        .expect("the row exists");
    assert_eq!(found.display_currency().as_str(), "EUR");
    assert_eq!(found.language(), Language::English);
    let rows =
        sqlx::query_scalar::<_, i64>("SELECT count(*) FROM user_settings WHERE user_id = $1")
            .bind(user_id.as_uuid())
            .fetch_one(&pool)
            .await
            .expect("should count");
    assert_eq!(rows, 1);
}

#[tokio::test]
async fn the_catalog_only_accepts_a_currency_that_exists_and_is_offered() {
    let Some(pool) = pool().await else { return };
    sqlx::query(
        "INSERT INTO currencies (code, name, symbol, offered) \
         VALUES ('COP', 'Peso colombiano', '$', TRUE), \
                ('XTS', 'Codigo reservado para pruebas', '', FALSE) \
         ON CONFLICT (code) DO UPDATE SET offered = excluded.offered",
    )
    .execute(&pool)
    .await
    .expect("should seed the catalog");
    let catalog = PostgresDisplayCurrencyCatalog::new(pool.clone());

    let offered = CurrencyCode::parse("COP").expect("valid");
    let retired = CurrencyCode::parse("XTS").expect("valid");
    let unknown = CurrencyCode::parse("QQQ").expect("valid");

    assert!(catalog.is_selectable(&offered).await.expect("should query"));
    assert!(!catalog.is_selectable(&retired).await.expect("should query"));
    assert!(!catalog.is_selectable(&unknown).await.expect("should query"));
}
