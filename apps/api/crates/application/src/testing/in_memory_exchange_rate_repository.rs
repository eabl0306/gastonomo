//! Tasas de cambio en memoria.

use std::collections::BTreeMap;
use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRateTable, RateBulletin};

use crate::error::AppError;
use crate::port::currency::{ExchangeRateRepository, SaveOutcome};

/// Lo que el doble guarda.
#[derive(Debug, Default)]
struct Stored {
    tables: BTreeMap<NaiveDate, ExchangeRateTable>,
    currencies: Vec<Currency>,
    writes: usize,
    locked: bool,
}

/// Doble del puerto [`ExchangeRateRepository`] sobre un mapa por fecha.
///
/// Imita lo que importa del adaptador real: `latest` devuelve la fecha mas alta y
/// guardar dos veces la misma fecha no anade una fila nueva.
#[derive(Debug, Default)]
pub struct InMemoryExchangeRateRepository {
    stored: Mutex<Stored>,
}

impl InMemoryExchangeRateRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            stored: Mutex::new(Stored {
                tables: BTreeMap::new(),
                currencies: Vec::new(),
                writes: 0,
                locked: false,
            }),
        }
    }

    /// Repositorio que ya tiene una pasada guardada.
    #[must_use]
    pub fn seeded(bulletin: &RateBulletin, fetched_at: DateTime<Utc>) -> Self {
        let repository = Self::new();
        repository.store(bulletin, fetched_at);
        repository
    }

    /// Guarda una pasada sin pasar por el puerto, para preparar un test.
    pub fn seed(&self, bulletin: &RateBulletin, fetched_at: DateTime<Utc>) {
        self.store(bulletin, fetched_at);
    }

    /// Hace como si otra replica tuviera el refresco tomado.
    pub fn lock_refresh(&self) {
        self.with(|stored| stored.locked = true);
    }

    /// Fechas guardadas, de la mas antigua a la mas reciente.
    #[must_use]
    pub fn stored_dates(&self) -> Vec<NaiveDate> {
        self.with(|stored| stored.tables.keys().copied().collect())
    }

    /// Escrituras que llegaron a tocar los datos.
    #[must_use]
    pub fn writes(&self) -> usize {
        self.with(|stored| stored.writes)
    }

    fn store(&self, bulletin: &RateBulletin, fetched_at: DateTime<Utc>) {
        let table = bulletin.clone().into_table(fetched_at);
        let currencies = bulletin.currencies().to_vec();
        self.with(|stored| {
            stored.tables.insert(table.date(), table.clone());
            stored.currencies.clone_from(&currencies);
            stored.writes += 1;
        });
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Stored) -> T) -> T {
        match self.stored.lock() {
            Ok(mut stored) => operation(&mut stored),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl ExchangeRateRepository for InMemoryExchangeRateRepository {
    async fn latest(&self, base: &CurrencyCode) -> Result<Option<ExchangeRateTable>, AppError> {
        Ok(self.with(|stored| {
            stored
                .tables
                .values()
                .rev()
                .find(|table| table.base() == base)
                .cloned()
        }))
    }

    async fn offered_currencies(&self) -> Result<Vec<Currency>, AppError> {
        Ok(self.with(|stored| stored.currencies.clone()))
    }

    async fn save(
        &self,
        bulletin: &RateBulletin,
        fetched_at: DateTime<Utc>,
    ) -> Result<SaveOutcome, AppError> {
        if self.with(|stored| stored.locked) {
            return Ok(SaveOutcome::LockedElsewhere);
        }
        self.store(bulletin, fetched_at);
        Ok(SaveOutcome::Written)
    }
}
