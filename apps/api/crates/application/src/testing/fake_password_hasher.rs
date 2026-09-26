//! Hash de contrasenas de juguete, sin coste de CPU.

use async_trait::async_trait;
use gastonomo_domain::{Password, PasswordHash};

use crate::error::AppError;
use crate::port::PasswordHasher;

/// Prefijo del hash falso, para que nadie lo confunda con uno de verdad.
const PREFIX: &str = "fake$";

/// Doble del puerto [`PasswordHasher`]: concatena un prefijo.
///
/// Argon2id tarda decenas de milisegundos a proposito, y en una bateria de tests
/// eso se nota. Aqui basta con que dos contrasenas distintas den hashes distintos.
#[derive(Debug, Default)]
pub struct FakePasswordHasher;

impl FakePasswordHasher {
    /// Crea el doble.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

impl FakePasswordHasher {
    fn digest(password: &Password) -> Result<PasswordHash, AppError> {
        let plain = String::from_utf8_lossy(password.expose()).into_owned();
        PasswordHash::parse(&format!("{PREFIX}{plain}"))
            .map_err(|error| AppError::dependency(error.to_string()))
    }
}

#[async_trait]
impl PasswordHasher for FakePasswordHasher {
    async fn hash(&self, password: &Password) -> Result<PasswordHash, AppError> {
        Self::digest(password)
    }

    async fn verify(&self, password: &Password, hash: &PasswordHash) -> Result<bool, AppError> {
        Ok(Self::digest(password)?.as_str() == hash.as_str())
    }
}
