//! Piezas que comparten los cuatro casos de uso de prestamos.

use std::fmt;
use std::sync::Arc;

use crate::port::Clock;
use crate::port::loan::LoanRepository;

/// Puertos del contexto de prestamos.
#[derive(Clone)]
pub struct LoanPorts {
    /// Persistencia de prestamos, abonos y sus movimientos.
    pub loans: Arc<dyn LoanRepository>,
    /// Reloj, para los instantes tecnicos de creacion.
    pub clock: Arc<dyn Clock>,
}

impl fmt::Debug for LoanPorts {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("LoanPorts").finish_non_exhaustive()
    }
}
