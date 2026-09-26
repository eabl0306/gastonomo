//! Reloj de test que no avanza solo.

use std::sync::Mutex;

use chrono::{DateTime, Duration, Utc};

use crate::port::Clock;

/// Reloj con la hora puesta a mano.
#[derive(Debug)]
pub struct FixedClock {
    now: Mutex<DateTime<Utc>>,
}

impl FixedClock {
    /// Crea el reloj en el instante dado.
    #[must_use]
    pub const fn new(now: DateTime<Utc>) -> Self {
        Self {
            now: Mutex::new(now),
        }
    }

    /// Crea el reloj en un instante fijo y conocido, util como valor por defecto.
    ///
    /// # Panics
    ///
    /// No entra en panico: la fecha es una constante valida.
    #[must_use]
    pub fn at_fixture() -> Self {
        let Ok(parsed) = DateTime::parse_from_rfc3339("2026-09-22T10:00:00Z") else {
            unreachable!("the fixture instant is a valid rfc3339 literal")
        };
        Self::new(parsed.with_timezone(&Utc))
    }

    /// Mueve el reloj hacia delante.
    pub fn advance(&self, delta: Duration) {
        if let Ok(mut now) = self.now.lock() {
            *now += delta;
        }
    }
}

impl Clock for FixedClock {
    fn now(&self) -> DateTime<Utc> {
        self.now
            .lock()
            .map_or_else(|poisoned| *poisoned.into_inner(), |now| *now)
    }
}
