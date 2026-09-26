//! Abonos de un prestamo.

use std::fmt;

use chrono::{DateTime, NaiveDate, Utc};
use uuid::Uuid;

use super::agreement::LoanId;
use crate::value_object::{MonetaryAmount, UserId};

/// Longitud maxima de la etiqueta del abono, la que el prototipo rellena con
/// "Detalles" o "Intereses".
pub const MAX_PAYMENT_LABEL_LENGTH: usize = 40;

/// Identificador de un abono.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct LoanPaymentId(Uuid);

impl LoanPaymentId {
    /// Genera un identificador nuevo.
    #[must_use]
    pub fn generate() -> Self {
        Self(Uuid::new_v4())
    }

    /// Envuelve un UUID ya existente, por ejemplo el de una fila.
    #[must_use]
    pub const fn from_uuid(value: Uuid) -> Self {
        Self(value)
    }

    /// UUID subyacente.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

impl fmt::Display for LoanPaymentId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Datos con los que nace un abono, y con los que se rehidrata desde una fila.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewLoanPayment {
    /// Prestamo al que pertenece.
    pub loan_id: LoanId,
    /// Cuenta a la que pertenece, repetida para poder filtrar sin join.
    pub user_id: UserId,
    /// Fecha del abono, sin hora ni zona.
    pub paid_on: NaiveDate,
    /// Concepto opcional que el historial ensena como "Abono - {concepto}".
    pub label: Option<String>,
    /// Importe con sus cuatro campos.
    pub amount: MonetaryAmount,
    /// Instante tecnico de creacion.
    pub created_at: DateTime<Utc>,
}

/// Abono de un prestamo.
///
/// Un abono puede pasarse del saldo: el diseno lo contempla y ensena el exceso
/// como "Intereses ganados", asi que el dominio no lo limita.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LoanPayment {
    id: LoanPaymentId,
    data: NewLoanPayment,
}

impl LoanPayment {
    /// Registra un abono nuevo.
    #[must_use]
    pub fn record(data: NewLoanPayment) -> Self {
        Self {
            id: LoanPaymentId::generate(),
            data,
        }
    }

    /// Reconstruye un abono ya guardado.
    #[must_use]
    pub const fn rehydrate(id: LoanPaymentId, data: NewLoanPayment) -> Self {
        Self { id, data }
    }

    /// Identificador.
    #[must_use]
    pub const fn id(&self) -> LoanPaymentId {
        self.id
    }

    /// Prestamo al que pertenece.
    #[must_use]
    pub const fn loan_id(&self) -> LoanId {
        self.data.loan_id
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.data.user_id
    }

    /// Fecha del abono.
    #[must_use]
    pub const fn paid_on(&self) -> NaiveDate {
        self.data.paid_on
    }

    /// Concepto opcional.
    #[must_use]
    pub fn label(&self) -> Option<&str> {
        self.data.label.as_deref()
    }

    /// Importe con sus cuatro campos.
    #[must_use]
    pub const fn amount(&self) -> &MonetaryAmount {
        &self.data.amount
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.data.created_at
    }
}
