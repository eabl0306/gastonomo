//! Lectura de los campos del cuerpo a tipos de dominio.
//!
//! Vive aparte porque los cuatro casos de uso del contexto validan los mismos
//! campos y el mapeo de errores tiene que ser identico en crear y en editar.

use gastonomo_domain::category::{
    CUSTOM_CATEGORY_COLORS, CUSTOM_CATEGORY_ICONS, CategoryColor, CategoryError, CategoryIcon,
    CategoryKind, CategoryName, MAX_CATEGORY_NAME_LENGTH,
};

use crate::error::{AppError, ValidationErrors};

/// Campos de una categoria tal y como llegan en el JSON.
#[derive(Debug, Clone, Default)]
pub struct RawCategoryFields {
    /// Nombre sin validar.
    pub name: Option<String>,
    /// Tipo sin validar: `in` o `out`.
    pub kind: Option<String>,
    /// Icono sin validar.
    pub icon: Option<String>,
    /// Color sin validar.
    pub color: Option<String>,
}

/// Los mismos campos ya convertidos a dominio.
#[derive(Debug, Clone, Default)]
pub struct ParsedCategoryFields {
    /// Nombre validado.
    pub name: Option<CategoryName>,
    /// Tipo validado.
    pub kind: Option<CategoryKind>,
    /// Icono validado.
    pub icon: Option<CategoryIcon>,
    /// Color validado.
    pub color: Option<CategoryColor>,
}

/// Convierte los campos recibidos, juntando todos los errores en una respuesta.
///
/// Comprueba tambien que el tipo, el icono y el color sean de los que ofrece el
/// sheet. El dominio lo vuelve a comprobar al construir el agregado, pero aqui se
/// hace campo por campo: la pantalla marca cada campo con su motivo y una respuesta
/// que solo nombrara el primero obligaria a tres viajes.
///
/// # Errors
///
/// Devuelve [`AppError::Validation`] con un error por campo que no vale.
pub fn parse_fields(raw: &RawCategoryFields) -> Result<ParsedCategoryFields, AppError> {
    let mut errors = ValidationErrors::new();
    let mut parsed = ParsedCategoryFields::default();

    if let Some(value) = raw.name.as_deref() {
        match CategoryName::parse(value) {
            Ok(name) => parsed.name = Some(name),
            Err(_) => errors.push(
                "name",
                format!("El nombre es obligatorio y admite {MAX_CATEGORY_NAME_LENGTH} caracteres."),
            ),
        }
    }
    if let Some(value) = raw.kind.as_deref() {
        match CategoryKind::parse(value) {
            Ok(kind) if kind.is_selectable_by_users() => parsed.kind = Some(kind),
            Ok(_) | Err(_) => errors.push("kind", "El tipo tiene que ser `in` o `out`."),
        }
    }
    if let Some(value) = raw.icon.as_deref() {
        match CategoryIcon::parse(value) {
            Ok(icon) if icon.is_offered_to_users() => parsed.icon = Some(icon),
            Ok(_) | Err(_) => errors.push("icon", offered_icons()),
        }
    }
    if let Some(value) = raw.color.as_deref() {
        match CategoryColor::parse(value) {
            Ok(color) if color.is_offered_to_users() => parsed.color = Some(color),
            Ok(_) | Err(_) => errors.push("color", offered_colors()),
        }
    }
    errors.into_result()?;
    Ok(parsed)
}

/// Traduce un error del dominio al error de la capa de aplicacion.
///
/// Los tres primeros son datos que manda el cliente, asi que son 422. Tocar una
/// predeterminada es un 409: el cuerpo esta bien, la operacion no cabe.
#[must_use]
pub fn domain_error(error: &CategoryError) -> AppError {
    match error {
        CategoryError::KindNotSelectable => {
            AppError::validation("kind", "El tipo tiene que ser `in` o `out`.")
        }
        CategoryError::IconNotOffered => AppError::validation("icon", offered_icons()),
        CategoryError::ColorNotOffered => AppError::validation("color", offered_colors()),
        CategoryError::DefaultIsReadOnly => AppError::conflict(
            "category_is_default",
            "Las categorias predeterminadas no se pueden editar ni borrar.",
        ),
    }
}

fn offered_icons() -> String {
    format!(
        "El icono tiene que ser uno de: {}.",
        CUSTOM_CATEGORY_ICONS.join(", ")
    )
}

fn offered_colors() -> String {
    format!(
        "El color tiene que ser uno de: {}.",
        CUSTOM_CATEGORY_COLORS.join(", ")
    )
}
