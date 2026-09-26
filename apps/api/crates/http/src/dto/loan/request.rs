//! Cuerpos de las peticiones de prestamos y abonos.

use serde::Deserialize;
use utoipa::ToSchema;

/// Cuerpo de `POST /v1/loans`, el formulario de Nuevo prestamo.
///
/// `amount` y `currency` son obligatorios. `base` y `baseCurrency` se pueden
/// omitir los dos juntos, exactamente igual que en `POST /v1/movements`: es el caso
/// del prestamo en la moneda principal, donde la equivalencia es el propio importe.
/// La API no calcula equivalencias: guarda la que el cliente manda, que puede ser
/// la tasa sugerida por Frankfurter o la que el usuario edito a mano.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateLoanRequestDto {
    /// Persona a la que preste o que me presto.
    #[schema(example = "Rosa")]
    pub person: String,
    /// Nota libre. Si no viene, se guarda vacia.
    #[serde(default)]
    #[schema(example = "Teléfono")]
    pub note: String,
    /// `lent` si preste, `borrowed` si me prestaron.
    #[schema(example = "lent")]
    pub direction: String,
    /// Fecha del prestamo.
    #[schema(example = "2026-06-28")]
    pub opened_on: String,
    /// Importe tal y como lo tecleo el usuario.
    #[schema(example = "600000.00")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia en la moneda principal del momento. Opcional junto con
    /// `baseCurrency`.
    #[schema(example = "600000.00")]
    pub base: Option<String>,
    /// Moneda de la equivalencia. Va siempre con `base`.
    #[schema(example = "COP")]
    pub base_currency: Option<String>,
}

/// Cuerpo de `POST /v1/loans/{id}/payments`, el sheet Registrar abono.
///
/// Un abono puede pasarse del saldo: el diseno lo contempla y ensena el exceso
/// como "Intereses ganados".
///
/// `base` y `baseCurrency` son opcionales los dos juntos, como en el resto de la
/// API.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateLoanPaymentRequestDto {
    /// Fecha del abono.
    #[schema(example = "2026-07-29")]
    pub paid_on: String,
    /// Concepto opcional: en el diseno, "Detalles" o "Intereses".
    #[serde(default)]
    #[schema(example = "Intereses")]
    pub label: Option<String>,
    /// Importe tal y como lo tecleo el usuario.
    #[schema(example = "200000.00")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia en la moneda principal del momento. Opcional junto con
    /// `baseCurrency`.
    #[schema(example = "200000.00")]
    pub base: Option<String>,
    /// Moneda de la equivalencia. Va siempre con `base`.
    #[schema(example = "COP")]
    pub base_currency: Option<String>,
}
