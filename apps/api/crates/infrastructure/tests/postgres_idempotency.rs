//! Tests del almacen de claves de idempotencia contra un PostgreSQL de verdad.
//!
//! Lo que hay que probar aqui, y no con un doble en memoria, es la atomicidad: dos
//! reintentos simultaneos no pueden recibir los dos "la clave era nueva". Eso
//! depende del `INSERT ... ON CONFLICT DO NOTHING ... RETURNING` y de la clave
//! primaria, no del codigo de Rust.
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

mod movement_fixture;

use gastonomo_application::port::{
    IdempotencyStore as _, IdempotentRequest, IdempotentResponse, Reservation,
};
use gastonomo_domain::UserId;
use gastonomo_infrastructure::PostgresIdempotencyStore;
use movement_fixture::Fixture;

const ENDPOINT: &str = "POST /v1/movements";

const fn request<'a>(user_id: UserId, key: &'a str, fingerprint: &'a str) -> IdempotentRequest<'a> {
    IdempotentRequest {
        user_id,
        endpoint: ENDPOINT,
        key,
        fingerprint,
    }
}

fn answer() -> IdempotentResponse {
    IdempotentResponse {
        status: 201,
        body: br#"{"id":"stored"}"#.to_vec(),
    }
}

#[tokio::test]
async fn the_first_reserve_wins_and_the_second_sees_the_stored_answer() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(fixture.pool.clone());
    let key = format!("intento-{}", UserId::generate());
    let first = store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should reserve");
    assert_eq!(first, Reservation::Fresh);

    let in_flight = store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should read");
    assert_eq!(
        in_flight,
        Reservation::InProgress,
        "sin respuesta guardada todavia, el segundo intento espera"
    );

    store
        .complete(request(fixture.user, &key, "huella"), &answer())
        .await
        .expect("should record");
    let replay = store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should read");
    assert_eq!(replay, Reservation::Replay(answer()));
}

#[tokio::test]
async fn two_reserves_at_the_same_time_only_give_one_fresh() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(fixture.pool.clone());
    let key = format!("carrera-{}", UserId::generate());

    let (left, right) = tokio::join!(
        store.reserve(request(fixture.user, &key, "huella")),
        store.reserve(request(fixture.user, &key, "huella")),
    );

    let left = left.expect("should reserve");
    let right = right.expect("should reserve");
    let fresh = usize::from(left == Reservation::Fresh) + usize::from(right == Reservation::Fresh);
    assert_eq!(
        fresh, 1,
        "solo un intento puede ejecutar la operacion: {left:?} y {right:?}"
    );
}

#[tokio::test]
async fn the_same_key_with_another_fingerprint_is_a_mismatch() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(fixture.pool.clone());
    let key = format!("reutilizada-{}", UserId::generate());
    store
        .reserve(request(fixture.user, &key, "huella-1"))
        .await
        .expect("should reserve");

    let reused = store
        .reserve(request(fixture.user, &key, "huella-2"))
        .await
        .expect("should read");

    assert_eq!(reused, Reservation::Mismatch);
}

#[tokio::test]
async fn a_released_key_can_be_reserved_again() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(fixture.pool.clone());
    let key = format!("suelta-{}", UserId::generate());
    store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should reserve");
    store
        .release(request(fixture.user, &key, "huella"))
        .await
        .expect("should release");

    let again = store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should reserve");

    assert_eq!(
        again,
        Reservation::Fresh,
        "un cuerpo rechazado no quema la clave"
    );
}

#[tokio::test]
async fn a_completed_key_is_not_released() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(fixture.pool.clone());
    let key = format!("completa-{}", UserId::generate());
    store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should reserve");
    store
        .complete(request(fixture.user, &key, "huella"), &answer())
        .await
        .expect("should record");
    store
        .release(request(fixture.user, &key, "huella"))
        .await
        .expect("release is a no-op here");

    let replay = store
        .reserve(request(fixture.user, &key, "huella"))
        .await
        .expect("should read");

    assert_eq!(
        replay,
        Reservation::Replay(answer()),
        "una respuesta ya guardada no se puede soltar"
    );
}

#[tokio::test]
async fn the_key_of_one_account_does_not_reach_another() {
    let Some(first) = Fixture::new().await else {
        return;
    };
    let Some(second) = Fixture::new().await else {
        return;
    };
    let store = PostgresIdempotencyStore::new(first.pool.clone());
    let key = "misma-clave-dos-cuentas";
    store
        .reserve(request(first.user, key, "huella"))
        .await
        .expect("should reserve");
    store
        .complete(request(first.user, key, "huella"), &answer())
        .await
        .expect("should record");

    let other = store
        .reserve(request(second.user, key, "huella"))
        .await
        .expect("should reserve");

    assert_eq!(
        other,
        Reservation::Fresh,
        "la clave de una cuenta no alcanza a otra"
    );
}
