//! Identificador de un componente observable del sistema.

use thiserror::Error;

/// Longitud maxima admitida para el nombre de un componente.
const MAX_LENGTH: usize = 64;

/// Motivos por los que un nombre de componente puede rechazarse.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum ComponentNameError {
    /// El nombre estaba vacio o solo contenia espacios.
    #[error("component name must not be empty")]
    Empty,
    /// El nombre supera el limite de caracteres permitido.
    #[error("component name must not exceed {MAX_LENGTH} characters, got {0}")]
    TooLong(usize),
    /// El nombre contiene un caracter fuera del juego permitido.
    #[error("component name contains an unsupported character: {0:?}")]
    UnsupportedCharacter(char),
}

/// Nombre normalizado de un componente (por ejemplo `database`).
///
/// Solo admite minusculas ASCII, digitos, guion y guion bajo. Se normaliza a
/// minusculas y se recortan los espacios de los extremos.
#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct ComponentName(String);

impl ComponentName {
    /// Construye un nombre validado a partir de texto libre.
    ///
    /// # Errors
    ///
    /// Devuelve [`ComponentNameError`] si el texto esta vacio, es demasiado
    /// largo o contiene caracteres no admitidos.
    pub fn parse(raw: &str) -> Result<Self, ComponentNameError> {
        let trimmed = raw.trim().to_ascii_lowercase();
        if trimmed.is_empty() {
            return Err(ComponentNameError::Empty);
        }
        if trimmed.len() > MAX_LENGTH {
            return Err(ComponentNameError::TooLong(trimmed.len()));
        }
        if let Some(invalid) = trimmed.chars().find(|c| !is_allowed(*c)) {
            return Err(ComponentNameError::UnsupportedCharacter(invalid));
        }
        Ok(Self(trimmed))
    }

    /// Devuelve la representacion textual del nombre.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl std::fmt::Display for ComponentName {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str(&self.0)
    }
}

const fn is_allowed(character: char) -> bool {
    character.is_ascii_lowercase() || character.is_ascii_digit() || matches!(character, '-' | '_')
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::{ComponentName, ComponentNameError, MAX_LENGTH};

    #[test]
    fn normalizes_case_and_surrounding_spaces() {
        let name = ComponentName::parse("  DataBase  ").expect("name should be valid");
        assert_eq!(name.as_str(), "database");
        assert_eq!(name.to_string(), "database");
    }

    #[test]
    fn rejects_blank_names() {
        assert_eq!(ComponentName::parse("   "), Err(ComponentNameError::Empty));
    }

    #[test]
    fn rejects_names_over_the_limit() {
        let raw = "a".repeat(MAX_LENGTH + 1);
        assert_eq!(
            ComponentName::parse(&raw),
            Err(ComponentNameError::TooLong(MAX_LENGTH + 1))
        );
    }

    #[test]
    fn rejects_unsupported_characters() {
        assert_eq!(
            ComponentName::parse("data base"),
            Err(ComponentNameError::UnsupportedCharacter(' '))
        );
    }

    #[test]
    fn accepts_digits_dashes_and_underscores() {
        assert!(ComponentName::parse("cache-2_primary").is_ok());
    }
}
