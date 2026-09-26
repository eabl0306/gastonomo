//! Tasa de cambio exacta.

use std::fmt;
use std::str::FromStr as _;

use rust_decimal::{Decimal, RoundingStrategy};
use thiserror::Error;

/// Decimales que guarda la base de datos para una tasa.
///
/// Diez, los de `NUMERIC(20,10)`. Frankfurter publica cinco o seis, pero una tasa
/// cruzada entre dos monedas necesita mas margen que un importe.
pub const RATE_SCALE: u32 = 10;

/// Mayor valor admitido, impuesto por `NUMERIC(20,10)`: diez digitos enteros.
fn max_absolute() -> Decimal {
    Decimal::new(10_000_000_000, 0)
}

/// Motivo por el que una tasa no es valida.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum ExchangeRateError {
    /// El texto recibido no es un numero decimal.
    #[error("{got:?} is not a decimal rate")]
    NotANumber {
        /// Valor recibido.
        got: String,
    },
    /// La tasa no es estrictamente positiva.
    #[error("a rate must be greater than zero")]
    NotPositive,
    /// La tasa no cabe en `NUMERIC(20,10)`.
    #[error("the rate is out of the supported range")]
    OutOfRange,
}

/// Cuantas unidades de la moneda cotizada da una unidad de la base.
///
/// Es un `Decimal` y no un `f64` por el mismo motivo que
/// [`crate::value_object::Money`]: las conversiones acaban en cifras que el
/// usuario compara con su banco.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct ExchangeRate(Decimal);

impl ExchangeRate {
    /// La tasa de una moneda consigo misma.
    pub const ONE: Self = Self(Decimal::ONE);

    /// Construye una tasa desde un `Decimal`.
    ///
    /// Redondea a diez decimales y quita los ceros de la derecha, para que la
    /// misma tasa se sirva igual venga de la fuente (`1.0`) o de una columna
    /// `NUMERIC(20,10)` (`1.0000000000`).
    ///
    /// # Errors
    ///
    /// Devuelve [`ExchangeRateError::NotPositive`] si no es mayor que cero y
    /// [`ExchangeRateError::OutOfRange`] si no cabe en `NUMERIC(20,10)`.
    pub fn from_decimal(value: Decimal) -> Result<Self, ExchangeRateError> {
        let rounded = value
            .round_dp_with_strategy(RATE_SCALE, RoundingStrategy::MidpointAwayFromZero)
            .normalize();
        if rounded <= Decimal::ZERO {
            return Err(ExchangeRateError::NotPositive);
        }
        if rounded >= max_absolute() {
            return Err(ExchangeRateError::OutOfRange);
        }
        Ok(Self(rounded))
    }

    /// Construye una tasa desde su representacion textual.
    ///
    /// Es la puerta de entrada de lo que publica la fuente: el numero llega como
    /// el texto literal del JSON, sin pasar por un `f64`.
    ///
    /// # Errors
    ///
    /// Devuelve [`ExchangeRateError`] si el texto no es un decimal, no es
    /// positivo o no cabe.
    pub fn parse(raw: &str) -> Result<Self, ExchangeRateError> {
        let trimmed = raw.trim();
        let parsed = Decimal::from_str(trimmed).map_err(|_| ExchangeRateError::NotANumber {
            got: trimmed.to_owned(),
        })?;
        Self::from_decimal(parsed)
    }

    /// Valor interno, para guardarlo en `NUMERIC(20,10)`.
    #[must_use]
    pub const fn decimal(self) -> Decimal {
        self.0
    }
}

impl fmt::Display for ExchangeRate {
    /// Escribe la tasa sin ceros sobrantes: `1`, `3316.44`, `0.87732`.
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use rust_decimal::Decimal;

    use super::{ExchangeRate, ExchangeRateError};

    #[test]
    fn the_source_representation_of_one_is_served_as_one() {
        assert_eq!(ExchangeRate::parse("1.0").expect("valid").to_string(), "1");
        assert_eq!(ExchangeRate::ONE.to_string(), "1");
    }

    #[test]
    fn a_rate_read_from_the_database_loses_its_padding_zeros() {
        let rate = ExchangeRate::parse("3316.4400000000").expect("valid");
        assert_eq!(rate.to_string(), "3316.44");
    }

    #[test]
    fn the_published_precision_survives_the_round_trip() {
        let rate = ExchangeRate::parse("0.87732").expect("valid");
        assert_eq!(rate.to_string(), "0.87732");
        assert_eq!(rate.decimal(), Decimal::new(87_732, 5));
    }

    #[test]
    fn eleven_decimals_are_rounded_to_ten() {
        let rate = ExchangeRate::parse("0.00000000005").expect("valid");
        assert_eq!(rate.to_string(), "0.0000000001");
    }

    #[test]
    fn a_rate_that_rounds_to_zero_is_rejected() {
        assert_eq!(
            ExchangeRate::parse("0.00000000001"),
            Err(ExchangeRateError::NotPositive)
        );
    }

    #[test]
    fn zero_and_negative_rates_are_rejected() {
        assert_eq!(
            ExchangeRate::parse("0"),
            Err(ExchangeRateError::NotPositive)
        );
        assert_eq!(
            ExchangeRate::parse("-3.5"),
            Err(ExchangeRateError::NotPositive)
        );
    }

    #[test]
    fn a_rate_that_does_not_fit_the_column_is_rejected() {
        assert_eq!(
            ExchangeRate::parse("10000000000"),
            Err(ExchangeRateError::OutOfRange)
        );
    }

    #[test]
    fn something_that_is_not_a_number_is_rejected() {
        assert!(matches!(
            ExchangeRate::parse("null"),
            Err(ExchangeRateError::NotANumber { .. })
        ));
    }
}
