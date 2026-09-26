//! Nombre de una categoria.

use std::fmt;

use thiserror::Error;

/// Longitud maxima que admite el campo del sheet de categoria.
pub const MAX_CATEGORY_NAME_LENGTH: usize = 22;

/// Motivo por el que un nombre de categoria no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CategoryNameError {
    /// El nombre esta vacio o solo tiene espacios.
    #[error("the category name is empty")]
    Empty,
    /// El nombre pasa de {MAX_CATEGORY_NAME_LENGTH} caracteres.
    #[error("the category name is longer than {MAX_CATEGORY_NAME_LENGTH} characters")]
    TooLong,
}

/// Nombre de categoria, recortado y de 22 caracteres como maximo.
///
/// La unicidad no se comprueba aqui: es entre todas las categorias del usuario,
/// sin distinguir tipo ni mayusculas, y eso lo garantiza un indice unico sobre
/// `(user_id, lower(name))`.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct CategoryName(String);

impl CategoryName {
    /// Valida y normaliza un nombre.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryNameError`] si esta vacio o pasa del maximo.
    pub fn parse(raw: &str) -> Result<Self, CategoryNameError> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return Err(CategoryNameError::Empty);
        }
        if trimmed.chars().count() > MAX_CATEGORY_NAME_LENGTH {
            return Err(CategoryNameError::TooLong);
        }
        Ok(Self(trimmed.to_owned()))
    }

    /// Nombre normalizado.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    /// Clave de comparacion: el nombre en minusculas, como el indice unico.
    #[must_use]
    pub fn comparison_key(&self) -> String {
        self.0.to_lowercase()
    }

    /// Indica si el nombre es el mismo sin distinguir mayusculas.
    #[must_use]
    pub fn same_as(&self, other: &str) -> bool {
        self.comparison_key() == other.trim().to_lowercase()
    }
}

impl fmt::Display for CategoryName {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
