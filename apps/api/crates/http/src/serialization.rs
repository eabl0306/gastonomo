//! Convenciones de serializacion del contrato publico.
//!
//! Se escriben aqui una vez para que ningun contexto las reinvente:
//!
//! - Las claves van en **camelCase** (`#[serde(rename_all = "camelCase")]`).
//! - Los importes viajan como **string**, nunca como numero: `"amount": "140.0000"`.
//!   Un JSON con `140.0000` sin comillas acaba en un `double` en cuanto el cliente
//!   lo parsea, y esto es una app de finanzas.
//! - Las fechas de registro son `YYYY-MM-DD`, sin hora ni zona.
//! - Los instantes tecnicos van en RFC 3339 con `Z`.
//! - Los identificadores son UUID en string.

use chrono::{DateTime, NaiveDate, SecondsFormat, Utc};

/// Formato de las fechas de registro.
pub const DATE_FORMAT: &str = "%Y-%m-%d";

/// Serializa una fecha de registro.
#[must_use]
pub fn date_to_string(date: NaiveDate) -> String {
    date.format(DATE_FORMAT).to_string()
}

/// Lee una fecha de registro del cuerpo de una peticion.
///
/// # Errors
///
/// Devuelve el error de `chrono` si el texto no es `YYYY-MM-DD`.
pub fn parse_date(raw: &str) -> Result<NaiveDate, chrono::ParseError> {
    NaiveDate::parse_from_str(raw.trim(), DATE_FORMAT)
}

/// Serializa un instante tecnico en RFC 3339 UTC, con segundos enteros.
#[must_use]
pub fn timestamp_to_string(instant: DateTime<Utc>) -> String {
    instant.to_rfc3339_opts(SecondsFormat::Secs, true)
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use chrono::{DateTime, Utc};

    use super::{date_to_string, parse_date, timestamp_to_string};

    #[test]
    fn a_record_date_has_no_time_and_no_zone() {
        let date = parse_date(" 2026-09-22 ").expect("should parse");
        assert_eq!(date_to_string(date), "2026-09-22");
    }

    #[test]
    fn a_bogus_date_is_rejected() {
        assert!(parse_date("22/09/2026").is_err());
        assert!(parse_date("2026-09-22T10:00:00Z").is_err());
    }

    #[test]
    fn a_technical_instant_is_rfc_3339_in_utc() {
        let instant: DateTime<Utc> = DateTime::parse_from_rfc3339("2026-09-22T10:00:00.123Z")
            .expect("literal is valid")
            .with_timezone(&Utc);
        assert_eq!(timestamp_to_string(instant), "2026-09-22T10:00:00Z");
    }
}
