//! Direccion de un prestamo: lo que preste o lo que me prestaron.

use std::fmt;

use thiserror::Error;

use crate::category::CategoryKind;

/// El texto recibido no es una direccion de prestamo.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a loan direction")]
pub struct LoanDirectionError {
    /// Valor recibido.
    pub got: String,
}

/// Direccion del prestamo, tal como la eligen los dos botones del diseno.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum LoanDirection {
    /// Preste: el dinero salio y me lo deben.
    Lent,
    /// Me prestaron: el dinero entro y lo debo.
    Borrowed,
}

impl LoanDirection {
    /// Valor tal y como se guarda en la columna `direction`.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Lent => "lent",
            Self::Borrowed => "borrowed",
        }
    }

    /// Lee la direccion de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`LoanDirectionError`] si el texto no es `lent` ni `borrowed`.
    pub fn parse(raw: &str) -> Result<Self, LoanDirectionError> {
        match raw.trim() {
            "lent" => Ok(Self::Lent),
            "borrowed" => Ok(Self::Borrowed),
            other => Err(LoanDirectionError {
                got: other.to_owned(),
            }),
        }
    }

    /// Tipo del movimiento que genera el propio prestamo.
    ///
    /// Lo dice la caja de info de Nuevo prestamo: si preste "se registrara como
    /// gasto en tus movimientos", y si me prestaron, como ingreso.
    #[must_use]
    pub const fn loan_movement_kind(self) -> CategoryKind {
        match self {
            Self::Lent => CategoryKind::Out,
            Self::Borrowed => CategoryKind::In,
        }
    }

    /// Tipo del movimiento que genera un abono, que es el contrario del anterior.
    ///
    /// La misma caja de info: "cada abono que recibas contara como ingreso" si
    /// preste, y "cada abono que pagues contara como gasto" si me prestaron.
    #[must_use]
    pub const fn payment_movement_kind(self) -> CategoryKind {
        match self {
            Self::Lent => CategoryKind::In,
            Self::Borrowed => CategoryKind::Out,
        }
    }
}

impl fmt::Display for LoanDirection {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}
