//! DTOs del feed de cambios.
//!
//! Las colecciones reutilizan los DTO de cada contexto: un movimiento del feed es
//! el mismo movimiento que devuelve `GET /v1/movements`, asi que el cliente tiene
//! un solo mapeo por tipo y no dos que se puedan desincronizar.

use base64::Engine as _;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use gastonomo_application::error::AppError;
use gastonomo_application::port::changes::{ChangeBatch, ChangeNumber};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use super::category::CategoryDto;
use super::loan::{LoanDto, LoanPaymentDto};
use super::movement::MovementDto;
use super::settings::SettingsDto;

/// Prefijo del cursor. Va dentro para poder cambiar el formato algun dia y
/// reconocer los cursores viejos en vez de leerlos como otra cosa.
const CURSOR_VERSION: &str = "v1:";

/// Convierte un numero de cambio en el cursor que viaja al cliente.
///
/// Va codificado **a proposito**. El `nextCursor` de movimientos es legible
/// (`YYYY-MM-DD_uuid`) y eso invita a construirlo a mano; un cursor que alguien
/// fabrica es un cliente que se salta cambios sin enterarse.
#[must_use]
pub fn encode_cursor(cursor: ChangeNumber) -> String {
    URL_SAFE_NO_PAD.encode(format!("{CURSOR_VERSION}{cursor}"))
}

/// Lee el cursor que devolvio una respuesta anterior.
///
/// # Errors
///
/// Devuelve [`AppError::Validation`] si no es un cursor de esta API. Se rechaza en
/// vez de tratarlo como "desde el principio": un cursor roto que se leyera como
/// arranque en frio haria que el cliente se bajara el historico entero sin saber
/// por que.
pub fn decode_cursor(raw: &str) -> Result<ChangeNumber, AppError> {
    URL_SAFE_NO_PAD
        .decode(raw)
        .ok()
        .and_then(|bytes| String::from_utf8(bytes).ok())
        .and_then(|text| text.strip_prefix(CURSOR_VERSION).map(str::to_owned))
        .and_then(|number| number.parse::<ChangeNumber>().ok())
        .filter(|number| *number >= 0)
        .ok_or_else(|| {
            AppError::validation(
                "since",
                "El cursor no es uno de esta API. Vuelve a sincronizar desde el principio.",
            )
        })
}

/// Respuesta de `GET /v1/changes`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ChangeBatchDto {
    /// Cursor al que llega este lote. Se guarda **despues** de escribir el lote.
    #[schema(example = "djE6NDI")]
    pub cursor: String,
    /// True si quedan cambios por encima del cursor.
    pub has_more: bool,
    /// Movimientos nuevos.
    pub movements: Vec<MovementDto>,
    /// Prestamos nuevos, con sus abonos.
    pub loans: Vec<LoanDto>,
    /// Abonos cuyo prestamo puede ser mas viejo que el lote.
    pub loan_payments: Vec<LoanPaymentDto>,
    /// Categorias nuevas, renombradas, archivadas o recuperadas.
    pub categories: Vec<CategoryDto>,
    /// Los ajustes, solo si cambiaron. Ausente si no.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<SettingsDto>,
}

impl From<&ChangeBatch> for ChangeBatchDto {
    fn from(batch: &ChangeBatch) -> Self {
        Self {
            cursor: encode_cursor(batch.cursor),
            has_more: batch.has_more,
            movements: batch.movements.iter().map(MovementDto::from).collect(),
            loans: batch.loans.iter().map(LoanDto::from).collect(),
            loan_payments: batch.payments.iter().map(LoanPaymentDto::from).collect(),
            categories: batch.categories.iter().map(CategoryDto::from).collect(),
            settings: batch.settings.as_ref().map(SettingsDto::from),
        }
    }
}
