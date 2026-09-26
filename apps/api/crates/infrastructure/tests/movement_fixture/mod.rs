//! Cuenta de verdad en PostgreSQL para los tests de movimientos.
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

use chrono::{NaiveDate, Utc};
use gastonomo_application::port::{UserProvisioning as _, UserRepository as _};
use gastonomo_domain::movement::{CategoryId, MovementCategory};
use gastonomo_domain::{CategoryKind, Email, NewUser, PasswordHash, PersonName, User, UserId};
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresUserProvisioning, PostgresUserRepository, connect_pool,
    run_migrations,
};
use sqlx::Row as _;
use uuid::Uuid;

/// Variable que habilita esta bateria de tests.
const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

/// Lee una fecha de registro de un literal de test.
#[must_use]
pub fn date(raw: &str) -> NaiveDate {
    NaiveDate::parse_from_str(raw, "%Y-%m-%d").expect("fixture date")
}

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

/// Cuenta ya creada, con sus 12 categorias predeterminadas sembradas.
pub struct Fixture {
    pub pool: PgPool,
    pub user: UserId,
}

impl Fixture {
    /// Crea la cuenta, o devuelve `None` si no hay base de datos.
    pub async fn new() -> Option<Self> {
        // Los tests en paralelo hacen cola en el advisory lock del migrador, asi que
        // el timeout por defecto de tres segundos no llega.
        let config = DatabaseConfig {
            url: database_url()?,
            max_connections: 2,
            acquire_timeout_ms: 30_000,
        };
        let pool = connect_pool(&config).expect("pool should build");
        run_migrations(&pool)
            .await
            .expect("migrations should apply");

        let user = a_user();
        PostgresUserRepository::new(pool.clone())
            .insert(&user)
            .await
            .expect("should insert the user");
        PostgresUserProvisioning::new(pool.clone())
            .provision_new_user(user.id(), Utc::now())
            .await
            .expect("should seed the categories");
        Some(Self {
            pool,
            user: user.id(),
        })
    }

    /// Lee una de las categorias predeterminadas de la cuenta, por nombre.
    pub async fn category(&self, name: &str) -> MovementCategory {
        let row = sqlx::query(
            "SELECT id, kind, icon, color FROM categories WHERE user_id = $1 AND name = $2",
        )
        .bind(self.user.as_uuid())
        .bind(name)
        .fetch_one(&self.pool)
        .await
        .expect("the default category should exist");
        let kind: String = row.try_get("kind").expect("kind column");
        let icon: String = row.try_get("icon").expect("icon column");
        let color: String = row.try_get("color").expect("color column");
        MovementCategory::new(
            CategoryId::from_uuid(row.try_get::<Uuid, _>("id").expect("id column")),
            name,
            CategoryKind::parse(&kind).expect("a known kind"),
            &icon,
            &color,
        )
        .expect("a seeded category is valid")
    }
}

fn a_user() -> User {
    let unique = UserId::generate();
    User::register(NewUser {
        name: PersonName::parse("Camilo Rojas").expect("valid"),
        email: Email::parse(&format!("movimientos-{unique}@correo.com")).expect("valid"),
        password_hash: PasswordHash::parse("$argon2id$v=19$fixture").expect("valid"),
        created_at: Utc::now(),
    })
}
