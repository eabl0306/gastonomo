//! Adaptador driven: tasas de cambio desde Frankfurter.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::currency::ExchangeRateProvider;
use gastonomo_domain::currency::{RATES_BASE, RateBulletin};
use serde::de::DeserializeOwned;
use thiserror::Error;

use super::frankfurter_payload::{CurrencyPayload, RatePayload, read_bulletin};
use crate::config::ExchangeRatesConfig;

/// Identificacion que la API manda a la fuente.
const USER_AGENT: &str = concat!("gastonomo-api/", env!("CARGO_PKG_VERSION"));

/// Fallo al construir el cliente de la fuente.
#[derive(Debug, Error)]
pub enum ProviderError {
    /// El cliente HTTP no se pudo construir.
    #[error("could not build the http client for the exchange rate source: {0}")]
    Client(#[source] Box<reqwest::Error>),
}

/// Lee las tasas publicadas por Frankfurter.
///
/// Es el unico sitio de la aplicacion que sale a internet a por tasas: los
/// clientes preguntan siempre a `/v1/exchange-rates`.
#[derive(Debug, Clone)]
pub struct FrankfurterExchangeRateProvider {
    http: reqwest::Client,
    config: ExchangeRatesConfig,
}

impl FrankfurterExchangeRateProvider {
    /// Crea el proveedor con el tiempo maximo de peticion de la configuracion.
    ///
    /// # Errors
    ///
    /// Devuelve [`ProviderError::Client`] si el cliente HTTP no se puede construir.
    pub fn new(config: ExchangeRatesConfig) -> Result<Self, ProviderError> {
        let http = reqwest::Client::builder()
            .timeout(config.request_timeout())
            .user_agent(USER_AGENT)
            .build()
            .map_err(|error| ProviderError::Client(Box::new(error)))?;
        Ok(Self { http, config })
    }

    /// Pide un documento JSON y lo interpreta.
    async fn get<T: DeserializeOwned>(&self, url: &str) -> Result<T, AppError> {
        let response = self.http.get(url).send().await.map_err(|error| {
            AppError::dependency(format!("the exchange rate source is unreachable: {error}"))
        })?;
        let status = response.status();
        if !status.is_success() {
            return Err(AppError::dependency(format!(
                "the exchange rate source answered {status}"
            )));
        }
        response.json::<T>().await.map_err(|error| {
            AppError::dependency(format!(
                "the exchange rate source answered something unreadable: {error}"
            ))
        })
    }
}

#[async_trait]
impl ExchangeRateProvider for FrankfurterExchangeRateProvider {
    async fn fetch(&self) -> Result<RateBulletin, AppError> {
        let rates: Vec<RatePayload> = self.get(&self.config.rates_url(RATES_BASE)).await?;
        let currencies: Vec<CurrencyPayload> = self.get(&self.config.currencies_url()).await?;
        read_bulletin(&rates, &currencies)
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_application::port::currency::ExchangeRateProvider as _;

    use super::{FrankfurterExchangeRateProvider, USER_AGENT};
    use crate::config::ExchangeRatesConfig;

    #[test]
    fn the_client_identifies_itself_with_the_service_version() {
        assert!(USER_AGENT.starts_with("gastonomo-api/"));
    }

    #[tokio::test]
    async fn a_source_that_does_not_answer_is_a_dependency_failure() {
        // El puerto 9 es el descarte de TCP: nadie escucha y la conexion se cae
        // enseguida, asi que este test no sale a internet ni espera al timeout.
        let config = ExchangeRatesConfig {
            base_url: "http://127.0.0.1:9".to_owned(),
            request_timeout_ms: 300,
            ..ExchangeRatesConfig::default()
        };
        let provider =
            FrankfurterExchangeRateProvider::new(config).expect("the client should build");
        assert!(provider.fetch().await.is_err());
    }
}
