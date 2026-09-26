//! Identificador de un movimiento.

use std::fmt;

use thiserror::Error;
use uuid::Uuid;

/// El texto recibido no es un UUID.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a valid movement identifier")]
pub struct MovementIdError {
    /// Valor recibido.
    pub got: String,
}

/// Identificador de un movimiento.
///
/// Se genera en el dominio, igual que [`crate::UserId`]: asi el caso de uso puede
/// construir el agregado completo antes de tocar persistencia y el cursor de
/// paginacion puede apuntar a un movimiento que todavia no esta guardado.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct MovementId(Uuid);

impl MovementId {
    /// Genera un identificador nuevo.
    #[must_use]
    pub fn generate() -> Self {
        Self(Uuid::new_v4())
    }

    /// Envuelve un UUID ya existente, por ejemplo el de una fila.
    #[must_use]
    pub const fn from_uuid(value: Uuid) -> Self {
        Self(value)
    }

    /// Lee un identificador de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementIdError`] si el texto no es un UUID.
    pub fn parse(raw: &str) -> Result<Self, MovementIdError> {
        Uuid::parse_str(raw.trim())
            .map(Self)
            .map_err(|_| MovementIdError {
                got: raw.trim().to_owned(),
            })
    }

    /// UUID subyacente, que es lo que espera el adaptador de persistencia.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

impl fmt::Display for MovementId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}
