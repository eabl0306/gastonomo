//! Codigo de moneda ISO 4217.

use std::fmt;

use thiserror::Error;

use super::currency_decimals::{decimals_for, is_zero_decimal};

/// Motivo por el que un codigo de moneda no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CurrencyCodeError {
    /// El codigo no tiene exactamente tres letras.
    #[error("a currency code must be three letters, got {got:?}")]
    Length {
        /// Valor recibido.
        got: String,
    },
    /// El codigo lleva algo que no es una letra ASCII.
    #[error("a currency code only accepts ascii letters, got {got:?}")]
    NotAlphabetic {
        /// Valor recibido.
        got: String,
    },
}

/// Codigo de moneda ISO 4217 en mayusculas.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct CurrencyCode(String);

impl CurrencyCode {
    /// Valida y normaliza un codigo de moneda.
    ///
    /// Acepta minusculas y espacios alrededor, y guarda siempre mayusculas.
    ///
    /// # Errors
    ///
    /// Devuelve [`CurrencyCodeError`] si no son tres letras ASCII.
    pub fn parse(raw: &str) -> Result<Self, CurrencyCodeError> {
        let trimmed = raw.trim();
        if trimmed.chars().count() != 3 {
            return Err(CurrencyCodeError::Length {
                got: trimmed.to_owned(),
            });
        }
        if !trimmed.chars().all(|c| c.is_ascii_alphabetic()) {
            return Err(CurrencyCodeError::NotAlphabetic {
                got: trimmed.to_owned(),
            });
        }
        Ok(Self(trimmed.to_ascii_uppercase()))
    }

    /// Codigo en mayusculas.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    /// Decimales con los que se muestra esta moneda.
    #[must_use]
    pub fn decimals(&self) -> u32 {
        decimals_for(&self.0)
    }

    /// Indica si la moneda no usa decimales.
    #[must_use]
    pub fn is_zero_decimal(&self) -> bool {
        is_zero_decimal(&self.0)
    }
}

impl fmt::Display for CurrencyCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
