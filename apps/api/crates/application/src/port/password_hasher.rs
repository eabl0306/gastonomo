//! Puerto de hash de contrasenas.

use async_trait::async_trait;
use gastonomo_domain::{Password, PasswordHash};

use crate::error::AppError;

/// Calcula y comprueba hashes de contrasena.
///
/// Es asincrono aunque Argon2id no espere a ningun IO. El motivo es el contrario:
/// quema CPU durante decenas de milisegundos, y en un runtime asincrono eso bloquea
/// un hilo de trabajo entero y para todas las peticiones que ese hilo tenia a
/// medias. El adaptador real saca el calculo a un hilo de bloqueo, y para poder
/// hacerlo necesita poder esperar.
#[async_trait]
pub trait PasswordHasher: Send + Sync {
    /// Calcula el hash de una contrasena en claro.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si el algoritmo falla.
    async fn hash(&self, password: &Password) -> Result<PasswordHash, AppError>;

    /// Comprueba una contrasena contra un hash guardado.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si el hash guardado no se puede leer.
    async fn verify(&self, password: &Password, hash: &PasswordHash) -> Result<bool, AppError>;
}
