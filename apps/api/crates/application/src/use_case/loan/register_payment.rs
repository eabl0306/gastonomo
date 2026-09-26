//! Caso de uso: registrar un abono, con el movimiento que el diseno exige.

use chrono::NaiveDate;
use gastonomo_domain::loan::{GeneratedMovement, Loan, LoanPayment, NewLoanPayment};
use gastonomo_domain::{MonetaryAmount, UserId};

use super::get_loan::parse_loan_id;
use super::ports::LoanPorts;
use super::validation::{self, MoneyFields};
use crate::error::{AppError, ValidationErrors};

/// Datos del sheet Registrar abono.
#[derive(Debug, Clone)]
pub struct RegisterLoanPaymentCommand {
    /// Fecha del abono, `YYYY-MM-DD`.
    pub paid_on: String,
    /// Concepto opcional: en el diseno, "Detalles" o "Intereses".
    pub label: Option<String>,
    /// Importe tecleado.
    pub amount: String,
    /// Moneda del importe tecleado.
    pub currency: String,
    /// Equivalencia en la moneda principal del momento. Se puede omitir junto con
    /// `base_currency`, y entonces la equivalencia es el propio importe.
    pub base: Option<String>,
    /// Moneda de la equivalencia. Va siempre con `base`.
    pub base_currency: Option<String>,
}

/// Registra el abono y su movimiento.
///
/// Un abono puede pasarse del saldo y no se rechaza: el diseno lo contempla y
/// ensena el exceso como "Intereses ganados".
#[derive(Debug, Clone)]
pub struct RegisterLoanPayment {
    ports: LoanPorts,
}

/// Campos ya validados del sheet.
struct ValidPayment {
    paid_on: NaiveDate,
    label: Option<String>,
    amount: MonetaryAmount,
}

impl RegisterLoanPayment {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: LoanPorts) -> Self {
        Self { ports }
    }

    /// Guarda el abono y devuelve el prestamo ya con el abono dentro.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si el sheet no cumple las reglas,
    /// [`AppError::NotFound`] si el prestamo no existe o es de otra cuenta, y
    /// [`AppError::Dependency`] si la transaccion falla.
    pub async fn execute(
        &self,
        user_id: UserId,
        loan_id: &str,
        command: RegisterLoanPaymentCommand,
    ) -> Result<Loan, AppError> {
        let loan_id = parse_loan_id(loan_id)?;
        let valid = validate(&command)?;
        let loan = self
            .ports
            .loans
            .find(user_id, loan_id)
            .await?
            .ok_or_else(|| AppError::not_found("loan"))?;

        let payment = LoanPayment::record(NewLoanPayment {
            loan_id: loan.id(),
            user_id,
            paid_on: valid.paid_on,
            label: valid.label,
            amount: valid.amount,
            created_at: self.ports.clock.now(),
        });
        let movement = GeneratedMovement::for_payment(&loan, &payment);
        self.ports.loans.add_payment(&payment, &movement).await?;
        Ok(loan.with_payment(payment))
    }
}

fn validate(command: &RegisterLoanPaymentCommand) -> Result<ValidPayment, AppError> {
    let mut errors = ValidationErrors::new();
    let paid_on = validation::date(&command.paid_on, "paidOn", &mut errors);
    let label = validation::label(command.label.as_deref(), &mut errors);
    let amount = validation::money(
        MoneyFields {
            amount: &command.amount,
            currency: &command.currency,
            base: command.base.as_deref(),
            base_currency: command.base_currency.as_deref(),
        },
        &mut errors,
    );
    errors.into_result()?;

    let (Some(paid_on), Some(amount)) = (paid_on, amount) else {
        unreachable!("every error was already collected above")
    };
    Ok(ValidPayment {
        paid_on,
        label,
        amount,
    })
}
