//! Validacion compartida por los casos de uso de prestamos.
//!
//! Todo lo que llega del cliente entra como texto y sale como value object del
//! dominio, y cada fallo se acumula en [`ValidationErrors`] con el nombre del campo
//! tal y como viaja en el JSON. Asi un formulario con tres errores responde los
//! tres de una vez, que es lo que el diseno necesita para marcar los campos.

use chrono::NaiveDate;
use gastonomo_domain::loan::{LoanDirection, MAX_LOAN_NOTE_LENGTH, MAX_PAYMENT_LABEL_LENGTH};
use gastonomo_domain::{CurrencyCode, MonetaryAmount, Money, PersonName};

use crate::error::ValidationErrors;
use crate::use_case::validation_message::{monetary_amount_field, monetary_amount_message};

/// Formato de las fechas de registro, el mismo que usa el contrato HTTP.
const RECORD_DATE_FORMAT: &str = "%Y-%m-%d";

/// Los cuatro campos de importe tal y como llegan del cliente.
///
/// Van juntos a proposito: la API guarda lo que el usuario tecleo y su
/// equivalencia en la moneda principal, y ninguno de los dos pares tiene sentido
/// sin el otro miembro de su par.
///
/// `base` y `base_currency` se pueden omitir los dos juntos, igual que en un
/// movimiento: es el caso del usuario que teclea en su moneda principal, y entonces
/// la equivalencia es el propio importe. Mandar uno solo de los dos es un error.
#[derive(Debug, Clone, Copy)]
pub struct MoneyFields<'a> {
    /// Importe tecleado, como texto decimal.
    pub amount: &'a str,
    /// Moneda del importe tecleado.
    pub currency: &'a str,
    /// Equivalencia en la moneda principal del momento, si viaja.
    pub base: Option<&'a str>,
    /// Moneda de la equivalencia, si viaja.
    pub base_currency: Option<&'a str>,
}

/// Convierte los campos de importe en un importe de dominio.
pub(super) fn money(
    fields: MoneyFields<'_>,
    errors: &mut ValidationErrors,
) -> Option<MonetaryAmount> {
    let amount = parse_decimal(Some(fields.amount), "amount", errors);
    let currency = parse_currency(Some(fields.currency), "currency", errors);
    let base = parse_decimal(fields.base, "base", errors);
    let base_currency = parse_currency(fields.base_currency, "baseCurrency", errors);
    if fields.base.is_some() != fields.base_currency.is_some() {
        errors.push("base", "La equivalencia necesita importe y moneda.");
        return None;
    }
    let (Some(amount), Some(currency)) = (amount, currency) else {
        return None;
    };
    build(amount, currency, base, base_currency, errors)
}

/// Monta el par de importes, que es donde vive la invariante del diseno.
fn build(
    amount: Money,
    currency: CurrencyCode,
    base: Option<Money>,
    base_currency: Option<CurrencyCode>,
    errors: &mut ValidationErrors,
) -> Option<MonetaryAmount> {
    let built = match (base, base_currency) {
        (Some(base), Some(base_currency)) => {
            MonetaryAmount::new(amount, currency, base, base_currency)
        }
        _ => MonetaryAmount::single_currency(amount, currency),
    };
    match built {
        Ok(value) => Some(value),
        Err(error) => {
            errors.push(
                monetary_amount_field(&error),
                monetary_amount_message(&error),
            );
            None
        }
    }
}

fn parse_decimal(raw: Option<&str>, field: &str, errors: &mut ValidationErrors) -> Option<Money> {
    Money::parse(raw?)
        .inspect_err(|_| errors.push(field, "El importe no es un numero."))
        .ok()
}

fn parse_currency(
    raw: Option<&str>,
    field: &str,
    errors: &mut ValidationErrors,
) -> Option<CurrencyCode> {
    CurrencyCode::parse(raw?)
        .inspect_err(|_| errors.push(field, "La moneda tiene que ser un codigo ISO 4217."))
        .ok()
}

/// Lee una fecha de registro `YYYY-MM-DD`.
pub(super) fn date(raw: &str, field: &str, errors: &mut ValidationErrors) -> Option<NaiveDate> {
    if let Ok(value) = NaiveDate::parse_from_str(raw.trim(), RECORD_DATE_FORMAT) {
        return Some(value);
    }
    errors.push(field, "La fecha tiene que ser YYYY-MM-DD.");
    None
}

/// Lee el nombre de la persona del prestamo.
pub(super) fn person(raw: &str, errors: &mut ValidationErrors) -> Option<PersonName> {
    PersonName::parse(raw)
        .inspect_err(|_| errors.push("person", "El nombre de la persona es obligatorio."))
        .ok()
}

/// Recorta la nota del prestamo, que puede venir vacia.
pub(super) fn note(raw: &str, errors: &mut ValidationErrors) -> Option<String> {
    let trimmed = raw.trim();
    if trimmed.chars().count() > MAX_LOAN_NOTE_LENGTH {
        errors.push(
            "note",
            format!("La nota admite {MAX_LOAN_NOTE_LENGTH} caracteres."),
        );
        return None;
    }
    Some(trimmed.to_owned())
}

/// Recorta el concepto del abono.
///
/// Devuelve `None` tanto si no hay etiqueta como si es demasiado larga: el caso
/// del fallo ya queda anotado en `errors`, y el que valida corta antes de leer el
/// resultado en cuanto haya un solo error.
pub(super) fn label(raw: Option<&str>, errors: &mut ValidationErrors) -> Option<String> {
    let trimmed = raw.map(str::trim).filter(|value| !value.is_empty())?;
    if trimmed.chars().count() > MAX_PAYMENT_LABEL_LENGTH {
        errors.push(
            "label",
            format!("El concepto admite {MAX_PAYMENT_LABEL_LENGTH} caracteres."),
        );
        return None;
    }
    Some(trimmed.to_owned())
}

/// Lee la direccion del prestamo.
pub(super) fn direction(raw: &str, errors: &mut ValidationErrors) -> Option<LoanDirection> {
    if let Ok(value) = LoanDirection::parse(raw) {
        return Some(value);
    }
    errors.push("direction", "El tipo tiene que ser `lent` o `borrowed`.");
    None
}
