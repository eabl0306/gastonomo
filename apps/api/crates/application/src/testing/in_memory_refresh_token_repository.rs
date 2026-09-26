//! Repositorio de refresh tokens en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{RefreshToken, RefreshTokenId, TokenDigest, UserId};

use crate::error::AppError;
use crate::port::RefreshTokenRepository;

/// Doble del puerto [`RefreshTokenRepository`] sobre un vector.
#[derive(Debug, Default)]
pub struct InMemoryRefreshTokenRepository {
    tokens: Mutex<Vec<RefreshToken>>,
}

impl InMemoryRefreshTokenRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            tokens: Mutex::new(Vec::new()),
        }
    }

    /// Tokens guardados, en orden de insercion.
    #[must_use]
    pub fn all(&self) -> Vec<RefreshToken> {
        self.with(|tokens| tokens.clone())
    }

    /// Numero de tokens vivos en el instante dado.
    #[must_use]
    pub fn usable_count(&self, now: DateTime<Utc>) -> usize {
        self.with(|tokens| tokens.iter().filter(|token| token.is_usable(now)).count())
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<RefreshToken>) -> T) -> T {
        match self.tokens.lock() {
            Ok(mut tokens) => operation(&mut tokens),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl RefreshTokenRepository for InMemoryRefreshTokenRepository {
    async fn insert(&self, token: &RefreshToken) -> Result<(), AppError> {
        self.with(|tokens| tokens.push(token.clone()));
        Ok(())
    }

    async fn find_by_digest(&self, digest: &TokenDigest) -> Result<Option<RefreshToken>, AppError> {
        Ok(self.with(|tokens| {
            tokens
                .iter()
                .find(|token| token.digest() == digest)
                .cloned()
        }))
    }

    async fn rotate(
        &self,
        current: RefreshTokenId,
        replacement: &RefreshToken,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        self.with(|tokens| {
            if let Some(token) = tokens.iter_mut().find(|token| token.id() == current) {
                token.mark_rotated(replacement.id(), now);
            }
            tokens.push(replacement.clone());
        });
        Ok(())
    }

    async fn revoke(&self, token: RefreshTokenId, now: DateTime<Utc>) -> Result<(), AppError> {
        self.with(|tokens| {
            if let Some(stored) = tokens.iter_mut().find(|stored| stored.id() == token) {
                stored.revoke(now);
            }
        });
        Ok(())
    }

    async fn revoke_all_for_user(
        &self,
        user_id: UserId,
        except: Option<RefreshTokenId>,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        self.with(|tokens| {
            for token in tokens
                .iter_mut()
                .filter(|token| token.user_id() == user_id && Some(token.id()) != except)
            {
                token.revoke(now);
            }
        });
        Ok(())
    }
}
