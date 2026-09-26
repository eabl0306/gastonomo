//! Caso de uso: detalle de un prestamo.

use gastonomo_domain::UserId;
use gastonomo_domain::loan::{Loan, LoanId};

use super::ports::LoanPorts;
use crate::error::AppError;

/// Devuelve un prestamo del usuario con sus abonos embebidos.
#[derive(Debug, Clone)]
pub struct GetLoan {
    ports: LoanPorts,
}

impl GetLoan {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: LoanPorts) -> Self {
        Self { ports }
    }

    /// Busca el prestamo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si el prestamo no existe, si el
    /// identificador no es un UUID o si el prestamo es de otra cuenta. Las tres
    /// respuestas son iguales a proposito: un 403 confirmaria que existe.
    pub async fn execute(&self, user_id: UserId, loan_id: &str) -> Result<Loan, AppError> {
        let loan_id = parse_loan_id(loan_id)?;
        self.ports
            .loans
            .find(user_id, loan_id)
            .await?
            .ok_or_else(|| AppError::not_found("loan"))
    }
}

/// Un identificador que no es UUID no puede existir, asi que es un 404.
pub(super) fn parse_loan_id(raw: &str) -> Result<LoanId, AppError> {
    LoanId::parse(raw).map_err(|_| AppError::not_found("loan"))
}
