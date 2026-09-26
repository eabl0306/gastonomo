//! Doble en memoria del repositorio de prestamos.
//!
//! Sirve a los tests de los casos de uso y a los del router HTTP. Guarda los
//! movimientos generados aparte para poder comprobar que cada prestamo y cada
//! abono escriben el suyo.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::UserId;
use gastonomo_domain::loan::{GeneratedMovement, Loan, LoanDirection, LoanId, LoanPayment};

use crate::error::AppError;
use crate::port::loan::LoanRepository;

/// Repositorio de prestamos en memoria.
#[derive(Debug, Default)]
pub struct InMemoryLoanRepository {
    loans: Mutex<Vec<Loan>>,
    movements: Mutex<Vec<GeneratedMovement>>,
    breaks_on_movement: bool,
}

impl InMemoryLoanRepository {
    /// Repositorio vacio.
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// Repositorio que ya tiene un prestamo dentro.
    #[must_use]
    pub fn with_loan(loan: Loan) -> Self {
        Self {
            loans: Mutex::new(vec![loan]),
            ..Self::default()
        }
    }

    /// Repositorio en el que la segunda escritura siempre falla.
    ///
    /// Igual que la transaccion de PostgreSQL, no deja nada a medias: ni el
    /// prestamo ni el movimiento.
    #[must_use]
    pub fn breaking_on_the_movement() -> Self {
        Self {
            breaks_on_movement: true,
            ..Self::default()
        }
    }

    /// Prestamos guardados.
    #[must_use]
    pub fn loans(&self) -> Vec<Loan> {
        self.loans.lock().map_or_else(
            |poisoned| poisoned.into_inner().clone(),
            |loans| loans.clone(),
        )
    }

    /// Movimientos generados.
    #[must_use]
    pub fn movements(&self) -> Vec<GeneratedMovement> {
        self.movements.lock().map_or_else(
            |poisoned| poisoned.into_inner().clone(),
            |movements| movements.clone(),
        )
    }

    fn guard(&self) -> Result<(), AppError> {
        if self.breaks_on_movement {
            return Err(AppError::dependency("the movement could not be written"));
        }
        Ok(())
    }

    fn write(&self, movement: &GeneratedMovement) -> Result<(), AppError> {
        self.movements
            .lock()
            .map_err(|_| AppError::dependency("the movement store is poisoned"))?
            .push(movement.clone());
        Ok(())
    }
}

#[async_trait]
impl LoanRepository for InMemoryLoanRepository {
    async fn list(
        &self,
        user_id: UserId,
        direction: Option<LoanDirection>,
    ) -> Result<Vec<Loan>, AppError> {
        Ok(self
            .loans()
            .into_iter()
            .filter(|loan| loan.user_id() == user_id)
            .filter(|loan| direction.is_none_or(|wanted| loan.direction() == wanted))
            .collect())
    }

    async fn find(&self, user_id: UserId, loan_id: LoanId) -> Result<Option<Loan>, AppError> {
        Ok(self
            .loans()
            .into_iter()
            .find(|loan| loan.id() == loan_id && loan.user_id() == user_id))
    }

    async fn create(&self, loan: &Loan, movement: &GeneratedMovement) -> Result<(), AppError> {
        self.guard()?;
        self.loans
            .lock()
            .map_err(|_| AppError::dependency("the loan store is poisoned"))?
            .push(loan.clone());
        self.write(movement)
    }

    async fn add_payment(
        &self,
        payment: &LoanPayment,
        movement: &GeneratedMovement,
    ) -> Result<(), AppError> {
        self.guard()?;
        let mut loans = self
            .loans
            .lock()
            .map_err(|_| AppError::dependency("the loan store is poisoned"))?;
        let position = loans
            .iter()
            .position(|loan| loan.id() == payment.loan_id())
            .ok_or_else(|| AppError::not_found("loan"))?;
        let stored = loans.remove(position);
        loans.insert(position, stored.with_payment(payment.clone()));
        drop(loans);
        self.write(movement)
    }
}
