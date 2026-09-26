//! Repositorio de tokens de recuperacion en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{PasswordResetToken, PasswordResetTokenId, TokenDigest};

use crate::error::AppError;
use crate::port::PasswordResetTokenRepository;

/// Doble del puerto [`PasswordResetTokenRepository`] sobre un vector.
#[derive(Debug, Default)]
pub struct InMemoryPasswordResetTokenRepository {
    tokens: Mutex<Vec<PasswordResetToken>>,
}

impl InMemoryPasswordResetTokenRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            tokens: Mutex::new(Vec::new()),
        }
    }

    /// Tokens guardados, en orden de insercion.
    #[must_use]
    pub fn all(&self) -> Vec<PasswordResetToken> {
        self.with(|tokens| tokens.clone())
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<PasswordResetToken>) -> T) -> T {
        match self.tokens.lock() {
            Ok(mut tokens) => operation(&mut tokens),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl PasswordResetTokenRepository for InMemoryPasswordResetTokenRepository {
    async fn insert(&self, token: &PasswordResetToken) -> Result<(), AppError> {
        self.with(|tokens| tokens.push(token.clone()));
        Ok(())
    }

    async fn find_by_digest(
        &self,
        digest: &TokenDigest,
    ) -> Result<Option<PasswordResetToken>, AppError> {
        Ok(self.with(|tokens| {
            tokens
                .iter()
                .find(|token| token.digest() == digest)
                .cloned()
        }))
    }

    async fn mark_used(
        &self,
        token: PasswordResetTokenId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        self.with(|tokens| {
            if let Some(stored) = tokens.iter_mut().find(|stored| stored.id() == token) {
                stored.mark_used(now);
            }
        });
        Ok(())
    }
}
