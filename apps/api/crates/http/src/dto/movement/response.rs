//! Respuestas de la lista y del alta de movimientos.

use gastonomo_application::use_case::movement::MovementPage;
use gastonomo_domain::movement::Movement;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use crate::serialization::{date_to_string, timestamp_to_string};

/// Categoria tal y como viaja dentro de un movimiento.
///
/// Lleva icono y color porque la fila de la pantalla de Movimientos pinta el tile
/// con ellos: sin esto el cliente tendria que cruzar cada fila con el catalogo.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct MovementCategoryDto {
    /// Identificador de la categoria.
    #[schema(example = "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34")]
    pub id: String,
    /// Nombre que ve el usuario.
    #[schema(example = "Alimentación")]
    pub name: String,
    /// `in`, `out` o `both`.
    #[schema(example = "out")]
    pub kind: String,
    /// Material Symbol del tile.
    #[schema(example = "restaurant")]
    pub icon: String,
    /// Color del tile, en `#rrggbb`.
    #[schema(example = "#cf5f35")]
    pub color: String,
}

/// Un movimiento del periodo.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct MovementDto {
    /// Identificador del movimiento.
    #[schema(example = "6f1f3b2a-1c44-4f8f-9c0a-7d5e2b1a9c31")]
    pub id: String,
    /// Fecha del registro, sin hora ni zona.
    #[schema(example = "2026-09-15")]
    pub occurred_on: String,
    /// `in` o `out`.
    #[schema(example = "out")]
    pub kind: String,
    /// Titulo de la fila: la nota, o el nombre de la categoria si no hay nota.
    #[schema(example = "Mercado")]
    pub title: String,
    /// Importe tal y como lo tecleo el usuario.
    #[schema(example = "186400.0000")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia guardada en la moneda principal del momento.
    #[schema(example = "186400.0000")]
    pub base: String,
    /// Moneda de la equivalencia.
    #[schema(example = "COP")]
    pub base_currency: String,
    /// Categoria completa.
    pub category: MovementCategoryDto,
    /// Prestamo que lo genero, o `null`.
    pub loan_id: Option<String>,
    /// Abono que lo genero, o `null`.
    pub loan_payment_id: Option<String>,
    /// Instante de creacion, en RFC 3339 UTC.
    #[schema(example = "2026-09-15T18:20:00Z")]
    pub created_at: String,
}

impl From<&Movement> for MovementDto {
    fn from(movement: &Movement) -> Self {
        let amount = movement.amount();
        let category = movement.category();
        Self {
            id: movement.id().to_string(),
            occurred_on: date_to_string(movement.occurred_on()),
            kind: movement.kind().as_str().to_owned(),
            title: movement.title().as_str().to_owned(),
            amount: amount.amount().to_storage_string(),
            currency: amount.currency().as_str().to_owned(),
            base: amount.base().to_storage_string(),
            base_currency: amount.base_currency().as_str().to_owned(),
            category: MovementCategoryDto {
                id: category.id().to_string(),
                name: category.name().to_owned(),
                kind: category.kind().as_str().to_owned(),
                icon: category.icon().to_owned(),
                color: category.color().to_owned(),
            },
            loan_id: movement.loan_id().map(|id| id.to_string()),
            loan_payment_id: movement.loan_payment_id().map(|id| id.to_string()),
            created_at: timestamp_to_string(movement.created_at()),
        }
    }
}

/// Una pagina de movimientos.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct MovementPageDto {
    /// Movimientos, de mas reciente a mas antiguo.
    pub items: Vec<MovementDto>,
    /// Valor que hay que devolver en `cursor` para pedir la pagina siguiente.
    ///
    /// Va a `null` cuando esta fue la ultima pagina, que es como el cliente sabe
    /// que ya tiene el periodo completo y puede sumar totales con confianza.
    pub next_cursor: Option<String>,
}

impl From<&MovementPage> for MovementPageDto {
    fn from(page: &MovementPage) -> Self {
        Self {
            items: page.items.iter().map(MovementDto::from).collect(),
            next_cursor: page.next_cursor.map(|cursor| cursor.to_string()),
        }
    }
}
