//! Tipo de un movimiento: ingreso o gasto.

use std::fmt;

use thiserror::Error;

use crate::category::CategoryKind;

/// El texto recibido no es un tipo de movimiento conocido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a movement kind")]
pub struct MovementKindError {
    /// Valor recibido.
    pub got: String,
}

/// Tipo de movimiento.
///
/// Son solo dos, los del segmented control de Nuevo movimiento. No se reutiliza
/// [`CategoryKind`] porque una categoria admite un tercer valor, `both`, que un
/// movimiento nunca puede tener: un gasto es un gasto.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum MovementKind {
    /// Ingreso.
    In,
    /// Gasto.
    Out,
}

impl MovementKind {
    /// Valor tal y como se guarda en la columna `kind`.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::In => "in",
            Self::Out => "out",
        }
    }

    /// Lee el tipo de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementKindError`] si el texto no es `in` ni `out`.
    pub fn parse(raw: &str) -> Result<Self, MovementKindError> {
        match raw.trim() {
            "in" => Ok(Self::In),
            "out" => Ok(Self::Out),
            other => Err(MovementKindError {
                got: other.to_owned(),
            }),
        }
    }

    /// Tipo de categoria equivalente, para preguntarle si acepta el movimiento.
    #[must_use]
    pub const fn as_category_kind(self) -> CategoryKind {
        match self {
            Self::In => CategoryKind::In,
            Self::Out => CategoryKind::Out,
        }
    }

    /// Signo con el que el diseno pinta el importe: `+` en ingresos.
    #[must_use]
    pub const fn is_income(self) -> bool {
        matches!(self, Self::In)
    }
}

impl fmt::Display for MovementKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}
