//! Tipo de una categoria.

use std::fmt;

use thiserror::Error;

/// El texto recibido no es un tipo de categoria conocido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a category kind")]
pub struct CategoryKindError {
    /// Valor recibido.
    pub got: String,
}

/// Tipo de categoria.
///
/// El prototipo ensena `Prestamo`, `Abono prestamo` y `Otros` en el grid de gasto
/// y en el de ingreso, y a la vez el nombre de una categoria es unico entre todas
/// las del usuario. Las dos cosas solo caben juntas si existe un tipo que sirva
/// para los dos lados: eso es [`CategoryKind::Both`], reservado a las
/// predeterminadas. Una categoria que crea el usuario es siempre `In` u `Out`,
/// que es lo unico que ofrece el sheet.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum CategoryKind {
    /// Ingreso.
    In,
    /// Gasto.
    Out,
    /// Sirve para ingresos y para gastos. Solo las predeterminadas.
    Both,
}

impl CategoryKind {
    /// Valor tal y como se guarda en la columna `kind`.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::In => "in",
            Self::Out => "out",
            Self::Both => "both",
        }
    }

    /// Lee el tipo de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryKindError`] si el texto no es `in`, `out` ni `both`.
    pub fn parse(raw: &str) -> Result<Self, CategoryKindError> {
        match raw.trim() {
            "in" => Ok(Self::In),
            "out" => Ok(Self::Out),
            "both" => Ok(Self::Both),
            other => Err(CategoryKindError {
                got: other.to_owned(),
            }),
        }
    }

    /// Indica si la categoria se ofrece para un movimiento del tipo dado.
    #[must_use]
    pub fn accepts(self, movement_kind: Self) -> bool {
        self == Self::Both || self == movement_kind
    }

    /// Indica si el usuario puede elegir este tipo al crear una categoria.
    #[must_use]
    pub fn is_selectable_by_users(self) -> bool {
        self != Self::Both
    }
}

impl fmt::Display for CategoryKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}
