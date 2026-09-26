//! Tabla de tasas vigente y su frescura.

use std::collections::BTreeMap;

use chrono::{DateTime, Duration, NaiveDate, Utc};

use super::exchange_rate::ExchangeRate;
use crate::value_object::CurrencyCode;

/// Moneda contra la que la API guarda todas las tasas.
///
/// La fuente publica con base USD y aqui se guarda tal cual: convertir la base al
/// guardar meteria un redondeo que despues nadie podria deshacer.
pub const RATES_BASE: &str = "USD";

/// Moneda base como value object.
///
/// # Panics
///
/// No entra en panico: [`RATES_BASE`] son tres letras y la rama imposible se
/// cierra con `unreachable!`.
#[must_use]
pub fn rates_base() -> CurrencyCode {
    let Ok(base) = CurrencyCode::parse(RATES_BASE) else {
        unreachable!("the rates base is a valid iso 4217 code")
    };
    base
}

/// Horas tras las que la tabla se sirve marcada como rancia.
///
/// Cuarenta y ocho: la fuente publica una vez al dia en dia laborable y la tarea
/// refresca dos veces al dia, asi que pasar de 48 horas solo ocurre si los
/// refrescos llevan fallando. Es lo que el cliente pinta como "Sin conexion".
pub const STALE_AFTER_HOURS: i64 = 48;

/// Horas tras las que conviene refrescar al arrancar el proceso.
///
/// Veinticuatro: un despliegue en limpio no puede quedarse esperando a las 06:00
/// sin poder convertir nada.
pub const REFRESH_AFTER_HOURS: i64 = 24;

/// Frescura de las tasas que se sirven.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RateStatus {
    /// El ultimo refresco fue reciente.
    Ok,
    /// Los datos tienen mas de [`STALE_AFTER_HOURS`] horas.
    ///
    /// Se sirven igualmente: unas tasas de anteayer convierten mejor que ninguna.
    Stale,
}

impl RateStatus {
    /// Valor que viaja en el JSON.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Ok => "ok",
            Self::Stale => "stale",
        }
    }
}

/// Tasas vigentes de una base, tal y como las sirve `GET /v1/exchange-rates`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExchangeRateTable {
    base: CurrencyCode,
    date: NaiveDate,
    fetched_at: DateTime<Utc>,
    rates: BTreeMap<CurrencyCode, ExchangeRate>,
}

impl ExchangeRateTable {
    /// Monta la tabla.
    ///
    /// La base siempre queda con tasa 1, la anade la fuente o no: el cliente
    /// convierte con `n * rates[b] / rates[a]` y sin esa entrada la moneda base
    /// no se podria usar como origen ni como destino.
    #[must_use]
    pub fn new(
        base: CurrencyCode,
        date: NaiveDate,
        fetched_at: DateTime<Utc>,
        rates: BTreeMap<CurrencyCode, ExchangeRate>,
    ) -> Self {
        let mut rates = rates;
        rates.insert(base.clone(), ExchangeRate::ONE);
        Self {
            base,
            date,
            fetched_at,
            rates,
        }
    }

    /// Moneda contra la que estan expresadas todas las tasas.
    #[must_use]
    pub const fn base(&self) -> &CurrencyCode {
        &self.base
    }

    /// Fecha de publicacion mas reciente de las tasas.
    ///
    /// Es la que Ajustes muestra en "Tasas de cambio: Frankfurter · {fecha}".
    #[must_use]
    pub const fn date(&self) -> NaiveDate {
        self.date
    }

    /// Instante del ultimo refresco con exito.
    #[must_use]
    pub const fn fetched_at(&self) -> DateTime<Utc> {
        self.fetched_at
    }

    /// Tasas por moneda, ordenadas por codigo.
    #[must_use]
    pub const fn rates(&self) -> &BTreeMap<CurrencyCode, ExchangeRate> {
        &self.rates
    }

    /// Tasa de una moneda concreta.
    #[must_use]
    pub fn rate(&self, currency: &CurrencyCode) -> Option<ExchangeRate> {
        self.rates.get(currency).copied()
    }

    /// Frescura de la tabla en un instante dado.
    #[must_use]
    pub fn status(&self, now: DateTime<Utc>) -> RateStatus {
        if self.age_exceeds(now, STALE_AFTER_HOURS) {
            RateStatus::Stale
        } else {
            RateStatus::Ok
        }
    }

    /// Indica si conviene refrescar ya, sin esperar a la proxima pasada.
    #[must_use]
    pub fn is_due_for_refresh(&self, now: DateTime<Utc>) -> bool {
        self.age_exceeds(now, REFRESH_AFTER_HOURS)
    }

    fn age_exceeds(&self, now: DateTime<Utc>, hours: i64) -> bool {
        now.signed_duration_since(self.fetched_at) > Duration::hours(hours)
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

    use chrono::{DateTime, Duration, NaiveDate, Utc};

    use super::{ExchangeRateTable, RateStatus};
    use crate::currency::ExchangeRate;
    use crate::value_object::CurrencyCode;

    fn code(raw: &str) -> CurrencyCode {
        CurrencyCode::parse(raw).expect("literal is a valid code")
    }

    fn instant() -> DateTime<Utc> {
        DateTime::parse_from_rfc3339("2026-09-22T06:00:00Z")
            .expect("literal is valid")
            .with_timezone(&Utc)
    }

    fn table() -> ExchangeRateTable {
        let mut rates = BTreeMap::new();
        rates.insert(code("COP"), ExchangeRate::parse("3316.44").expect("valid"));
        rates.insert(code("EUR"), ExchangeRate::parse("0.87732").expect("valid"));
        ExchangeRateTable::new(
            code("USD"),
            NaiveDate::from_ymd_opt(2026, 9, 22).expect("literal is valid"),
            instant(),
            rates,
        )
    }

    #[test]
    fn the_rates_base_is_the_dollar() {
        assert_eq!(super::rates_base().as_str(), super::RATES_BASE);
        assert_eq!(super::RATES_BASE, "USD");
    }

    #[test]
    fn the_base_always_has_a_rate_of_one() {
        let table = table();
        assert_eq!(table.rate(&code("USD")), Some(ExchangeRate::ONE));
        assert_eq!(table.base().as_str(), "USD");
        assert_eq!(table.rates().len(), 3);
    }

    #[test]
    fn the_quoted_rates_survive_the_table() {
        let table = table();
        assert_eq!(
            table.rate(&code("COP")).map(|rate| rate.to_string()),
            Some("3316.44".to_owned())
        );
        assert_eq!(table.rate(&code("JPY")), None);
    }

    #[test]
    fn fresh_rates_are_served_as_ok() {
        let table = table();
        assert_eq!(table.status(instant()), RateStatus::Ok);
        assert_eq!(
            table.status(instant() + Duration::hours(47)),
            RateStatus::Ok
        );
        assert_eq!(table.date().to_string(), "2026-09-22");
        assert_eq!(table.fetched_at(), instant());
    }

    #[test]
    fn rates_older_than_two_days_are_served_as_stale() {
        let table = table();
        assert_eq!(
            table.status(instant() + Duration::hours(49)),
            RateStatus::Stale
        );
        assert_eq!(RateStatus::Stale.as_str(), "stale");
        assert_eq!(RateStatus::Ok.as_str(), "ok");
    }

    #[test]
    fn a_refresh_is_due_only_after_a_full_day() {
        let table = table();
        assert!(!table.is_due_for_refresh(instant() + Duration::hours(23)));
        assert!(table.is_due_for_refresh(instant() + Duration::hours(25)));
    }
}
