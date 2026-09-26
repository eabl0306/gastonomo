//! Identificador de usuario.

use std::fmt;

use thiserror::Error;
use uuid::Uuid;

/// El texto recibido no es un UUID.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a valid identifier")]
pub struct UserIdError {
    /// Valor recibido.
    pub got: String,
}

/// Identificador de una cuenta.
///
/// Se genera en el dominio y no en la base de datos: asi un caso de uso puede
/// construir el agregado completo antes de tocar persistencia, y los tests no
/// necesitan una base para tener identidades reales.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct UserId(Uuid);

impl UserId {
    /// Genera un identificador nuevo.
    #[must_use]
    pub fn generate() -> Self {
        Self(Uuid::new_v4())
    }

    /// Envuelve un UUID ya existente, por ejemplo el que devuelve una consulta.
    #[must_use]
    pub const fn from_uuid(value: Uuid) -> Self {
        Self(value)
    }

    /// Lee un identificador de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`UserIdError`] si el texto no es un UUID.
    pub fn parse(raw: &str) -> Result<Self, UserIdError> {
        Uuid::parse_str(raw.trim())
            .map(Self)
            .map_err(|_| UserIdError {
                got: raw.trim().to_owned(),
            })
    }

    /// UUID subyacente, que es lo que espera el adaptador de persistencia.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

impl fmt::Display for UserId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}
