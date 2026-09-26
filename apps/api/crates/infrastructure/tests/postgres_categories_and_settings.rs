//! Tests de integracion de los adaptadores de categorias y ajustes contra
//! PostgreSQL.
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

use chrono::Utc;
use gastonomo_application::port::UserProvisioning as _;
use gastonomo_application::port::UserRepository as _;
use gastonomo_application::port::category::CategoryRepository as _;
use gastonomo_application::port::settings::{
    DisplayCurrencyCatalog as _, UserSettingsRepository as _,
};
use gastonomo_domain::category::{
    Category, CategoryColor, CategoryIcon, CategoryKind, CategoryName, DEFAULT_CATEGORIES,
    FALLBACK_CATEGORY_NAME, NewCategory,
};
use gastonomo_domain::settings::{Language, UserSettings};
use gastonomo_domain::value_object::CurrencyCode;
use gastonomo_domain::{Email, NewUser, PasswordHash, PersonName, User, UserId};
use gastonomo_infrastructure::persistence::{
    PostgresCategoryRepository, PostgresDisplayCurrencyCatalog, PostgresUserSettingsRepository,
};
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresUserProvisioning, PostgresUserRepository, connect_pool,
    run_migrations,
};
use uuid::Uuid;

/// Variable que habilita esta bateria de tests.
const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

fn database_url() -> Option<String> {
    let url = std::env::var(URL_VAR).ok().filter(|url| !url.is_empty());
    let mandatory = std::env::var(REQUIRE_VAR)
        .is_ok_and(|value| !matches!(value.trim(), "" | "0" | "false" | "FALSE"));
    assert!(
        url.is_some() || !mandatory,
        "{URL_VAR} is empty but {REQUIRE_VAR} is set: \
         these tests must not pass without a real postgres"
    );
    url
}

async fn pool() -> Option<PgPool> {
    let config = DatabaseConfig {
        url: database_url()?,
        max_connections: 2,
        acquire_timeout_ms: 30_000,
    };
    let pool = connect_pool(&config).expect("pool should build");
    run_migrations(&pool)
        .await
        .expect("migrations should apply");
    Some(pool)
}

/// Cuenta nueva con las 12 predeterminadas y los ajustes ya sembrados.
async fn an_account(pool: &PgPool) -> UserId {
    let unique = Uuid::new_v4();
    let user = User::register(NewUser {
        name: PersonName::parse("Camilo Rojas").expect("valid"),
        email: Email::parse(&format!("camilo-{unique}@correo.com")).expect("valid"),
        password_hash: PasswordHash::parse("$argon2id$v=19$fixture").expect("valid"),
        created_at: Utc::now(),
    });
    PostgresUserRepository::new(pool.clone())
        .insert(&user)
        .await
        .expect("should insert the account");
    PostgresUserProvisioning::new(pool.clone())
        .provision_new_user(user.id(), Utc::now())
        .await
        .expect("should seed categories and settings");
    user.id()
}

fn a_custom_category(user_id: UserId, name: &str) -> Category {
    Category::create_custom(NewCategory {
        user_id,
        name: CategoryName::parse(name).expect("valid name"),
        kind: CategoryKind::Out,
        icon: CategoryIcon::parse("pets").expect("offered icon"),
        color: CategoryColor::parse("#2f7fb5").expect("offered color"),
        created_at: Utc::now(),
    })
    .expect("the fixture respects the design rules")
}

/// Inserta un movimiento con SQL literal: el adaptador de movimientos es de otro
/// contexto y aqui solo hace falta una fila que apunte a la categoria.
async fn a_movement(pool: &PgPool, user_id: UserId, category_id: Uuid) -> Uuid {
    let id = Uuid::new_v4();
    sqlx::query(
        "INSERT INTO movements \
         (id, user_id, occurred_on, kind, category_id, title, amount, currency, base, \
          base_currency) \
         VALUES ($1, $2, DATE '2026-09-15', 'out', $3, 'Croquetas', 140.0000, 'COP', \
                 140.0000, 'COP')",
    )
    .bind(id)
    .bind(user_id.as_uuid())
    .bind(category_id)
    .execute(pool)
    .await
    .expect("should insert the movement");
    id
}

async fn category_of(pool: &PgPool, movement_id: Uuid) -> Uuid {
    sqlx::query_scalar::<_, Uuid>("SELECT category_id FROM movements WHERE id = $1")
        .bind(movement_id)
        .fetch_one(pool)
        .await
        .expect("should read the movement")
}

#[tokio::test]
async fn a_new_account_starts_with_the_twelve_defaults_and_no_usage() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());

    let listed = categories
        .list_with_usage(user_id)
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
async fn deleting_reassigns_the_movements_to_otros_and_removes_the_row() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let category = a_custom_category(user_id, "Mascotas");
    categories.insert(&category).await.expect("should insert");
    let movement = a_movement(&pool, user_id, category.id().as_uuid()).await;
    let otros = categories
        .find_by_name(user_id, FALLBACK_CATEGORY_NAME)
        .await
        .expect("should query")
        .expect("Otros is seeded");

    let reassigned = categories
        .delete_reassigning_movements(user_id, category.id(), otros.id())
        .await
        .expect("should delete");

    assert_eq!(reassigned, 1);
    assert_eq!(category_of(&pool, movement).await, otros.id().as_uuid());
    assert!(
        categories
            .find(user_id, category.id())
            .await
            .expect("should query")
            .is_none()
    );
}

#[tokio::test]
async fn a_default_category_cannot_be_updated_nor_deleted_in_the_database() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let categories = PostgresCategoryRepository::new(pool.clone());
    let salario = categories
        .find_by_name(user_id, "Salario")
        .await
        .expect("should query")
        .expect("Salario is seeded");
    let otros = categories
        .find_by_name(user_id, FALLBACK_CATEGORY_NAME)
        .await
        .expect("should query")
        .expect("Otros is seeded");

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
    );

    let update = categories.update(&renamed).await;
    assert!(update.is_err(), "the WHERE clause guards is_default");

    let delete = categories
        .delete_reassigning_movements(user_id, salario.id(), otros.id())
        .await;
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
    let otros = categories
        .find_by_name(intruder, FALLBACK_CATEGORY_NAME)
        .await
        .expect("should query")
        .expect("Otros is seeded");
    let stolen = categories
        .delete_reassigning_movements(intruder, category.id(), otros.id())
        .await;
    assert!(stolen.is_err());
    assert!(
        categories
            .find(owner, category.id())
            .await
            .expect("should query")
            .is_some()
    );
}

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
