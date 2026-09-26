//! Puerto de reloj.

use chrono::{DateTime, NaiveDate, Utc};

/// Da la hora al nucleo.
///
/// Existe para que los casos de uso no llamen a `Utc::now()`: con un reloj fijo un
/// test puede comprobar caducidades sin esperar.
pub trait Clock: Send + Sync {
    /// Instante actual en UTC.
    fn now(&self) -> DateTime<Utc>;

    /// Fecha de hoy en UTC, que es la que se guarda en las columnas `date`.
    fn today(&self) -> NaiveDate {
        self.now().date_naive()
    }
}
