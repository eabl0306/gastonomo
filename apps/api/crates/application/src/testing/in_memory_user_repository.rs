//! Repositorio de cuentas en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{Email, PasswordHash, User, UserId};

use crate::error::AppError;
use crate::port::UserRepository;

/// Doble del puerto [`UserRepository`] sobre un vector.
#[derive(Debug, Default)]
pub struct InMemoryUserRepository {
    users: Mutex<Vec<User>>,
}

impl InMemoryUserRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            users: Mutex::new(Vec::new()),
        }
    }

    /// Numero de cuentas guardadas.
    #[must_use]
    pub fn count(&self) -> usize {
        self.with(|users| users.len())
    }

    /// Cuenta guardada con ese correo.
    #[must_use]
    pub fn by_email(&self, email: &str) -> Option<User> {
        self.with(|users| {
            users
                .iter()
                .find(|user| user.email().as_str() == email)
                .cloned()
        })
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<User>) -> T) -> T {
        match self.users.lock() {
            Ok(mut users) => operation(&mut users),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl UserRepository for InMemoryUserRepository {
    async fn insert(&self, user: &User) -> Result<(), AppError> {
        self.with(|users| {
            if users.iter().any(|stored| stored.email() == user.email()) {
                return Err(AppError::conflict(
                    "email_taken",
                    "Ya existe una cuenta con ese correo.",
                ));
            }
            users.push(user.clone());
            Ok(())
        })
    }

    async fn find_by_email(&self, email: &Email) -> Result<Option<User>, AppError> {
        Ok(self.with(|users| users.iter().find(|user| user.email() == email).cloned()))
    }

    async fn find_by_id(&self, user_id: UserId) -> Result<Option<User>, AppError> {
        Ok(self.with(|users| users.iter().find(|user| user.id() == user_id).cloned()))
    }

    async fn update_password(
        &self,
        user_id: UserId,
        password_hash: &PasswordHash,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        self.with(|users| {
            let Some(user) = users.iter_mut().find(|user| user.id() == user_id) else {
                return Err(AppError::not_found("user"));
            };
            user.change_password(password_hash.clone(), now);
            Ok(())
        })
    }
}
