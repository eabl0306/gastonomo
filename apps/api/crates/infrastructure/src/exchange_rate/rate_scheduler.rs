//! Adaptador driving: la tarea que refresca las tasas.
//!
//! Es un bucle de tokio, no un caso de uso: decide cuando se refresca y cuantas
//! veces se reintenta, y para todo lo demas llama a `RefreshExchangeRates`. Asi el
//! caso de uso se prueba con un proveedor falso y esta pieza solo tiene que
//! acertar con el reloj.

use std::sync::Arc;
use std::time::Duration;

use chrono::Utc;
use gastonomo_application::use_case::currency::RefreshExchangeRates;
use tokio::task::JoinHandle;

use super::schedule::next_tick_after;
use crate::config::ExchangeRatesConfig;

/// Espera de reserva si el calendario no devuelve una hora, que la configuracion
/// ya impide.
const FALLBACK_SLEEP: Duration = Duration::from_hours(1);

/// Refresca las tasas al arrancar y despues a sus horas.
#[derive(Debug, Clone)]
pub struct ExchangeRateScheduler {
    refresh: Arc<RefreshExchangeRates>,
    config: ExchangeRatesConfig,
}

impl ExchangeRateScheduler {
    /// Crea el planificador.
    #[must_use]
    pub const fn new(refresh: Arc<RefreshExchangeRates>, config: ExchangeRatesConfig) -> Self {
        Self { refresh, config }
    }

    /// Arranca el bucle en una tarea de tokio.
    ///
    /// Devuelve `None` si la tarea esta desactivada por configuracion, que es lo
    /// que hacen los tests y cualquier proceso que no deba salir a internet.
    /// Soltar el `JoinHandle` no para la tarea: vive lo que viva el proceso.
    #[must_use]
    pub fn spawn(self) -> Option<JoinHandle<()>> {
        if !self.config.enabled {
            log::disabled();
            return None;
        }
        Some(tokio::spawn(self.run()))
    }

    /// Bucle del planificador.
    async fn run(self) {
        self.catch_up().await;
        let hours = self.config.refresh_hours();
        log::scheduled(&hours);
        loop {
            self.sleep_until_next_tick(&hours).await;
            self.refresh_with_retries(1).await;
        }
    }

    /// Refresco de arranque: solo si no hay tasas o las que hay ya tienen un dia.
    async fn catch_up(&self) {
        match self.refresh.execute_if_due().await {
            Ok(Some(report)) => log::refreshed(&report),
            Ok(None) => log::already_fresh(),
            Err(error) => {
                log::attempt_failed(1, &error);
                self.refresh_with_retries(2).await;
            }
        }
    }

    /// Duerme hasta la siguiente hora del calendario.
    async fn sleep_until_next_tick(&self, hours: &[u32]) {
        let now = Utc::now();
        let wait = next_tick_after(now, hours)
            .and_then(|next| next.signed_duration_since(now).to_std().ok())
            .unwrap_or(FALLBACK_SLEEP);
        log::sleeping(wait);
        tokio::time::sleep(wait).await;
    }

    /// Refresca reintentando con espera creciente.
    ///
    /// `first_attempt` es el numero del intento con el que se entra, para que el
    /// refresco de arranque pueda contar el suyo como el primero.
    async fn refresh_with_retries(&self, first_attempt: u32) {
        let attempts = self.config.attempts();
        for attempt in first_attempt..=attempts {
            if attempt > 1 {
                tokio::time::sleep(self.config.backoff_before(attempt - 1)).await;
            }
            match self.refresh.execute().await {
                Ok(report) => {
                    log::refreshed(&report);
                    return;
                }
                Err(error) => log::attempt_failed(attempt, &error),
            }
        }
        log::gave_up(attempts);
    }
}

/// Trazas del planificador.
///
/// Cada funcion envuelve una sola macro de `tracing`: las macros expanden a
/// bastante codigo y disparan el limite de complejidad cognitiva si se mezclan con
/// la logica del bucle.
mod log {
    use std::time::Duration;

    use gastonomo_application::error::AppError;
    use gastonomo_application::use_case::currency::RefreshReport;

    pub fn disabled() {
        tracing::info!("the exchange rate refresh task is disabled by configuration");
    }

