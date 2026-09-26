//! DTOs del contexto de monedas y tasas.
//!
//! Convenciones en [`crate::serialization`]: camelCase, fechas de registro
//! `YYYY-MM-DD` e instantes tecnicos en RFC 3339. Las tasas viajan como **string**
//! por el mismo motivo que los importes: un `0.87732` sin comillas acaba en un
//! `double` en cuanto el cliente lo parsea.

use std::collections::BTreeMap;

use gastonomo_application::use_case::currency::ExchangeRatesView;
use gastonomo_domain::currency::Currency;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use crate::serialization::{date_to_string, timestamp_to_string};

/// Una moneda del catalogo.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CurrencyDto {
    /// Codigo ISO 4217.
    #[schema(example = "COP")]
    pub code: String,
    /// Nombre en ingles, tal y como lo publica la fuente.
    #[schema(example = "Colombian Peso")]
    pub name: String,
    /// Simbolo para pintar delante del importe. Nunca vacio: si la fuente no da
    /// ninguno, es el propio codigo.
    #[schema(example = "$")]
    pub symbol: String,
}

impl From<&Currency> for CurrencyDto {
    fn from(currency: &Currency) -> Self {
        Self {
            code: currency.code().to_string(),
            name: currency.name().to_owned(),
            symbol: currency.symbol().to_owned(),
        }
    }
}

/// Respuesta de `GET /v1/currencies`.
///
/// Trae solo las monedas que se pueden elegir: fuera los metales y los XDR, fuera
/// las que no tienen tasa. Ese filtro lo hace el servidor una vez y no cada
/// cliente.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CurrencyListDto {
    /// Monedas ordenadas por codigo.
    pub items: Vec<CurrencyDto>,
}

impl CurrencyListDto {
    /// Monta la respuesta desde el catalogo del caso de uso.
    #[must_use]
    pub fn from_catalog(currencies: &[Currency]) -> Self {
        Self {
            items: currencies.iter().map(CurrencyDto::from).collect(),
        }
    }
}

/// Respuesta de `GET /v1/exchange-rates`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ExchangeRatesDto {
    /// Moneda contra la que estan expresadas todas las tasas.
    #[schema(example = "USD")]
    pub base: String,
    /// Fecha de publicacion de las tasas. Es la que Ajustes muestra en
    /// "Tasas de cambio: Frankfurter · {fecha}".
    #[schema(example = "2026-09-25")]
    pub date: String,
    /// Instante del ultimo refresco con exito, en RFC 3339 UTC.
    #[schema(example = "2026-09-25T06:00:12Z")]
    pub fetched_at: String,
    /// `ok`, o `stale` si el ultimo refresco con exito ya tiene dos dias. Con
    /// `stale` el cliente pinta "Sin conexion" y sigue convirtiendo.
    #[schema(example = "ok")]
    pub status: String,
    /// Tasa por moneda: cuantas unidades de esa moneda da una de la base.
    #[schema(example = json!({"USD": "1", "COP": "3316.44", "EUR": "0.87732"}))]
    pub rates: BTreeMap<String, String>,
}

impl From<&ExchangeRatesView> for ExchangeRatesDto {
    fn from(view: &ExchangeRatesView) -> Self {
        Self {
            base: view.table.base().to_string(),
            date: date_to_string(view.table.date()),
            fetched_at: timestamp_to_string(view.table.fetched_at()),
            status: view.status.as_str().to_owned(),
            rates: view
                .table
                .rates()
                .iter()
                .map(|(code, rate)| (code.to_string(), rate.to_string()))
                .collect(),
        }
    }
}
