//! DTOs del contexto de categorias.
//!
//! Convenciones en [`crate::serialization`]: claves en camelCase y cuerpos de
//! entrada que rechazan campos desconocidos.
//!
//! La categoria no lleva sus instantes tecnicos: la pantalla de Categorias pinta el
//! tile, el nombre y el conteo de uso, y nada mas.

use gastonomo_application::port::category::CategoryUsage;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Una categoria tal y como la pinta la pantalla.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CategoryDto {
    /// Identificador, que es lo que guardan los movimientos.
    #[schema(example = "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34")]
    pub id: String,
    /// Nombre que ve el usuario.
    #[schema(example = "Mascotas")]
    pub name: String,
    /// Tipo: `in`, `out` o `both`.
    ///
    /// `both` solo lo llevan tres predeterminadas (Prestamo, Abono prestamo y
    /// Otros), que el diseno ensena en el grid de gasto y en el de ingreso. El
    /// cliente ofrece la categoria cuando el tipo es `both` o coincide con el del
    /// movimiento.
    #[schema(example = "out")]
    pub kind: String,
    /// Material Symbol del tile.
    #[schema(example = "pets")]
    pub icon: String,
    /// Color del tile, en `#rrggbb`.
    #[schema(example = "#2f7fb5")]
    pub color: String,
    /// Indica si es una de las 12 predeterminadas, que no se editan ni se borran.
    pub is_default: bool,
    /// Movimientos que la usan, para el texto "N movimientos".
    #[schema(example = 3)]
    pub movement_count: u32,
}

impl From<&CategoryUsage> for CategoryDto {
    fn from(usage: &CategoryUsage) -> Self {
        let category = &usage.category;
        Self {
            id: category.id().to_string(),
            name: category.name().to_string(),
            kind: category.kind().as_str().to_owned(),
            icon: category.icon().to_string(),
            color: category.color().to_string(),
            is_default: category.is_default(),
            movement_count: usage.movements,
        }
    }
}

/// Respuesta de `GET /v1/categories`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CategoryListDto {
    /// Todas las categorias de la cuenta, las predeterminadas primero.
    ///
    /// La clave es `items`, como en el resto de listas de la API (prestamos,
    /// monedas y movimientos), para que el cliente tenga una sola convencion.
    pub items: Vec<CategoryDto>,
}

impl CategoryListDto {
    /// Construye la respuesta desde el resultado del caso de uso.
    #[must_use]
    pub fn new(found: &[CategoryUsage]) -> Self {
        Self {
            items: found.iter().map(CategoryDto::from).collect(),
        }
    }
}

/// Cuerpo de `POST /v1/categories`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateCategoryRequestDto {
    /// Nombre, obligatorio y de 22 caracteres como maximo.
    #[schema(example = "Mascotas")]
    pub name: String,
    /// Tipo: `in` o `out`. El usuario no puede crear una de tipo `both`.
    #[schema(example = "out")]
    pub kind: String,
    /// Uno de los 16 iconos de la rejilla del sheet.
    #[schema(example = "pets")]
    pub icon: String,
    /// Uno de los 8 colores del sheet.
    #[schema(example = "#2f7fb5")]
    pub color: String,
}

/// Cuerpo de `PATCH /v1/categories/{id}`.
///
/// Todos los campos son opcionales: lo que no viene se queda como estaba.
#[derive(Debug, Clone, Default, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateCategoryRequestDto {
    /// Nombre nuevo.
    #[serde(default)]
    #[schema(example = "Veterinario")]
    pub name: Option<String>,
    /// Tipo nuevo: `in` o `out`.
    #[serde(default)]
    pub kind: Option<String>,
    /// Icono nuevo.
    #[serde(default)]
    pub icon: Option<String>,
    /// Color nuevo.
    #[serde(default)]
    pub color: Option<String>,
}
