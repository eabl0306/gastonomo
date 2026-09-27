//! Cuenta de verdad en PostgreSQL para los tests de categorias y ajustes.
//!
//! Cada test crea su propia cuenta con un correo unico: la base no se vacia entre
//! tests y dos cuentas no pueden compartir correo.

#![allow(
    dead_code,
    reason = "each integration test binary compiles this module and uses only the part it needs"
)]
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use chrono::Utc;
use gastonomo_application::port::UserProvisioning as _;
use gastonomo_application::port::UserRepository as _;
use gastonomo_domain::category::{
    Category, CategoryColor, CategoryIcon, CategoryKind, CategoryName, NewCategory,
};
use gastonomo_domain::{Email, NewUser, PasswordHash, PersonName, User, UserId};
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresUserProvisioning, PostgresUserRepository, connect_pool,
    run_migrations,
};
use uuid::Uuid;

/// Variable que habilita estas baterias de tests.
pub const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
pub const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

pub fn database_url() -> Option<String> {
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

pub async fn pool() -> Option<PgPool> {
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
pub async fn an_account(pool: &PgPool) -> UserId {
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

pub fn a_custom_category(user_id: UserId, name: &str) -> Category {
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
pub async fn a_movement(pool: &PgPool, user_id: UserId, category_id: Uuid) -> Uuid {
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

pub async fn category_of(pool: &PgPool, movement_id: Uuid) -> Uuid {
    sqlx::query_scalar::<_, Uuid>("SELECT category_id FROM movements WHERE id = $1")
        .bind(movement_id)
        .fetch_one(pool)
        .await
        .expect("should read the movement")
}
