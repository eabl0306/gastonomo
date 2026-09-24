//! Latencia medida de una comprobacion.

use std::time::Duration;

/// Latencia observada al comprobar un componente.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Default)]
pub struct Latency(Duration);

impl Latency {
    /// Latencia cero, util como valor neutro en tests y valores por defecto.
    pub const ZERO: Self = Self(Duration::ZERO);

    /// Construye una latencia a partir de una duracion medida.
    #[must_use]
    pub const fn from_duration(duration: Duration) -> Self {
        Self(duration)
    }

    /// Construye una latencia a partir de milisegundos enteros.
    #[must_use]
    pub const fn from_millis(millis: u64) -> Self {
        Self(Duration::from_millis(millis))
    }

    /// Devuelve la latencia en milisegundos, saturando en [`u64::MAX`].
    #[must_use]
    pub fn as_millis(&self) -> u64 {
        u64::try_from(self.0.as_millis()).unwrap_or(u64::MAX)
    }

    /// Devuelve la duracion subyacente.
    #[must_use]
    pub const fn as_duration(&self) -> Duration {
        self.0
    }
}

impl From<Duration> for Latency {
    fn from(duration: Duration) -> Self {
        Self(duration)
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use super::Latency;

    #[test]
    fn converts_duration_to_milliseconds() {
        let latency = Latency::from_duration(Duration::from_micros(1_500));
        assert_eq!(latency.as_millis(), 1);
    }

    #[test]
    fn zero_is_the_neutral_value() {
        assert_eq!(Latency::ZERO.as_millis(), 0);
        assert_eq!(Latency::default(), Latency::ZERO);
    }

    #[test]
    fn saturates_instead_of_truncating() {
        let latency = Latency::from_duration(Duration::MAX);
        assert_eq!(latency.as_millis(), u64::MAX);
    }

    #[test]
    fn keeps_the_original_duration() {
        let latency = Latency::from(Duration::from_millis(42));
        assert_eq!(latency.as_duration(), Duration::from_millis(42));
        assert_eq!(latency, Latency::from_millis(42));
    }
}
