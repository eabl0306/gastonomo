//! Nombre de una persona: el del usuario y, mas adelante, el de un prestamo.

use std::fmt;

use thiserror::Error;

/// Longitud maxima admitida.
const MAX_LENGTH: usize = 80;

/// Motivo por el que un nombre no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum PersonNameError {
    /// El nombre esta vacio o solo tiene espacios.
    #[error("the name is empty")]
    Empty,
    /// El nombre pasa de {MAX_LENGTH} caracteres.
    #[error("the name is longer than {MAX_LENGTH} characters")]
    TooLong,
}

/// Nombre con los espacios de los extremos recortados.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct PersonName(String);

impl PersonName {
    /// Valida y normaliza un nombre.
    ///
    /// # Errors
    ///
    /// Devuelve [`PersonNameError`] si esta vacio o es demasiado largo.
    pub fn parse(raw: &str) -> Result<Self, PersonNameError> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return Err(PersonNameError::Empty);
        }
        if trimmed.chars().count() > MAX_LENGTH {
            return Err(PersonNameError::TooLong);
        }
        Ok(Self(trimmed.to_owned()))
    }

    /// Nombre normalizado.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for PersonName {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
