//! Una pasada completa de la fuente de tasas.

use std::collections::BTreeMap;

use chrono::{DateTime, NaiveDate, Utc};
use thiserror::Error;

use super::catalog::{Currency, is_excluded};
use super::exchange_rate::ExchangeRate;
use super::rate_table::ExchangeRateTable;
use crate::value_object::CurrencyCode;

/// Minimo de tasas que hace creible una pasada.
///
/// La base mas una moneda cotizada. Por debajo de eso la respuesta de la fuente
/// esta vacia o rota, y guardarla dejaria de ofrecer todas las monedas.
const MIN_RATES: usize = 2;

/// Motivo por el que una pasada no se puede aceptar.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum BulletinError {
    /// La pasada no trae tasas suficientes para ser creible.
    #[error("the bulletin only carries {got} usable rates")]
    TooFewRates {
        /// Tasas utiles que quedaron tras el filtro.
        got: usize,
    },
}

/// Tasas y catalogo tal y como quedan tras una pasada de la fuente.
///
/// Aqui se aplica el filtro que el diseno pide una sola vez, en el servidor:
/// fuera los metales y los XDR, y fuera las monedas sin tasa. Asi el catalogo que
/// se guarda y las tasas que se sirven no pueden discrepar.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RateBulletin {
    base: CurrencyCode,
    date: NaiveDate,
    rates: BTreeMap<CurrencyCode, ExchangeRate>,
    currencies: Vec<Currency>,
}

impl RateBulletin {
    /// Monta la pasada, descartando lo que no se ofrece.
    ///
    /// La base entra siempre con tasa 1, y del catalogo solo sobreviven las
    /// monedas que tienen tasa.
    ///
    /// # Errors
    ///
    /// Devuelve [`BulletinError::TooFewRates`] si tras el filtro no queda ni la
    /// base con una moneda cotizada.
    pub fn new(
        base: CurrencyCode,
        date: NaiveDate,
        rates: BTreeMap<CurrencyCode, ExchangeRate>,
        currencies: Vec<Currency>,
    ) -> Result<Self, BulletinError> {
        let mut rates: BTreeMap<CurrencyCode, ExchangeRate> = rates
            .into_iter()
            .filter(|(code, _)| !is_excluded(code))
            .collect();
        rates.insert(base.clone(), ExchangeRate::ONE);
        if rates.len() < MIN_RATES {
            return Err(BulletinError::TooFewRates { got: rates.len() });
        }
        let mut currencies: Vec<Currency> = currencies
            .into_iter()
            .filter(|currency| rates.contains_key(currency.code()))
            .collect();
        currencies.sort_by(|left, right| left.code().cmp(right.code()));
        currencies.dedup_by(|left, right| left.code() == right.code());
        Ok(Self {
            base,
            date,
            rates,
            currencies,
        })
    }

    /// Moneda contra la que estan expresadas las tasas.
    #[must_use]
    pub const fn base(&self) -> &CurrencyCode {
        &self.base
    }

    /// Fecha de publicacion mas reciente de la pasada.
    #[must_use]
    pub const fn date(&self) -> NaiveDate {
        self.date
    }

    /// Tasas que se guardan, ordenadas por codigo.
    #[must_use]
    pub const fn rates(&self) -> &BTreeMap<CurrencyCode, ExchangeRate> {
        &self.rates
    }

    /// Monedas que quedan ofrecidas, ordenadas por codigo.
    #[must_use]
    pub fn currencies(&self) -> &[Currency] {
        &self.currencies
    }

    /// Convierte la pasada en la tabla que se sirve.
    #[must_use]
    pub fn into_table(self, fetched_at: DateTime<Utc>) -> ExchangeRateTable {
        ExchangeRateTable::new(self.base, self.date, fetched_at, self.rates)
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use std::collections::BTreeMap;

    use chrono::NaiveDate;

    use super::{BulletinError, RateBulletin};
    use crate::currency::{Currency, ExchangeRate};
    use crate::value_object::CurrencyCode;

    fn code(raw: &str) -> CurrencyCode {
        CurrencyCode::parse(raw).expect("literal is a valid code")
    }

    fn rate(raw: &str) -> ExchangeRate {
        ExchangeRate::parse(raw).expect("literal is a valid rate")
    }

    fn date() -> NaiveDate {
        NaiveDate::from_ymd_opt(2026, 9, 25).expect("literal is valid")
    }

    fn currency(raw: &str, name: &str) -> Currency {
        Currency::new(code(raw), name, "$").expect("literal is a valid currency")
    }

    fn rates() -> BTreeMap<CurrencyCode, ExchangeRate> {
        BTreeMap::from([
            (code("COP"), rate("3316.44")),
            (code("EUR"), rate("0.87732")),
            (code("XAU"), rate("0.00023")),
        ])
    }

    fn bulletin() -> RateBulletin {
        RateBulletin::new(
            code("USD"),
            date(),
            rates(),
            vec![
                currency("COP", "Colombian Peso"),
                currency("EUR", "Euro"),
                currency("USD", "United States Dollar"),
                currency("JPY", "Japanese Yen"),
            ],
        )
        .expect("should build")
    }

    #[test]
    fn gold_never_reaches_the_stored_rates() {
        assert!(!bulletin().rates().contains_key(&code("XAU")));
    }

    #[test]
    fn the_base_is_added_with_a_rate_of_one() {
        let bulletin = bulletin();
        assert_eq!(bulletin.rates().get(&code("USD")), Some(&ExchangeRate::ONE));
        assert_eq!(bulletin.base().as_str(), "USD");
        assert_eq!(bulletin.date(), date());
    }

    #[test]
    fn a_currency_without_a_rate_is_not_offered() {
        let bulletin = bulletin();
        let offered: Vec<&str> = bulletin
            .currencies()
            .iter()
            .map(|currency| currency.code().as_str())
            .collect();
        assert_eq!(offered, ["COP", "EUR", "USD"]);
    }

    #[test]
    fn an_empty_response_is_rejected_instead_of_wiping_the_catalog() {
        assert_eq!(
            RateBulletin::new(code("USD"), date(), BTreeMap::new(), Vec::new()),
            Err(BulletinError::TooFewRates { got: 1 })
        );
    }

    #[test]
    fn the_table_it_becomes_carries_the_same_rates() {
        let fetched_at = "2026-09-25T06:00:00Z"
            .parse()
            .expect("literal is a valid instant");
        let table = bulletin().into_table(fetched_at);
        assert_eq!(table.rates().len(), 3);
        assert_eq!(table.fetched_at(), fetched_at);
        assert_eq!(table.date(), date());
    }
}
