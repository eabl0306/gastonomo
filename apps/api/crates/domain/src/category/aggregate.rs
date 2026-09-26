//! Agregado de categoria.

use chrono::{DateTime, Utc};
use thiserror::Error;

use super::identifier::CategoryId;
use super::kind::CategoryKind;
use super::name::CategoryName;
use super::style::{CategoryColor, CategoryIcon};
use crate::value_object::UserId;

/// Motivo por el que una categoria no se puede crear o cambiar.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CategoryError {
    /// El tipo `both` esta reservado a las predeterminadas.
    #[error("users can only create categories of kind in or out")]
    KindNotSelectable,
    /// El icono no esta en la rejilla del sheet.
    #[error("the icon is not one of the sixteen the app offers")]
    IconNotOffered,
    /// El color no esta entre los 8 del sheet.
    #[error("the color is not one of the eight the app offers")]
    ColorNotOffered,
    /// Se intento editar o borrar una predeterminada.
    #[error("a default category cannot be edited or deleted")]
    DefaultIsReadOnly,
}

/// Datos con los que nace una categoria.
#[derive(Debug, Clone)]
pub struct NewCategory {
    /// Cuenta a la que pertenece.
    pub user_id: UserId,
    /// Nombre que ve el usuario.
    pub name: CategoryName,
    /// Tipo de movimiento al que aplica.
    pub kind: CategoryKind,
    /// Material Symbol del tile.
    pub icon: CategoryIcon,
    /// Color del tile.
    pub color: CategoryColor,
    /// Instante de creacion.
    pub created_at: DateTime<Utc>,
}

/// Cambio parcial de una categoria, tal y como llega en un `PATCH`.
///
/// Un campo a `None` se queda como estaba.
#[derive(Debug, Clone, Default)]
pub struct CategoryChange {
    /// Nombre nuevo.
    pub name: Option<CategoryName>,
    /// Tipo nuevo.
    pub kind: Option<CategoryKind>,
    /// Icono nuevo.
    pub icon: Option<CategoryIcon>,
    /// Color nuevo.
    pub color: Option<CategoryColor>,
}

impl CategoryChange {
    /// Indica si el cuerpo no pide cambiar nada.
    #[must_use]
    pub const fn is_empty(&self) -> bool {
        self.name.is_none() && self.kind.is_none() && self.icon.is_none() && self.color.is_none()
    }
}

/// Categoria de movimientos de una cuenta.
///
/// Las 12 predeterminadas se siembran al registrarse y llevan `is_default`: no se
/// pueden editar ni borrar, y son las unicas que pueden ser de tipo `both`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Category {
    id: CategoryId,
    user_id: UserId,
    name: CategoryName,
    kind: CategoryKind,
    icon: CategoryIcon,
    color: CategoryColor,
    is_default: bool,
    created_at: DateTime<Utc>,
    updated_at: DateTime<Utc>,
}

impl Category {
    /// Crea una categoria del usuario, con las reglas del sheet del diseno.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryError`] si el tipo es `both`, si el icono no esta en la
    /// rejilla de 16 o si el color no esta entre los 8 que ofrece la app.
    pub fn create_custom(new_category: NewCategory) -> Result<Self, CategoryError> {
        check_selectable(new_category.kind)?;
        check_style(&new_category.icon, &new_category.color)?;
        Ok(Self {
            id: CategoryId::generate(),
            user_id: new_category.user_id,
            name: new_category.name,
            kind: new_category.kind,
            icon: new_category.icon,
            color: new_category.color,
            is_default: false,
            created_at: new_category.created_at,
            updated_at: new_category.created_at,
        })
    }

    /// Reconstruye una categoria leida de la base de datos.
    #[must_use]
    pub fn rehydrate(
        id: CategoryId,
        new_category: NewCategory,
        is_default: bool,
        updated_at: DateTime<Utc>,
    ) -> Self {
        Self {
            id,
            user_id: new_category.user_id,
            name: new_category.name,
            kind: new_category.kind,
            icon: new_category.icon,
            color: new_category.color,
            is_default,
            created_at: new_category.created_at,
            updated_at,
        }
    }

    /// Aplica un cambio parcial.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryError::DefaultIsReadOnly`] si es una predeterminada, y
    /// los mismos errores que [`Category::create_custom`] si el tipo, el icono o
    /// el color nuevos no valen.
    pub fn apply(
        &mut self,
        change: CategoryChange,
        now: DateTime<Utc>,
    ) -> Result<(), CategoryError> {
        self.check_writable()?;
        let kind = change.kind.unwrap_or(self.kind);
        let icon = change.icon.unwrap_or_else(|| self.icon.clone());
        let color = change.color.unwrap_or_else(|| self.color.clone());
        check_selectable(kind)?;
        check_style(&icon, &color)?;
        if let Some(name) = change.name {
            self.name = name;
        }
        self.kind = kind;
        self.icon = icon;
        self.color = color;
        self.updated_at = now;
        Ok(())
    }

    /// Comprueba que la categoria se puede editar o borrar.
    ///
    /// # Errors
    ///
    /// Devuelve [`CategoryError::DefaultIsReadOnly`] si es una predeterminada.
    pub const fn check_writable(&self) -> Result<(), CategoryError> {
        if self.is_default {
            return Err(CategoryError::DefaultIsReadOnly);
        }
        Ok(())
    }

    /// Identificador.
    #[must_use]
    pub const fn id(&self) -> CategoryId {
        self.id
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.user_id
    }

    /// Nombre que ve el usuario.
    #[must_use]
    pub const fn name(&self) -> &CategoryName {
        &self.name
    }

    /// Tipo de movimiento al que aplica.
    #[must_use]
    pub const fn kind(&self) -> CategoryKind {
        self.kind
    }

    /// Material Symbol del tile.
    #[must_use]
    pub const fn icon(&self) -> &CategoryIcon {
        &self.icon
    }

    /// Color del tile.
    #[must_use]
    pub const fn color(&self) -> &CategoryColor {
        &self.color
    }

    /// Indica si es una de las 12 predeterminadas.
    #[must_use]
    pub const fn is_default(&self) -> bool {
        self.is_default
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.created_at
    }

    /// Instante del ultimo cambio.
    #[must_use]
    pub const fn updated_at(&self) -> DateTime<Utc> {
        self.updated_at
    }
}

fn check_selectable(kind: CategoryKind) -> Result<(), CategoryError> {
    if kind.is_selectable_by_users() {
        Ok(())
    } else {
        Err(CategoryError::KindNotSelectable)
    }
}

fn check_style(icon: &CategoryIcon, color: &CategoryColor) -> Result<(), CategoryError> {
    if !icon.is_offered_to_users() {
        return Err(CategoryError::IconNotOffered);
    }
    if !color.is_offered_to_users() {
        return Err(CategoryError::ColorNotOffered);
    }
    Ok(())
}
