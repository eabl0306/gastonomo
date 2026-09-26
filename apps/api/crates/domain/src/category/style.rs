//! Icono y color del tile de una categoria.
//!
//! El sheet de categoria del diseno no deja escribir un icono ni un color: ofrece
//! una rejilla de 16 iconos y 8 circulos de color. Por eso las categorias del
//! usuario solo admiten esos valores, y asi la app nunca se encuentra con un tile
//! que no sabe pintar. Las predeterminadas usan iconos y colores propios, fuera de
//! esas dos listas, de modo que la validacion de la paleta es de la categoria que
//! crea el usuario y no del tipo en si.

use std::fmt;

use thiserror::Error;

/// Los 16 iconos que ofrece la rejilla del sheet, en su orden.
pub const CUSTOM_CATEGORY_ICONS: [&str; 16] = [
    "shopping_cart",
    "pets",
    "fitness_center",
    "local_cafe",
    "flight",
    "checkroom",
    "child_care",
    "sports_esports",
    "card_giftcard",
    "local_bar",
    "spa",
    "build",
    "savings",
    "subscriptions",
    "local_gas_station",
    "volunteer_activism",
];

/// Los 8 colores que ofrece el sheet, en su orden.
pub const CUSTOM_CATEGORY_COLORS: [&str; 8] = [
    "#cf5f35", "#c48f12", "#2f8a55", "#2f7fb5", "#3f6fc4", "#7a52c7", "#b84a86", "#5b6573",
];

/// Longitud maxima del nombre de un Material Symbol.
const MAX_ICON_LENGTH: usize = 40;

/// Motivo por el que un icono no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CategoryIconError {
    /// Esta vacio o pasa de {MAX_ICON_LENGTH} caracteres.
    #[error("an icon name must have between 1 and {MAX_ICON_LENGTH} characters")]
    Length,
    /// Lleva algo que no es el nombre de un Material Symbol.
    #[error("{got:?} is not a material symbol name")]
    Shape {
        /// Valor recibido.
        got: String,
    },
}

/// Nombre de un Material Symbol.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct CategoryIcon(String);

impl CategoryIcon {
    /// Valida y normaliza el nombre de un icono.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryIconError`] si no parece un Material Symbol.
    pub fn parse(raw: &str) -> Result<Self, CategoryIconError> {
        let trimmed = raw.trim().to_ascii_lowercase();
        if trimmed.is_empty() || trimmed.chars().count() > MAX_ICON_LENGTH {
            return Err(CategoryIconError::Length);
        }
        if !trimmed
            .chars()
            .all(|c| c.is_ascii_lowercase() || c.is_ascii_digit() || c == '_')
        {
            return Err(CategoryIconError::Shape { got: trimmed });
        }
        Ok(Self(trimmed))
    }

    /// Nombre normalizado.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    /// Indica si el icono esta en la rejilla que ofrece el sheet.
    #[must_use]
    pub fn is_offered_to_users(&self) -> bool {
        CUSTOM_CATEGORY_ICONS.contains(&self.0.as_str())
    }
}

impl fmt::Display for CategoryIcon {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

/// El texto recibido no es un color hexadecimal de seis digitos.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a #rrggbb color")]
pub struct CategoryColorError {
    /// Valor recibido.
    pub got: String,
}

/// Color del tile, en hexadecimal `#rrggbb` y en minusculas.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct CategoryColor(String);

impl CategoryColor {
    /// Valida y normaliza un color.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryColorError`] si no es `#rrggbb`.
    pub fn parse(raw: &str) -> Result<Self, CategoryColorError> {
        let trimmed = raw.trim().to_ascii_lowercase();
        let digits = trimmed.strip_prefix('#').unwrap_or_default();
        if digits.len() != 6 || !digits.chars().all(|c| c.is_ascii_hexdigit()) {
            return Err(CategoryColorError {
                got: raw.trim().to_owned(),
            });
        }
        Ok(Self(trimmed))
    }

    /// Color normalizado, con el `#` delante.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    /// Indica si el color esta entre los 8 que ofrece el sheet.
    #[must_use]
    pub fn is_offered_to_users(&self) -> bool {
        CUSTOM_CATEGORY_COLORS.contains(&self.0.as_str())
    }
}

impl fmt::Display for CategoryColor {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
