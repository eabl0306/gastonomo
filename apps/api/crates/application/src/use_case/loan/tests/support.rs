//! Montaje comun de los tests de prestamos.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::loan::{Loan, LoanDirection, NewLoan};
use gastonomo_domain::{CurrencyCode, MonetaryAmount, Money, PersonName, UserId};

use crate::port::Clock as _;
use crate::testing::FixedClock;
use crate::use_case::loan::testing::InMemoryLoanRepository;
use crate::use_case::loan::{
    CreateLoan, CreateLoanCommand, GetLoan, ListLoans, LoanPorts, RegisterLoanPayment,
    RegisterLoanPaymentCommand,
};

/// Cuenta dueña de los prestamos en los tests.
#[must_use]
pub fn owner() -> UserId {
    UserId::generate()
}

/// Casos de uso cableados contra un repositorio en memoria.
pub struct Harness {
    pub loans: Arc<InMemoryLoanRepository>,
    pub ports: LoanPorts,
}

impl Harness {
    /// Repositorio vacio.
    #[must_use]
    pub fn new() -> Self {
        Self::with(Arc::new(InMemoryLoanRepository::new()))
    }

    /// Repositorio en el que la segunda escritura falla.
    #[must_use]
    pub fn breaking() -> Self {
        Self::with(Arc::new(InMemoryLoanRepository::breaking_on_the_movement()))
    }

    /// Repositorio con un prestamo ya dentro.
    #[must_use]
    pub fn with_loan(loan: Loan) -> Self {
        Self::with(Arc::new(InMemoryLoanRepository::with_loan(loan)))
    }

    fn with(loans: Arc<InMemoryLoanRepository>) -> Self {
        let ports = LoanPorts {
            loans: loans.clone(),
            clock: Arc::new(FixedClock::at_fixture()),
        };
        Self { loans, ports }
    }

    /// Caso de uso de creacion.
    #[must_use]
    pub fn create(&self) -> CreateLoan {
        CreateLoan::new(self.ports.clone())
    }

    /// Caso de uso de listado.
    #[must_use]
    pub fn list(&self) -> ListLoans {
        ListLoans::new(self.ports.clone())
    }

    /// Caso de uso de detalle.
    #[must_use]
    pub fn get(&self) -> GetLoan {
        GetLoan::new(self.ports.clone())
    }

    /// Caso de uso de abono.
    #[must_use]
    pub fn pay(&self) -> RegisterLoanPayment {
        RegisterLoanPayment::new(self.ports.clone())
    }
}

/// Formulario valido de Nuevo prestamo, en una sola moneda.
#[must_use]
pub fn a_loan() -> CreateLoanCommand {
    CreateLoanCommand {
        person: "Rosa".to_owned(),
        note: "Teléfono".to_owned(),
        direction: "lent".to_owned(),
        opened_on: "2026-06-28".to_owned(),
        amount: "600000".to_owned(),
        currency: "COP".to_owned(),
        base: Some("600000".to_owned()),
        base_currency: Some("COP".to_owned()),
    }
}

/// Sheet valido de Registrar abono.
#[must_use]
pub fn a_payment() -> RegisterLoanPaymentCommand {
    RegisterLoanPaymentCommand {
        paid_on: "2026-07-29".to_owned(),
        label: Some("Intereses".to_owned()),
        amount: "200000".to_owned(),
        currency: "COP".to_owned(),
        base: Some("200000".to_owned()),
        base_currency: Some("COP".to_owned()),
    }
}

/// Prestamo ya guardado, del usuario dado.
#[must_use]
pub fn stored_loan(user_id: UserId, direction: LoanDirection) -> Loan {
    let clock = FixedClock::at_fixture();
    let amount = Money::parse("600000").expect("valid amount");
    let currency = CurrencyCode::parse("COP").expect("valid currency");
    Loan::open(NewLoan {
        user_id,
        person: PersonName::parse("Rosa").expect("valid person"),
        note: String::new(),
        direction,
        opened_on: clock.today(),
        amount: MonetaryAmount::single_currency(amount, currency).expect("valid amount"),
        created_at: clock.now(),
    })
}
