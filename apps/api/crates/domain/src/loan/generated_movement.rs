//! El movimiento que nace con un prestamo o con un abono.

use chrono::{DateTime, NaiveDate, Utc};

use super::agreement::{Loan, LoanId};
use super::payment::{LoanPayment, LoanPaymentId};
use crate::category::{CategoryKind, LOAN_CATEGORY_NAME, LOAN_PAYMENT_CATEGORY_NAME};
use crate::value_object::{MonetaryAmount, UserId};

/// Separador entre el prefijo y la persona en el titulo, el del prototipo.
const TITLE_SEPARATOR: &str = " · ";

/// Prefijo del titulo del movimiento de un prestamo: "Préstamo · {persona}".
const LOAN_TITLE_PREFIX: &str = "Préstamo";

/// Prefijo del titulo del movimiento de un abono: "Abono · {persona}".
const PAYMENT_TITLE_PREFIX: &str = "Abono";

/// Fila de `movements` que hay que escribir junto al prestamo o al abono.
///
/// No es el agregado de movimientos, que vive en su propio contexto: es solo lo
/// que esta transaccion tiene que insertar. Asi el contexto de prestamos cumple el
/// efecto que pide el diseno sin invadir el modelo del vecino.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GeneratedMovement {
    user_id: UserId,
    occurred_on: NaiveDate,
    kind: CategoryKind,
    category_name: &'static str,
    title: String,
    amount: MonetaryAmount,
    loan_id: LoanId,
    loan_payment_id: Option<LoanPaymentId>,
    created_at: DateTime<Utc>,
}

impl GeneratedMovement {
    /// Movimiento del prestamo: gasto si preste, ingreso si me prestaron.
    #[must_use]
    pub fn for_loan(loan: &Loan) -> Self {
        Self {
            user_id: loan.user_id(),
            occurred_on: loan.opened_on(),
            kind: loan.direction().loan_movement_kind(),
            category_name: LOAN_CATEGORY_NAME,
            title: title(LOAN_TITLE_PREFIX, loan),
            amount: loan.amount().clone(),
            loan_id: loan.id(),
            loan_payment_id: None,
            created_at: loan.created_at(),
        }
    }

    /// Movimiento del abono: ingreso si preste, gasto si me prestaron.
    #[must_use]
    pub fn for_payment(loan: &Loan, payment: &LoanPayment) -> Self {
        Self {
            user_id: payment.user_id(),
            occurred_on: payment.paid_on(),
            kind: loan.direction().payment_movement_kind(),
            category_name: LOAN_PAYMENT_CATEGORY_NAME,
            title: title(PAYMENT_TITLE_PREFIX, loan),
            amount: payment.amount().clone(),
            loan_id: loan.id(),
            loan_payment_id: Some(payment.id()),
            created_at: payment.created_at(),
        }
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.user_id
    }

    /// Fecha del movimiento, la misma del prestamo o del abono.
    #[must_use]
    pub const fn occurred_on(&self) -> NaiveDate {
        self.occurred_on
    }

    /// Ingreso o gasto.
    #[must_use]
    pub const fn kind(&self) -> CategoryKind {
        self.kind
    }

    /// Nombre de la categoria predeterminada a la que se cuelga.
    ///
    /// Es un nombre y no un identificador porque las categorias son de otro
    /// contexto: el adaptador resuelve el identificador dentro de la transaccion.
    #[must_use]
    pub const fn category_name(&self) -> &'static str {
        self.category_name
    }

    /// Titulo que ve el usuario en la lista de movimientos.
    #[must_use]
    pub fn title(&self) -> &str {
        &self.title
    }

    /// Importe, el mismo del prestamo o del abono.
    #[must_use]
    pub const fn amount(&self) -> &MonetaryAmount {
        &self.amount
    }

    /// Prestamo que lo genero.
    #[must_use]
    pub const fn loan_id(&self) -> LoanId {
        self.loan_id
    }

    /// Abono que lo genero, si lo genero un abono.
    #[must_use]
    pub const fn loan_payment_id(&self) -> Option<LoanPaymentId> {
        self.loan_payment_id
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.created_at
    }
}

fn title(prefix: &str, loan: &Loan) -> String {
    format!("{prefix}{TITLE_SEPARATOR}{}", loan.person())
}
