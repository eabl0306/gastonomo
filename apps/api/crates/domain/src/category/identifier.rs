//! Identificador de una categoria.

use std::fmt;

use thiserror::Error;
use uuid::Uuid;

/// El texto recibido no es un UUID.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a valid category identifier")]
pub struct CategoryIdError {
    /// Valor recibido.
    pub got: String,
}

/// Identificador de una categoria.
///
/// El diseno referencia la categoria por nombre, pero el analisis obliga a usar
/// un identificador: renombrar no debe tocar los movimientos y borrar tiene que
/// poder reasignarlos con un `UPDATE` acotado.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct CategoryId(Uuid);

impl CategoryId {
    /// Genera un identificador nuevo.
    #[must_use]
    pub fn generate() -> Self {
        Self(Uuid::new_v4())
    }

    /// Envuelve un UUID ya existente, por ejemplo el de una fila leida.
    #[must_use]
    pub const fn from_uuid(value: Uuid) -> Self {
        Self(value)
    }

    /// Lee un identificador de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryIdError`] si el texto no es un UUID.
    pub fn parse(raw: &str) -> Result<Self, CategoryIdError> {
        Uuid::parse_str(raw.trim())
            .map(Self)
            .map_err(|_| CategoryIdError {
                got: raw.trim().to_owned(),
            })
    }

    /// UUID subyacente, que es lo que espera el adaptador de persistencia.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

impl fmt::Display for CategoryId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}
