//! Caso de uso: nuevo prestamo, con el movimiento que el diseno exige.

use chrono::NaiveDate;
use gastonomo_domain::loan::{GeneratedMovement, Loan, LoanDirection, NewLoan};
use gastonomo_domain::{MonetaryAmount, PersonName, UserId};

use super::ports::LoanPorts;
use super::validation::{self, MoneyFields};
use crate::error::{AppError, ValidationErrors};

/// Datos del formulario de Nuevo prestamo.
///
/// `amount` y `currency` son obligatorios. `base` y `baseCurrency` se pueden omitir
/// los dos juntos, igual que en un movimiento: si el prestamo va en la moneda
/// principal la equivalencia es el propio importe. La API no calcula equivalencias,
/// guarda la que reciba, incluida la tasa que el usuario haya editado a mano.
#[derive(Debug, Clone)]
pub struct CreateLoanCommand {
    /// Persona a la que preste o que me presto.
    pub person: String,
    /// Nota libre, puede venir vacia.
    pub note: String,
    /// `lent` si preste, `borrowed` si me prestaron.
    pub direction: String,
    /// Fecha del prestamo, `YYYY-MM-DD`.
    pub opened_on: String,
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

/// Crea el prestamo y su movimiento.
///
/// Las dos escrituras van en una transaccion. Si el movimiento falla no queda el
/// prestamo: el balance de Inicio saldria mal y el usuario no tiene forma de
/// arreglarlo desde la app, porque el diseno no tiene pantalla de edicion.
#[derive(Debug, Clone)]
pub struct CreateLoan {
    ports: LoanPorts,
}

/// Campos ya validados del formulario.
struct ValidLoan {
    person: PersonName,
    note: String,
    direction: LoanDirection,
    opened_on: NaiveDate,
    amount: MonetaryAmount,
}

impl CreateLoan {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: LoanPorts) -> Self {
        Self { ports }
    }

    /// Guarda el prestamo y devuelve el agregado recien creado.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si el formulario no cumple las reglas y
    /// [`AppError::Dependency`] si la transaccion falla.
    pub async fn execute(
        &self,
        user_id: UserId,
        command: CreateLoanCommand,
    ) -> Result<Loan, AppError> {
        let valid = validate(&command)?;
        let loan = Loan::open(NewLoan {
            user_id,
            person: valid.person,
            note: valid.note,
            direction: valid.direction,
            opened_on: valid.opened_on,
            amount: valid.amount,
            created_at: self.ports.clock.now(),
        });
        let movement = GeneratedMovement::for_loan(&loan);
        self.ports.loans.create(&loan, &movement).await?;
        Ok(loan)
    }
}

fn validate(command: &CreateLoanCommand) -> Result<ValidLoan, AppError> {
    let mut errors = ValidationErrors::new();
    let person = validation::person(&command.person, &mut errors);
    let note = validation::note(&command.note, &mut errors);
    let direction = validation::direction(&command.direction, &mut errors);
    let opened_on = validation::date(&command.opened_on, "openedOn", &mut errors);
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

    let (Some(person), Some(note), Some(direction), Some(opened_on), Some(amount)) =
        (person, note, direction, opened_on, amount)
    else {
        unreachable!("every error was already collected above")
    };
    Ok(ValidLoan {
        person,
        note,
        direction,
        opened_on,
        amount,
    })
}