    pub fn scheduled(hours: &[u32]) {
        tracing::info!(?hours, "the exchange rate refresh task is scheduled");
    }

    pub fn sleeping(wait: Duration) {
        tracing::debug!(
            seconds = wait.as_secs(),
            "waiting for the next exchange rate refresh"
        );
    }

    pub fn already_fresh() {
        tracing::info!("the stored exchange rates are recent enough; skipping the startup refresh");
    }

    pub fn refreshed(report: &RefreshReport) {
        tracing::info!(
            date = %report.date,
            currencies = report.currencies,
            outcome = ?report.outcome,
            "exchange rates refreshed"
        );
    }

    pub fn attempt_failed(attempt: u32, error: &AppError) {
        tracing::warn!(attempt, %error, "an exchange rate refresh attempt failed");
    }

    pub fn gave_up(attempts: u32) {
        tracing::error!(
            attempts,
            "could not refresh the exchange rates; keeping the stored ones, which will be \
             served as stale once they are two days old"
        );
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use std::sync::Arc;

    use chrono::NaiveDate;
    use gastonomo_application::testing::{
        InMemoryExchangeRateRepository, StubExchangeRateProvider,
    };
    use gastonomo_application::use_case::currency::RefreshExchangeRates;
    use gastonomo_domain::CurrencyCode;
    use gastonomo_domain::currency::{Currency, ExchangeRate, RateBulletin};

    use super::ExchangeRateScheduler;
    use crate::clock::SystemClock;
    use crate::config::ExchangeRatesConfig;

    fn bulletin() -> RateBulletin {
        let code = |raw: &str| CurrencyCode::parse(raw).expect("literal is valid");
        let rates = std::collections::BTreeMap::from([(
            code("COP"),
            ExchangeRate::parse("3316.44").expect("literal is valid"),
        )]);
        let currencies = vec![
            Currency::new(code("COP"), "Colombian Peso", "$").expect("literal is valid"),
            Currency::new(code("USD"), "United States Dollar", "$").expect("literal is valid"),
        ];
        RateBulletin::new(
            code("USD"),
            NaiveDate::from_ymd_opt(2026, 9, 25).expect("literal is valid"),
            rates,
            currencies,
        )
        .expect("the bulletin should build")
    }

    fn scheduler(
        config: ExchangeRatesConfig,
    ) -> (ExchangeRateScheduler, Arc<InMemoryExchangeRateRepository>) {
        let rates = Arc::new(InMemoryExchangeRateRepository::new());
        let refresh = Arc::new(RefreshExchangeRates::new(
            Arc::new(StubExchangeRateProvider::publishing(bulletin())),
            rates.clone(),
            Arc::new(SystemClock::new()),
        ));
        (ExchangeRateScheduler::new(refresh, config), rates)
    }

    /// Espera a que el planificador escriba, sin quedarse colgado si no lo hace.
    async fn wait_for_a_write(rates: &Arc<InMemoryExchangeRateRepository>) {
        for _ in 0..200_u32 {
            if rates.writes() > 0 {
                return;
            }
            tokio::time::sleep(std::time::Duration::from_millis(10)).await;
        }
    }

    #[tokio::test]
    async fn a_disabled_task_is_not_even_spawned() {
        let (scheduler, rates) = scheduler(ExchangeRatesConfig {
            enabled: false,
            ..ExchangeRatesConfig::default()
        });
        assert!(scheduler.spawn().is_none());
        assert_eq!(rates.writes(), 0);
    }

    #[tokio::test]
    async fn an_enabled_task_refreshes_on_startup() {
        let (scheduler, rates) = scheduler(ExchangeRatesConfig::default());
        let handle = scheduler.spawn().expect("the task should be spawned");

        // La primera pasada es inmediata porque la base esta vacia; despues el
        // bucle se duerme hasta la siguiente hora, asi que basta con esperar a que
        // escriba y abortar.
        wait_for_a_write(&rates).await;
        handle.abort();

        assert_eq!(rates.writes(), 1);
        assert_eq!(
            rates.stored_dates(),
            [NaiveDate::from_ymd_opt(2026, 9, 25).expect("literal is valid")]
        );
    }
}
