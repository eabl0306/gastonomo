//! Importe monetario exacto.
//!
//! Envuelve `rust_decimal::Decimal` porque el dinero no puede ser un `f64`:
//! `0.1 + 0.2` no es `0.3` y aqui el usuario compara saldos con su banco. La
//! escala util es de cuatro decimales, la misma que `NUMERIC(18,4)` en
//! PostgreSQL.

use std::cmp::Ordering;
use std::fmt;
use std::str::FromStr as _;

use rust_decimal::{Decimal, RoundingStrategy};
use thiserror::Error;

use super::currency_code::CurrencyCode;

/// Decimales que se guardan en la base de datos.
pub const STORAGE_SCALE: u32 = 4;

/// Mayor valor absoluto admitido, impuesto por `NUMERIC(18,4)`: 14 digitos
/// enteros mas 4 decimales.
fn max_absolute() -> Decimal {
    Decimal::new(100_000_000_000_000, 0)
}

/// Umbral por debajo del cual el diseno considera cero un resto.
fn negligible_threshold(zero_decimal: bool) -> Decimal {
    if zero_decimal {
        Decimal::new(5, 1)
    } else {
        Decimal::new(5, 3)
    }
}

/// Motivo por el que un importe no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MoneyError {
    /// El texto recibido no es un numero decimal.
    #[error("{got:?} is not a decimal amount")]
    NotANumber {
        /// Valor recibido.
        got: String,
    },
    /// El importe no cabe en `NUMERIC(18,4)`.
    #[error("the amount is out of the supported range")]
    OutOfRange,
}

/// Importe con cuatro decimales de precision.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Default)]
pub struct Money(Decimal);

impl Money {
    /// Importe cero.
    pub const ZERO: Self = Self(Decimal::ZERO);

    /// Construye un importe desde un `Decimal`, redondeando a cuatro decimales.
    ///
    /// El redondeo es el comercial de toda la vida (la mitad se va hacia arriba en
    /// valor absoluto), no el bancario que trae `round_dp` por defecto: un usuario
    /// que teclea 1,00005 espera 1,0001.
    ///
    /// # Errors
    ///
    /// Devuelve [`MoneyError::OutOfRange`] si no cabe en `NUMERIC(18,4)`.
    pub fn from_decimal(value: Decimal) -> Result<Self, MoneyError> {
        let rounded =
            value.round_dp_with_strategy(STORAGE_SCALE, RoundingStrategy::MidpointAwayFromZero);
        if rounded.abs() >= max_absolute() {
            return Err(MoneyError::OutOfRange);
        }
        Ok(Self(rounded))
    }

    /// Construye un importe desde su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`MoneyError`] si el texto no es un decimal o no cabe.
    pub fn parse(raw: &str) -> Result<Self, MoneyError> {
        let trimmed = raw.trim();
        let parsed = Decimal::from_str(trimmed).map_err(|_| MoneyError::NotANumber {
            got: trimmed.to_owned(),
        })?;
        Self::from_decimal(parsed)
    }

    /// Valor interno.
    #[must_use]
    pub const fn decimal(self) -> Decimal {
        self.0
    }

    /// Suma dos importes.
    ///
    /// No es `std::ops::Add` porque la suma puede salirse del rango de
    /// `NUMERIC(18,4)` y devolver un `Result` es mejor que entrar en panico.
    ///
    /// # Errors
    ///
    /// Devuelve [`MoneyError::OutOfRange`] si el resultado se sale de rango.
    pub fn plus(self, other: Self) -> Result<Self, MoneyError> {
        self.0
            .checked_add(other.0)
            .ok_or(MoneyError::OutOfRange)
            .and_then(Self::from_decimal)
    }

    /// Resta dos importes.
    ///
    /// No es `std::ops::Sub`, por el mismo motivo que [`Money::plus`].
    ///
    /// # Errors
    ///
    /// Devuelve [`MoneyError::OutOfRange`] si el resultado se sale de rango.
    pub fn minus(self, other: Self) -> Result<Self, MoneyError> {
        self.0
            .checked_sub(other.0)
            .ok_or(MoneyError::OutOfRange)
            .and_then(Self::from_decimal)
    }

    /// Valor absoluto.
    #[must_use]
    pub fn absolute(self) -> Self {
        Self(self.0.abs())
    }

    /// Indica si el importe es exactamente cero.
    #[must_use]
    pub const fn is_zero(self) -> bool {
        self.0.is_zero()
    }

    /// Indica si el importe es negativo.
    #[must_use]
    pub fn is_negative(self) -> bool {
        self.0 < Decimal::ZERO
    }

    /// Indica si el importe es estrictamente mayor que cero.
    #[must_use]
    pub fn is_positive(self) -> bool {
        self.0 > Decimal::ZERO
    }

    /// Lleva a cero los restos que el diseno considera despreciables.
    ///
    /// El saldo de un prestamo sale de restar equivalencias con tasas actuales, y
    /// eso deja restos de fracciones de centimo. El diseno los trata como cero
    /// por debajo de 0,005, o de 0,5 en monedas sin decimales.
    #[must_use]
    pub fn negligible_to_zero(self, currency: &CurrencyCode) -> Self {
        if self.0.abs() < negligible_threshold(currency.is_zero_decimal()) {
            Self::ZERO
        } else {
            self
        }
    }

    /// Compara dos importes.
    #[must_use]
    pub fn compare(self, other: Self) -> Ordering {
        self.0.cmp(&other.0)
    }

    /// Representacion con los cuatro decimales que guarda la base de datos.
    #[must_use]
    pub fn to_storage_string(self) -> String {
        format!("{:.4}", self.0)
    }
}

impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}
