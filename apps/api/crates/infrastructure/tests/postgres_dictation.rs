//! Tests de integracion de la lista de aprobados del dictado contra PostgreSQL.
//!
//! Lo que se prueba aqui no es la consulta, que es un `EXISTS`: es que la fila y la
//! cuenta viven y mueren juntas. Una aprobacion que sobreviviera a la cuenta seria
//! una capacidad que se paga concedida a nadie.
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
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

mod account_fixture;

use gastonomo_application::port::dictation::DictationAccess as _;
use gastonomo_domain::value_object::UserId;
use gastonomo_infrastructure::PgPool;
use gastonomo_infrastructure::persistence::PostgresDictationAccess;

use account_fixture::{an_account, pool};

/// Aprueba una cuenta como lo hace el dueno de la instancia: una fila a mano.
///
/// El adaptador no escribe, asi que la unica forma de montar el caso "aprobado" es
/// SQL literal.
async fn approve(pool: &PgPool, user_id: UserId) {
    sqlx::query("INSERT INTO dictation_allowlist (user_id) VALUES ($1)")
        .bind(user_id.as_uuid())
        .execute(pool)
        .await
        .expect("should insert the approval");
}

async fn approvals_of(pool: &PgPool, user_id: UserId) -> i64 {
    sqlx::query_scalar::<_, i64>("SELECT count(*) FROM dictation_allowlist WHERE user_id = $1")
        .bind(user_id.as_uuid())
        .fetch_one(pool)
        .await
        .expect("should count the approvals")
}

#[tokio::test]
async fn an_account_without_a_row_does_not_have_the_dictation() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let access = PostgresDictationAccess::new(pool.clone());

    assert!(!access.is_allowed(user_id).await.expect("should query"));
}

#[tokio::test]
async fn an_account_with_a_row_does_have_it() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    let access = PostgresDictationAccess::new(pool.clone());
    approve(&pool, user_id).await;

    assert!(access.is_allowed(user_id).await.expect("should query"));
}

#[tokio::test]
async fn one_approval_does_not_reach_another_account() {
    let Some(pool) = pool().await else { return };
    let approved = an_account(&pool).await;
    let other = an_account(&pool).await;
    let access = PostgresDictationAccess::new(pool.clone());
    approve(&pool, approved).await;

    assert!(access.is_allowed(approved).await.expect("should query"));
    assert!(!access.is_allowed(other).await.expect("should query"));
}

#[tokio::test]
async fn deleting_the_account_takes_its_approval_with_it() {
    let Some(pool) = pool().await else { return };
    let user_id = an_account(&pool).await;
    approve(&pool, user_id).await;
    assert_eq!(approvals_of(&pool, user_id).await, 1);

    sqlx::query("DELETE FROM users WHERE id = $1")
        .bind(user_id.as_uuid())
        .execute(&pool)
        .await
        .expect("should delete the account");

    assert_eq!(approvals_of(&pool, user_id).await, 0);
}
