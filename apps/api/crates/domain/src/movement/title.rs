//! Titulo de un movimiento.

use std::fmt;

use thiserror::Error;

/// Longitud maxima del titulo, la misma que el CHECK de la columna.
pub const MAX_TITLE_LENGTH: usize = 120;

/// Motivo por el que el titulo no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MovementTitleError {
    /// Titulo vacio tras recortar espacios.
    #[error("the title cannot be empty")]
    Empty,
    /// Titulo mas largo de lo que admite la columna.
    #[error("the title cannot be longer than {MAX_TITLE_LENGTH} characters")]
    TooLong,
}

/// Texto que la fila de movimiento ensena como titulo.
///
/// La regla es del diseno: si el usuario no escribio nota, el titulo del
/// movimiento es el nombre de la categoria. Se resuelve aqui y no en el cliente
/// para que la exportacion, la lista y cualquier cliente futuro digan lo mismo.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MovementTitle(String);

impl MovementTitle {
    /// Construye el titulo a partir del texto ya decidido.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementTitleError`] si queda vacio o pasa de
    /// [`MAX_TITLE_LENGTH`] caracteres.
    pub fn parse(raw: &str) -> Result<Self, MovementTitleError> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return Err(MovementTitleError::Empty);
        }
        if trimmed.chars().count() > MAX_TITLE_LENGTH {
            return Err(MovementTitleError::TooLong);
        }
        Ok(Self(trimmed.to_owned()))
    }

    /// Aplica la regla del diseno: la nota manda y, si no hay, la categoria.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementTitleError`] si el texto resultante no es valido.
    pub fn from_note_or_category(
        note: Option<&str>,
        category_name: &str,
    ) -> Result<Self, MovementTitleError> {
        let note = note.map(str::trim).filter(|note| !note.is_empty());
        Self::parse(note.unwrap_or(category_name))
    }

    /// Texto del titulo.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for MovementTitle {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
