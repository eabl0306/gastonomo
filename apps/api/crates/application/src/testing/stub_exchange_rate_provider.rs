//! Proveedor de tasas de test, sin red.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::currency::RateBulletin;

use crate::error::AppError;
use crate::port::currency::ExchangeRateProvider;

/// Doble del puerto [`ExchangeRateProvider`].
///
/// Devuelve siempre la misma pasada, o siempre el mismo fallo, y cuenta las veces
/// que se le ha llamado.
#[derive(Debug)]
pub struct StubExchangeRateProvider {
    answer: Mutex<Result<RateBulletin, String>>,
    calls: Mutex<usize>,
}

impl StubExchangeRateProvider {
    /// Proveedor que responde con esta pasada.
    #[must_use]
    pub const fn publishing(bulletin: RateBulletin) -> Self {
        Self {
            answer: Mutex::new(Ok(bulletin)),
            calls: Mutex::new(0),
        }
    }

    /// Proveedor que falla, como cuando la fuente no responde.
    #[must_use]
    pub fn failing(detail: &str) -> Self {
        Self {
            answer: Mutex::new(Err(detail.to_owned())),
            calls: Mutex::new(0),
        }
    }

    /// Cambia lo que responde a partir de la siguiente llamada.
    pub fn publish(&self, bulletin: RateBulletin) {
        if let Ok(mut answer) = self.answer.lock() {
            *answer = Ok(bulletin);
        }
    }

    /// Veces que se ha llamado a la fuente.
    #[must_use]
    pub fn calls(&self) -> usize {
        self.calls
            .lock()
            .map_or_else(|poisoned| *poisoned.into_inner(), |calls| *calls)
    }
}

#[async_trait]
impl ExchangeRateProvider for StubExchangeRateProvider {
    async fn fetch(&self) -> Result<RateBulletin, AppError> {
        if let Ok(mut calls) = self.calls.lock() {
            *calls += 1;
        }
        match self.answer.lock() {
            Ok(answer) => answer.clone().map_err(AppError::dependency),
            Err(poisoned) => poisoned.into_inner().clone().map_err(AppError::dependency),
        }
    }
}
