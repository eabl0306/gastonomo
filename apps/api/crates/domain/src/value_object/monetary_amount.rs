//! El par de importes que guarda cada registro.

use thiserror::Error;

use super::currency_code::CurrencyCode;
use super::money::Money;

/// Motivo por el que el par de importes no es coherente.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MonetaryAmountError {
    /// El importe tecleado no es positivo.
    #[error("the amount must be greater than zero")]
    AmountNotPositive,
    /// La equivalencia en la moneda principal no es positiva.
    #[error("the base amount must be greater than zero")]
    BaseNotPositive,
    /// Misma moneda con importes distintos.
    #[error("with a single currency the amount and its base must match")]
    InconsistentSingleCurrency,
}

/// Importe de un registro: lo que tecleo el usuario y su equivalencia.
///
/// El diseno es explicito: `amount` mas `currency` es lo que escribio el usuario,
/// y `base` mas `base_currency` es la equivalencia en su moneda principal en el
/// momento de registrar, posiblemente con una tasa que el propio usuario edito.
/// La API guarda los cuatro campos tal cual y nunca recalcula la equivalencia.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MonetaryAmount {
    amount: Money,
    currency: CurrencyCode,
    base: Money,
    base_currency: CurrencyCode,
}

impl MonetaryAmount {
    /// Construye el par comprobando sus invariantes.
    ///
    /// # Errors
    ///
    /// Devuelve [`MonetaryAmountError`] si algun importe no es positivo o si con
    /// una sola moneda el importe y su equivalencia no coinciden.
    pub fn new(
        amount: Money,
        currency: CurrencyCode,
        base: Money,
        base_currency: CurrencyCode,
    ) -> Result<Self, MonetaryAmountError> {
        if !amount.is_positive() {
            return Err(MonetaryAmountError::AmountNotPositive);
        }
        if !base.is_positive() {
            return Err(MonetaryAmountError::BaseNotPositive);
        }
        if currency == base_currency && amount != base {
            return Err(MonetaryAmountError::InconsistentSingleCurrency);
        }
        Ok(Self {
            amount,
            currency,
            base,
            base_currency,
        })
    }

    /// Construye el par cuando el registro va en la moneda principal.
    ///
    /// # Errors
    ///
    /// Devuelve [`MonetaryAmountError::AmountNotPositive`] si el importe no es
    /// positivo.
    pub fn single_currency(
        amount: Money,
        currency: CurrencyCode,
    ) -> Result<Self, MonetaryAmountError> {
        Self::new(amount, currency.clone(), amount, currency)
    }

    /// Importe tal y como lo tecleo el usuario.
    #[must_use]
    pub const fn amount(&self) -> Money {
        self.amount
    }

    /// Moneda del importe tecleado.
    #[must_use]
    pub const fn currency(&self) -> &CurrencyCode {
        &self.currency
    }

    /// Equivalencia guardada en la moneda principal del momento.
    #[must_use]
    pub const fn base(&self) -> Money {
        self.base
    }

    /// Moneda de la equivalencia guardada.
    #[must_use]
    pub const fn base_currency(&self) -> &CurrencyCode {
        &self.base_currency
    }

    /// Indica si el registro se hizo en una moneda distinta de la principal.
    #[must_use]
    pub fn is_converted(&self) -> bool {
        self.currency != self.base_currency
    }
}
