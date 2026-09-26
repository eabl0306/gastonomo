//! Cuerpo de `POST /v1/movements`.

use gastonomo_application::use_case::movement::CreateMovementCommand;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Lo que manda el formulario de Nuevo movimiento.
///
/// Los importes llegan como string a proposito: un `186400.0000` sin comillas
/// termina en un `double` en cuanto el cliente lo parsea.
///
/// `base` y `baseCurrency` se pueden omitir los dos juntos, que es el caso del
/// usuario que teclea en su moneda principal: entonces la equivalencia es el propio
/// importe. Si se manda uno hay que mandar el otro, y la API los guarda tal cual,
/// incluida la tasa que el usuario haya editado en la caja de conversion.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateMovementRequestDto {
    /// Fecha del registro, sin hora ni zona.
    #[schema(example = "2026-09-15")]
    pub occurred_on: String,
    /// `in` para un ingreso, `out` para un gasto.
    #[schema(example = "out")]
    pub kind: String,
    /// Categoria elegida en el grid.
    #[schema(example = "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34")]
    pub category_id: String,
    /// Nota opcional. Si va vacia, el titulo es el nombre de la categoria.
    #[schema(example = "Mercado")]
    pub note: Option<String>,
    /// Importe tecleado.
    #[schema(example = "186400")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia en la moneda principal.
    #[schema(example = "450000")]
    pub base: Option<String>,
    /// Moneda de la equivalencia.
    #[schema(example = "COP")]
    pub base_currency: Option<String>,
}

impl From<CreateMovementRequestDto> for CreateMovementCommand {
    fn from(body: CreateMovementRequestDto) -> Self {
        Self {
            occurred_on: body.occurred_on,
            kind: body.kind,
            category_id: body.category_id,
            note: body.note,
            amount: body.amount,
            currency: body.currency,
            base: body.base,
            base_currency: body.base_currency,
        }
    }
}
