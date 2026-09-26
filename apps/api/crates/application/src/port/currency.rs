//! Puertos del contexto de monedas y tasas.
//!
//! Dos puertos y una razon para que sean dos: el caso de uso de refresco se
//! prueba con un proveedor falso y sin red, y el de lectura se prueba sin
//! PostgreSQL.
//!
//! - [`ExchangeRateProvider`] lo implementa el adaptador de Frankfurter.
//! - [`ExchangeRateRepository`] lo implementa el de PostgreSQL.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRateTable, RateBulletin};

use crate::error::AppError;

/// Lee las tasas publicadas por la fuente externa.
#[async_trait]
pub trait ExchangeRateProvider: Send + Sync {
    /// Trae la ultima pasada disponible.
    ///
    /// La implementacion es la que sabe que base pide y como filtra: devuelve ya
    /// un [`RateBulletin`], que es el tipo de dominio con el filtro aplicado.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si la fuente no responde o responde algo
    /// que no encaja con el contrato que la API espera de ella.
    async fn fetch(&self) -> Result<RateBulletin, AppError>;
}

/// Que paso al intentar guardar una pasada.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SaveOutcome {
    /// Las tasas quedaron escritas.
    Written,
    /// Otra replica tenia el refresco tomado, asi que esta no escribio nada.
    ///
    /// No es un error: es la coordinacion funcionando.
    LockedElsewhere,
}

/// Guarda y lee las tasas y el catalogo de monedas.
#[async_trait]
pub trait ExchangeRateRepository: Send + Sync {
    /// Tabla vigente de una base, o `None` si todavia no hay ninguna.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn latest(&self, base: &CurrencyCode) -> Result<Option<ExchangeRateTable>, AppError>;

    /// Monedas que se ofrecen hoy en el sheet de seleccion.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn offered_currencies(&self) -> Result<Vec<Currency>, AppError>;

    /// Guarda una pasada completa.
    ///
    /// La escritura es idempotente por `(rate_date, base, quote)` y nunca borra
    /// lo anterior: una moneda que desaparece de la fuente deja de ofrecerse, pero
    /// sus tasas y los registros que la usan siguen ahi.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn save(
        &self,
        bulletin: &RateBulletin,
        fetched_at: DateTime<Utc>,
    ) -> Result<SaveOutcome, AppError>;
}
