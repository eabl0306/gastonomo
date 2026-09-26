//! La categoria tal y como viaja dentro de un movimiento.

use thiserror::Error;

use crate::category::{CategoryId, CategoryKind};

/// Motivo por el que una categoria no es valida.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MovementCategoryError {
    /// Nombre vacio.
    #[error("the category name cannot be empty")]
    EmptyName,
    /// Icono vacio.
    #[error("the category icon cannot be empty")]
    EmptyIcon,
    /// El color no es un hexadecimal de seis digitos.
    #[error("{got:?} is not a #rrggbb color")]
    InvalidColor {
        /// Valor recibido.
        got: String,
    },
}

/// Categoria que viaja dentro de un movimiento.
///
/// Lleva nombre, icono y color porque la fila de la pantalla de Movimientos pinta
/// el tile con ellos. Sin esto el cliente tendria que cruzar cada movimiento con
/// el catalogo de categorias, y el diseno pide la fila completa de una sola vez.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MovementCategory {
    id: CategoryId,
    name: String,
    kind: CategoryKind,
    icon: String,
    color: String,
}

impl MovementCategory {
    /// Construye la categoria comprobando lo mismo que los CHECK de la tabla.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementCategoryError`] si el nombre o el icono estan vacios, o
    /// si el color no es `#rrggbb`.
    pub fn new(
        id: CategoryId,
        name: &str,
        kind: CategoryKind,
        icon: &str,
        color: &str,
    ) -> Result<Self, MovementCategoryError> {
        let name = name.trim();
        let icon = icon.trim();
        let color = color.trim();
        if name.is_empty() {
            return Err(MovementCategoryError::EmptyName);
        }
        if icon.is_empty() {
            return Err(MovementCategoryError::EmptyIcon);
        }
        if !is_hex_color(color) {
            return Err(MovementCategoryError::InvalidColor {
                got: color.to_owned(),
            });
        }
        Ok(Self {
            id,
            name: name.to_owned(),
            kind,
            icon: icon.to_owned(),
            color: color.to_owned(),
        })
    }

    /// Identificador.
    #[must_use]
    pub const fn id(&self) -> CategoryId {
        self.id
    }

    /// Nombre que ve el usuario, y titulo del movimiento si no hay nota.
    #[must_use]
    pub fn name(&self) -> &str {
        &self.name
    }

    /// Tipo de movimiento al que aplica.
    #[must_use]
    pub const fn kind(&self) -> CategoryKind {
        self.kind
    }

    /// Material Symbol del tile.
    #[must_use]
    pub fn icon(&self) -> &str {
        &self.icon
    }

    /// Color del tile, en `#rrggbb`.
    #[must_use]
    pub fn color(&self) -> &str {
        &self.color
    }
}

fn is_hex_color(raw: &str) -> bool {
    raw.len() == 7
        && raw.starts_with('#')
        && raw
            .chars()
            .skip(1)
            .all(|character| character.is_ascii_hexdigit())
}
