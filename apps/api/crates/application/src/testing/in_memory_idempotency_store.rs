//! Claves de idempotencia en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::UserId;

use crate::error::AppError;
use crate::port::{IdempotencyStore, IdempotentRequest, IdempotentResponse, Reservation};

/// Una reserva guardada.
#[derive(Debug, Clone)]
struct Entry {
    user_id: UserId,
    endpoint: String,
    key: String,
    fingerprint: String,
    response: Option<IdempotentResponse>,
}

impl Entry {
    fn is(&self, request: IdempotentRequest<'_>) -> bool {
        self.user_id == request.user_id
            && self.endpoint == request.endpoint
            && self.key == request.key
    }
}

/// Doble del puerto [`IdempotencyStore`] sobre un vector.
#[derive(Debug, Default)]
pub struct InMemoryIdempotencyStore {
    rows: Mutex<Vec<Entry>>,
}

impl InMemoryIdempotencyStore {
    /// Crea el almacen vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            rows: Mutex::new(Vec::new()),
        }
    }

    /// Reservas guardadas.
    #[must_use]
    pub fn count(&self) -> usize {
        self.with(|rows| rows.len())
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<Entry>) -> T) -> T {
        match self.rows.lock() {
            Ok(mut rows) => operation(&mut rows),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl IdempotencyStore for InMemoryIdempotencyStore {
    async fn reserve(&self, request: IdempotentRequest<'_>) -> Result<Reservation, AppError> {
        Ok(self.with(|rows| {
            let Some(found) = rows.iter().find(|entry| entry.is(request)) else {
                rows.push(Entry {
                    user_id: request.user_id,
                    endpoint: request.endpoint.to_owned(),
                    key: request.key.to_owned(),
                    fingerprint: request.fingerprint.to_owned(),
                    response: None,
                });
                return Reservation::Fresh;
            };
            if found.fingerprint != request.fingerprint {
                return Reservation::Mismatch;
            }
            found
                .response
                .clone()
                .map_or(Reservation::InProgress, Reservation::Replay)
        }))
    }

    async fn complete(
        &self,
        request: IdempotentRequest<'_>,
        response: &IdempotentResponse,
    ) -> Result<(), AppError> {
        self.with(|rows| {
            if let Some(entry) = rows.iter_mut().find(|entry| entry.is(request)) {
                entry.response = Some(response.clone());
            }
        });
        Ok(())
    }

    async fn release(&self, request: IdempotentRequest<'_>) -> Result<(), AppError> {
        self.with(|rows| rows.retain(|entry| !(entry.is(request) && entry.response.is_none())));
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_domain::UserId;

    use super::InMemoryIdempotencyStore;
    use crate::port::{IdempotencyStore as _, IdempotentRequest, IdempotentResponse, Reservation};
    use crate::testing::block_on;

    const ENDPOINT: &str = "POST /v1/movements";

    fn request<'a>(user_id: UserId, key: &'a str, fingerprint: &'a str) -> IdempotentRequest<'a> {
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
            body: b"{}".to_vec(),
        }
    }

    #[test]
    fn a_new_key_is_fresh_and_then_in_progress_until_it_completes() {
        let store = InMemoryIdempotencyStore::new();
        let user = UserId::generate();

        let first = block_on(store.reserve(request(user, "k", "huella"))).unwrap();
        let second = block_on(store.reserve(request(user, "k", "huella"))).unwrap();
        block_on(store.complete(request(user, "k", "huella"), &answer())).unwrap();
        let third = block_on(store.reserve(request(user, "k", "huella"))).unwrap();

        assert_eq!(first, Reservation::Fresh);
        assert_eq!(second, Reservation::InProgress);
        assert_eq!(third, Reservation::Replay(answer()));
        assert_eq!(store.count(), 1);
    }

    #[test]
    fn another_fingerprint_on_the_same_key_is_a_mismatch() {
        let store = InMemoryIdempotencyStore::new();
        let user = UserId::generate();
        block_on(store.reserve(request(user, "k", "huella-1"))).unwrap();

        let reused = block_on(store.reserve(request(user, "k", "huella-2"))).unwrap();

        assert_eq!(reused, Reservation::Mismatch);
    }

    #[test]
    fn releasing_frees_a_reservation_but_never_a_stored_answer() {
        let store = InMemoryIdempotencyStore::new();
        let user = UserId::generate();
        block_on(store.reserve(request(user, "libre", "huella"))).unwrap();
        block_on(store.release(request(user, "libre", "huella"))).unwrap();
        block_on(store.reserve(request(user, "hecha", "huella"))).unwrap();
        block_on(store.complete(request(user, "hecha", "huella"), &answer())).unwrap();
        block_on(store.release(request(user, "hecha", "huella"))).unwrap();

        assert_eq!(
            block_on(store.reserve(request(user, "libre", "huella"))).unwrap(),
            Reservation::Fresh
        );
        assert_eq!(
            block_on(store.reserve(request(user, "hecha", "huella"))).unwrap(),
            Reservation::Replay(answer())
        );
    }

    #[test]
    fn the_key_of_one_account_does_not_reach_another() {
        let store = InMemoryIdempotencyStore::new();
        let first = UserId::generate();
        let second = UserId::generate();
        block_on(store.reserve(request(first, "k", "huella"))).unwrap();
        block_on(store.complete(request(first, "k", "huella"), &answer())).unwrap();

        let other = block_on(store.reserve(request(second, "k", "huella"))).unwrap();

        assert_eq!(other, Reservation::Fresh);
        assert_eq!(store.count(), 2);
    }
}
