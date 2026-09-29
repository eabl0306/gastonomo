//! Tests de integracion de los tokens de recuperacion contra PostgreSQL.
//!
//! Salieron de `postgres_auth.rs` cuando ese fichero llego al limite de 400
//! lineas. Son un tema propio: los otros hablan de cuentas y de sesiones.
//!
//! Mismas reglas: sin `GASTONOMO_TEST_DATABASE_URL` cada test se salta a si mismo,
//! y con `GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta de base de datos es un fallo.
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

use chrono::{DateTime, Duration, Utc};
use gastonomo_application::port::{OpaqueTokenFactory as _, PasswordResetTokenRepository};
use gastonomo_domain::{NewPasswordResetToken, PasswordResetToken, UserId};
use gastonomo_infrastructure::{PostgresPasswordResetTokenRepository, RandomOpaqueTokenFactory};

use account_fixture::{an_account, pool};

/// Un token de la cuenta dada, con las fechas que pida el test.
fn token_for(
    user: UserId,
    created_at: DateTime<Utc>,
    expires_at: DateTime<Utc>,
) -> PasswordResetToken {
    PasswordResetToken::issue(NewPasswordResetToken {
        user_id: user,
        digest: RandomOpaqueTokenFactory::new().create().digest,
        created_at,
        expires_at,
    })
}

#[tokio::test]
async fn a_reset_token_is_single_use() {
    let Some(pool) = pool().await else { return };
    let user = an_account(&pool).await;
    let resets = PostgresPasswordResetTokenRepository::new(pool);

    let now = Utc::now();
    let token = token_for(user, now, now + Duration::minutes(30));
    resets.insert(&token).await.expect("should insert");

    let found = resets
        .find_by_digest(token.digest())
        .await
        .expect("should query")
        .expect("the token exists");
    assert!(found.is_usable(now));

    resets
        .mark_used(token.id(), now)
        .await
        .expect("should mark used");
    let spent = resets
        .find_by_digest(token.digest())
        .await
        .expect("should query")
        .expect("the token exists");
    assert!(!spent.is_usable(now));
}

/// El `DELETE` de verdad, no el del doble en memoria.
///
/// Se prueba contra Postgres porque el doble podria pasar con un SQL mal escrito:
/// un `WHERE` de mas y el borrado no se lleva nada, uno de menos y se lleva los de
/// otra cuenta.
#[tokio::test]
async fn discarding_a_users_reset_tokens_leaves_the_other_accounts_alone() {
    let Some(pool) = pool().await else { return };
    let mine = an_account(&pool).await;
    let other = an_account(&pool).await;
    let resets = PostgresPasswordResetTokenRepository::new(pool);

    let now = Utc::now();
    let issue = |owner: UserId| token_for(owner, now, now + Duration::minutes(30));
    let first = issue(mine);
    let second = issue(mine);
    let theirs = issue(other);
    for token in [&first, &second, &theirs] {
        resets.insert(token).await.expect("should insert");
    }

    resets
        .discard_all_for_user(mine)
        .await
        .expect("should discard");

    for gone in [&first, &second] {
        assert!(
            resets
                .find_by_digest(gone.digest())
                .await
                .expect("should query")
                .is_none(),
            "every token of that account should be gone"
        );
    }
    assert!(
        resets
            .find_by_digest(theirs.digest())
            .await
            .expect("should query")
            .is_some(),
        "another account's token must survive"
    );
}

/// El SELECT del ultimo enlace vivo, contra la base de verdad.
///
/// Tiene tres filtros que se pueden escribir mal sin que el doble en memoria se
/// entere: el usado no cuenta, el caducado tampoco, y de los vivos hay que coger
/// el mas reciente, no el primero que salga.
#[tokio::test]
async fn the_latest_live_reset_token_ignores_the_spent_and_the_expired_ones() {
    let Some(pool) = pool().await else { return };
    let user = an_account(&pool).await;
    let resets = PostgresPasswordResetTokenRepository::new(pool);

    let now = Utc::now();
    let issue = |created, expires| token_for(user, created, expires);
    // El orden por `created_at` los descarta a todos menos al bueno SOLO si los tres
    // filtros estan. Los dos descartables son los MAS recientes a proposito: si el
    // caducado o el usado fueran antiguos, quitarle un filtro a la consulta pasaria
    // igual y el test no serviria de nada.
    let expired = issue(now - Duration::seconds(30), now - Duration::seconds(10));
    let spent = issue(now - Duration::seconds(60), now + Duration::minutes(29));
    let live = issue(now - Duration::seconds(90), now + Duration::minutes(28));
    let older = issue(now - Duration::minutes(5), now + Duration::minutes(25));
    for token in [&expired, &spent, &live, &older] {
        resets.insert(token).await.expect("should insert");
    }
    resets
        .mark_used(spent.id(), now)
        .await
        .expect("should mark used");

    let found = resets
        .latest_live_for_user(user, now)
        .await
        .expect("should query")
        .expect("one of them is still live");
    assert_eq!(
        found.id(),
        live.id(),
        "the newest LIVE one, not the newest row"
    );

    // Sin ninguno vivo, no hay espera que valga.
    resets
        .discard_all_for_user(user)
        .await
        .expect("should discard");
    assert!(
        resets
            .latest_live_for_user(user, now)
            .await
            .expect("should query")
            .is_none()
    );
}
