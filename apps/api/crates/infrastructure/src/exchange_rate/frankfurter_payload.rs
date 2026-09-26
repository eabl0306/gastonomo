//! Lectura de la respuesta de Frankfurter.
//!
//! El numero de una tasa se lee como el texto literal del JSON
//! (`serde_json::value::RawValue`) y no como un `f64`: `serde_json` parsea los
//! numeros a coma flotante salvo que se le pida el valor en crudo, y una app de
//! finanzas no puede permitirse ese paso intermedio.

use std::collections::BTreeMap;

use chrono::NaiveDate;
use gastonomo_application::error::AppError;
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRate, RateBulletin, rates_base};
use serde::Deserialize;
use serde_json::value::RawValue;

/// Formato de la fecha que publica la fuente.
const SOURCE_DATE_FORMAT: &str = "%Y-%m-%d";

/// Una entrada de `GET /v2/rates?base=usd`.
///
/// Todos los campos son opcionales a proposito. La fuente manda `null` donde
/// documenta texto, y una entrada rara tiene que poder descartarse sola en vez de
/// tumbar la pasada entera.
#[derive(Debug, Deserialize)]
pub struct RatePayload {
    /// Fecha de publicacion de esta tasa.
    #[serde(default)]
    date: Option<String>,
    /// Moneda base de la peticion.
    #[serde(default)]
    base: Option<String>,
    /// Moneda cotizada.
    #[serde(default)]
    quote: Option<String>,
    /// Tasa, tal cual venia en el JSON.
    #[serde(default)]
    rate: Option<Box<RawValue>>,
}

/// Una entrada de `GET /v2/currencies`.
///
/// Opcional por lo mismo, y con un caso real: el dolar COMESA llega con
/// `"symbol": null`.
#[derive(Debug, Deserialize)]
pub struct CurrencyPayload {
    /// Codigo ISO 4217.
    #[serde(default)]
    iso_code: Option<String>,
    /// Nombre en ingles.
    #[serde(default)]
    name: Option<String>,
    /// Simbolo, que la fuente puede dejar vacio o directamente a `null`.
    #[serde(default)]
    symbol: Option<String>,
}

/// Lo que se descarto de la respuesta, para dejarlo en una sola traza.
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
struct Skipped {
    rates: usize,
    currencies: usize,
}

/// Convierte las dos respuestas de la fuente en una pasada de dominio.
///
/// La fecha de la pasada es la mas reciente de todas las entradas: la fuente
/// devuelve monedas que no cotizaron hoy con la fecha del ultimo dia que si lo
/// hicieron, y esas tasas siguen siendo las vigentes.
///
/// Una entrada ilegible se descarta en vez de tumbar la pasada entera: que la
/// fuente cambie una moneda no puede dejar la app sin tasas. Si no queda ninguna,
/// entonces si es un fallo.
///
/// # Errors
///
/// Devuelve [`AppError::Dependency`] si no queda ni una entrada utilizable.
pub fn read_bulletin(
    rates: &[RatePayload],
    currencies: &[CurrencyPayload],
) -> Result<RateBulletin, AppError> {
    let base = rates_base();
    let mut skipped = Skipped::default();
    let mut latest: Option<NaiveDate> = None;
    let mut quoted = BTreeMap::new();
    for payload in rates {
        match read_rate(payload, &base) {
            Some((date, quote, rate)) => {
                latest = Some(latest.map_or(date, |current| current.max(date)));
                quoted.insert(quote, rate);
            }
            None => skipped.rates += 1,
        }
    }
    let catalog = read_catalog(currencies, &mut skipped);
    trace_skipped(skipped);
    let Some(date) = latest else {
        return Err(AppError::dependency(
            "the rates response carried no usable entry",
        ));
    };
    RateBulletin::new(base, date, quoted, catalog)
        .map_err(|error| AppError::dependency(format!("the rates response is unusable: {error}")))
}

/// Lee una tasa, o `None` si la entrada no encaja con el contrato esperado.
fn read_rate(
    payload: &RatePayload,
    base: &CurrencyCode,
) -> Option<(NaiveDate, CurrencyCode, ExchangeRate)> {
    if !payload
        .base
        .as_deref()?
        .trim()
        .eq_ignore_ascii_case(base.as_str())
    {
        return None;
    }
    let date =
        NaiveDate::parse_from_str(payload.date.as_deref()?.trim(), SOURCE_DATE_FORMAT).ok()?;
    let quote = CurrencyCode::parse(payload.quote.as_deref()?).ok()?;
    let rate = ExchangeRate::parse(payload.rate.as_deref()?.get()).ok()?;
    Some((date, quote, rate))
}

/// Lee el catalogo, descartando lo que no se puede ofrecer.
fn read_catalog(currencies: &[CurrencyPayload], skipped: &mut Skipped) -> Vec<Currency> {
    let mut catalog = Vec::with_capacity(currencies.len());
    for payload in currencies {
        match read_currency(payload) {
            Some(currency) => catalog.push(currency),
            None => skipped.currencies += 1,
        }
    }
    catalog
}

/// Lee una moneda del catalogo, o `None` si es un metal, los XDR o viene rota.
fn read_currency(payload: &CurrencyPayload) -> Option<Currency> {
    let code = CurrencyCode::parse(payload.iso_code.as_deref()?).ok()?;
    let name = payload.name.as_deref().unwrap_or_default();
    let symbol = payload.symbol.as_deref().unwrap_or_default();
    Currency::new(code, name, symbol).ok()
}

/// Deja en el log lo que se descarto, si se descarto algo.
fn trace_skipped(skipped: Skipped) {
    if skipped == Skipped::default() {
        return;
    }
    tracing::debug!(
        rates = skipped.rates,
        currencies = skipped.currencies,
        "the exchange rate source sent entries that are not offered or not readable"
    );
}

#[cfg(test)]
mod tests;
