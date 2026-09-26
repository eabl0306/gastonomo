//! Configuracion de la tarea de refresco de tasas.

use std::time::Duration;

use serde::{Deserialize, Serialize};

/// Horas UTC a las que se refresca por defecto.
///
/// La fuente publica una vez al dia sobre las 16:00 CET con las referencias del
/// BCE, asi que dos pasadas bastan de sobra y dejan margen si una falla.
const DEFAULT_REFRESH_HOURS: &str = "6,18";

/// Configuracion del refresco de tasas de cambio.
///
/// Las horas viajan como texto (`"6,18"`) y no como lista porque el proveedor de
/// entorno de figment entrega cadenas: una lista obligaria al operador a escribir
/// la sintaxis de un array en una variable de entorno.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct ExchangeRatesConfig {
    /// Si la tarea se arranca. Apagada en los tests y en cualquier proceso que no
    /// deba salir a internet.
    pub enabled: bool,
    /// Raiz de la API de la fuente, sin barra final.
    pub base_url: String,
    /// Horas UTC del refresco, separadas por comas.
    pub refresh_hours_utc: String,
    /// Tiempo maximo de cada peticion a la fuente, en milisegundos.
    pub request_timeout_ms: u64,
    /// Intentos por pasada, contando el primero.
    pub retry_attempts: u32,
    /// Espera antes del segundo intento, en milisegundos. Se duplica en cada
    /// intento siguiente.
    pub retry_backoff_ms: u64,
}

impl Default for ExchangeRatesConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            base_url: "https://api.frankfurter.dev/v2".to_owned(),
            refresh_hours_utc: DEFAULT_REFRESH_HOURS.to_owned(),
            request_timeout_ms: 10_000,
            retry_attempts: 3,
            retry_backoff_ms: 2_000,
        }
    }
}

impl ExchangeRatesConfig {
    /// Horas UTC del refresco, ordenadas y sin repetidos.
    ///
    /// Lo que no sea una hora valida se ignora. Si no queda ninguna se cae a las
    /// dos por defecto: una variable mal escrita no puede dejar la app sin tasas.
    #[must_use]
    pub fn refresh_hours(&self) -> Vec<u32> {
        let mut hours: Vec<u32> = self
            .refresh_hours_utc
            .split(',')
            .filter_map(|raw| raw.trim().parse::<u32>().ok())
            .filter(|hour| *hour < 24)
            .collect();
        hours.sort_unstable();
        hours.dedup();
        if hours.is_empty() {
            return Self::default().refresh_hours();
        }
        hours
    }

    /// Tiempo maximo de una peticion a la fuente.
    #[must_use]
    pub const fn request_timeout(&self) -> Duration {
        Duration::from_millis(self.request_timeout_ms)
    }

    /// Intentos por pasada, nunca menos de uno.
    #[must_use]
    pub const fn attempts(&self) -> u32 {
        if self.retry_attempts == 0 {
            1
        } else {
            self.retry_attempts
        }
    }

    /// Espera antes del intento numero `attempt`, contando desde uno.
    ///
    /// Crece al doble en cada intento: 2 s, 4 s, 8 s con el valor por defecto.
    #[must_use]
    pub fn backoff_before(&self, attempt: u32) -> Duration {
        let factor = 1_u64 << attempt.saturating_sub(1).min(16);
        Duration::from_millis(self.retry_backoff_ms.saturating_mul(factor))
    }

    /// URL de las tasas con la base dada.
    #[must_use]
    pub fn rates_url(&self, base: &str) -> String {
        format!(
            "{}/rates?base={}",
            self.base_url.trim_end_matches('/'),
            base.to_lowercase()
        )
    }

    /// URL del catalogo de monedas.
    #[must_use]
    pub fn currencies_url(&self) -> String {
        format!("{}/currencies", self.base_url.trim_end_matches('/'))
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use std::time::Duration;

    use super::ExchangeRatesConfig;

    #[test]
    fn the_default_task_refreshes_twice_a_day() {
        let config = ExchangeRatesConfig::default();
        assert_eq!(config.refresh_hours(), [6, 18]);
        assert!(config.enabled);
        assert_eq!(config.request_timeout(), Duration::from_secs(10));
    }

    #[test]
    fn the_hours_are_sorted_and_deduplicated() {
        let config = ExchangeRatesConfig {
            refresh_hours_utc: " 18 , 6, 6 ,18".to_owned(),
            ..ExchangeRatesConfig::default()
        };
        assert_eq!(config.refresh_hours(), [6, 18]);
    }

    #[test]
    fn nonsense_hours_fall_back_to_the_defaults() {
        let config = ExchangeRatesConfig {
            refresh_hours_utc: "midnight, 25, -1".to_owned(),
            ..ExchangeRatesConfig::default()
        };
        assert_eq!(config.refresh_hours(), [6, 18]);
    }

    #[test]
    fn the_wait_doubles_on_every_attempt() {
        let config = ExchangeRatesConfig::default();
        assert_eq!(config.attempts(), 3);
        assert_eq!(config.backoff_before(1), Duration::from_secs(2));
        assert_eq!(config.backoff_before(2), Duration::from_secs(4));
        assert_eq!(config.backoff_before(3), Duration::from_secs(8));
    }

    #[test]
    fn zero_attempts_still_means_one_try() {
        let config = ExchangeRatesConfig {
            retry_attempts: 0,
            ..ExchangeRatesConfig::default()
        };
        assert_eq!(config.attempts(), 1);
    }

    #[test]
    fn the_urls_follow_the_documented_source() {
        let config = ExchangeRatesConfig {
            base_url: "https://api.frankfurter.dev/v2/".to_owned(),
            ..ExchangeRatesConfig::default()
        };
        assert_eq!(
            config.rates_url("USD"),
            "https://api.frankfurter.dev/v2/rates?base=usd"
        );
        assert_eq!(
            config.currencies_url(),
            "https://api.frankfurter.dev/v2/currencies"
        );
    }
}
