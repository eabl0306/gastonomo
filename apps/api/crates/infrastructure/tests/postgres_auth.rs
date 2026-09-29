//! Tests de integracion de los adaptadores de autenticacion contra PostgreSQL.
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

use chrono::{Duration, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::{
    Clock as _, OpaqueTokenFactory as _, RefreshTokenRepository, UserProvisioning, UserRepository,
};
use gastonomo_domain::{
    Email, NewRefreshToken, NewUser, PasswordHash, PersonName, RefreshToken, User, UserId,
};
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresRefreshTokenRepository, PostgresUserProvisioning,
    PostgresUserRepository, RandomOpaqueTokenFactory, SystemClock, connect_pool, run_migrations,
};

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
    // Cada test monta su propio pool y aplica las migraciones. El migrador toma un
    // advisory lock, asi que los tests en paralelo hacen cola: con el timeout de
    // tres segundos del valor por defecto, el ultimo de la fila se queda sin
    // conexion.
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

/// Cuenta con un correo distinto en cada ejecucion: la base no se vacia entre
/// tests y dos cuentas no pueden compartir correo.
fn a_user() -> User {
    let unique = UserId::generate();
    User::register(NewUser {
        name: PersonName::parse("Camilo Rojas").expect("valid"),
        email: Email::parse(&format!("camilo-{unique}@correo.com")).expect("valid"),
        password_hash: PasswordHash::parse("$argon2id$v=19$fixture").expect("valid"),
        created_at: Utc::now(),
    })
}

fn a_refresh_token(user_id: UserId) -> RefreshToken {
    let factory = RandomOpaqueTokenFactory::new();
    let now = SystemClock::new().now();
    RefreshToken::issue(NewRefreshToken {
        user_id,
        digest: factory.create().digest,
        created_at: now,
        expires_at: now + Duration::days(30),
        user_agent: Some("integration-test".to_owned()),
    })
}

#[tokio::test]
async fn a_user_round_trips_and_the_email_ignores_case() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool);
    let user = a_user();
    users.insert(&user).await.expect("should insert");

    let upper = Email::parse(&user.email().as_str().to_uppercase()).expect("valid");
    let found = users
        .find_by_email(&upper)
        .await
        .expect("should query")
        .expect("citext should ignore case");
    assert_eq!(found.id(), user.id());
    assert_eq!(found.name().as_str(), "Camilo Rojas");
    assert_eq!(found.password_hash().as_str(), "$argon2id$v=19$fixture");

    let by_id = users
        .find_by_id(user.id())
        .await
        .expect("should query")
        .expect("the account exists");
    assert_eq!(by_id.email(), user.email());
}

#[tokio::test]
async fn a_repeated_email_is_a_conflict_and_not_a_crash() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool);
    let user = a_user();
    users.insert(&user).await.expect("should insert");

    let clash = User::register(NewUser {
        name: PersonName::parse("Otro").expect("valid"),
        email: user.email().clone(),
        password_hash: PasswordHash::parse("$argon2id$v=19$other").expect("valid"),
        created_at: Utc::now(),
    });
    let error = users.insert(&clash).await.expect_err("should clash");
    assert!(matches!(error, AppError::Conflict { ref code, .. } if code == "email_taken"));
}

#[tokio::test]
async fn changing_the_password_of_a_missing_account_is_not_found() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool);
    let hash = PasswordHash::parse("$argon2id$v=19$new").expect("valid");
    let error = users
        .update_password(UserId::generate(), &hash, Utc::now())
        .await
        .expect_err("there is no such account");
    assert!(matches!(error, AppError::NotFound { .. }));
}

#[tokio::test]
async fn registering_seeds_twelve_categories_and_the_settings_row() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool.clone());
    let provisioning = PostgresUserProvisioning::new(pool.clone());
    let user = a_user();
    users.insert(&user).await.expect("should insert");

    provisioning
        .provision_new_user(user.id(), Utc::now())
        .await
        .expect("should seed");
    provisioning
        .provision_new_user(user.id(), Utc::now())
        .await
        .expect("seeding twice should change nothing");

    let categories: i64 =
        sqlx::query_scalar("SELECT count(*) FROM categories WHERE user_id = $1 AND is_default")
            .bind(user.id().as_uuid())
            .fetch_one(&pool)
            .await
            .expect("count should succeed");
    assert_eq!(categories, 12);

    let currency: String =
        sqlx::query_scalar("SELECT display_currency FROM user_settings WHERE user_id = $1")
            .bind(user.id().as_uuid())
            .fetch_one(&pool)
            .await
            .expect("the settings row should exist");
    assert_eq!(currency.trim(), "COP");
}

#[tokio::test]
async fn rotating_a_refresh_token_revokes_the_old_one_and_links_it() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool.clone());
    let tokens = PostgresRefreshTokenRepository::new(pool);
    let user = a_user();
    users.insert(&user).await.expect("should insert");

    let first = a_refresh_token(user.id());
    tokens.insert(&first).await.expect("should insert");
    let second = a_refresh_token(user.id());
    tokens
        .rotate(first.id(), &second, Utc::now())
        .await
        .expect("should rotate");

    let stored = tokens
        .find_by_digest(first.digest())
        .await
        .expect("should query")
        .expect("the old token is still there");
    assert!(stored.is_revoked());
    assert_eq!(stored.replaced_by(), Some(second.id()));
    assert_eq!(stored.user_agent(), Some("integration-test"));

    let fresh = tokens
        .find_by_digest(second.digest())
        .await
        .expect("should query")
        .expect("the new token exists");
    assert!(fresh.is_usable(Utc::now()));
}

#[tokio::test]
async fn revoking_every_session_can_spare_the_current_one() {
    let Some(pool) = pool().await else { return };
    let users = PostgresUserRepository::new(pool.clone());
    let tokens = PostgresRefreshTokenRepository::new(pool);
    let user = a_user();
    users.insert(&user).await.expect("should insert");

    let kept = a_refresh_token(user.id());
    let dropped = a_refresh_token(user.id());
    tokens.insert(&kept).await.expect("should insert");
    tokens.insert(&dropped).await.expect("should insert");

    tokens
        .revoke_all_for_user(user.id(), Some(kept.id()), Utc::now())
        .await
        .expect("should revoke");

    let kept_after = tokens
        .find_by_digest(kept.digest())
        .await
        .expect("should query")
        .expect("still there");
    let dropped_after = tokens
        .find_by_digest(dropped.digest())
        .await
        .expect("should query")
        .expect("still there");
    assert!(!kept_after.is_revoked());
    assert!(dropped_after.is_revoked());
}
