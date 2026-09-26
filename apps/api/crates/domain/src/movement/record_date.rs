//! Fecha de registro de un movimiento.
//!
//! Es un `DATE`, sin hora ni zona: un gasto del 22 de septiembre es del 22 en
//! Bogota y en Madrid. Eso obliga a ser generoso con el futuro. El telefono de un
//! usuario en UTC+14 ya esta en el dia siguiente cuando en UTC todavia es hoy, asi
//! que una fecha un dia por delante de hoy en UTC es legitima, no un error.

use chrono::{Days, NaiveDate};
use thiserror::Error;

/// Formato de las fechas de registro en el contrato publico.
///
/// Es el mismo que usa `crates/http/src/serialization.rs`: si uno de los dos
/// cambia, el otro tambien.
pub const RECORD_DATE_FORMAT: &str = "%Y-%m-%d";

/// Dias de margen hacia el futuro, por la diferencia de zona horaria.
pub const FUTURE_TOLERANCE_DAYS: u64 = 1;

/// Motivo por el que la fecha no es valida.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum RecordDateError {
    /// El texto no es `YYYY-MM-DD`.
    #[error("{got:?} is not a date in YYYY-MM-DD format")]
    NotADate {
        /// Valor recibido.
        got: String,
    },
    /// La fecha es anterior a lo que la aplicacion considera razonable.
    #[error("the date is before {}", earliest_record_date())]
    TooOld,
    /// La fecha va demasiado por delante de hoy.
    #[error("the date is more than {FUTURE_TOLERANCE_DAYS} day(s) in the future")]
    InTheFuture,
}

/// Fecha mas antigua que se acepta en un registro.
#[must_use]
pub fn earliest_record_date() -> NaiveDate {
    NaiveDate::from_ymd_opt(2000, 1, 1).unwrap_or(NaiveDate::MIN)
}

/// Lee una fecha de registro de su representacion textual.
///
/// # Errors
///
/// Devuelve [`RecordDateError::NotADate`] si el texto no es `YYYY-MM-DD`.
pub fn parse_record_date(raw: &str) -> Result<NaiveDate, RecordDateError> {
    NaiveDate::parse_from_str(raw.trim(), RECORD_DATE_FORMAT).map_err(|_| {
        RecordDateError::NotADate {
            got: raw.trim().to_owned(),
        }
    })
}

/// Comprueba que la fecha cae en la ventana que la aplicacion admite.
///
/// # Errors
///
/// Devuelve [`RecordDateError::TooOld`] o [`RecordDateError::InTheFuture`] segun
/// el lado por el que se sale.
pub fn validate_record_date(date: NaiveDate, today: NaiveDate) -> Result<(), RecordDateError> {
    if date < earliest_record_date() {
        return Err(RecordDateError::TooOld);
    }
    let latest = today
        .checked_add_days(Days::new(FUTURE_TOLERANCE_DAYS))
        .unwrap_or(NaiveDate::MAX);
    if date > latest {
        return Err(RecordDateError::InTheFuture);
    }
    Ok(())
}
