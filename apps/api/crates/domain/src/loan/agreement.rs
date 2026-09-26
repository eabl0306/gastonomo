//! El prestamo en si, con sus abonos embebidos.

use std::fmt;

use chrono::{DateTime, NaiveDate, Utc};
use thiserror::Error;
use uuid::Uuid;

use super::direction::LoanDirection;
use super::payment::LoanPayment;
use crate::value_object::{MonetaryAmount, PersonName, UserId};

/// Longitud maxima de la nota, la del campo con placeholder "Ej. Telefono".
pub const MAX_LOAN_NOTE_LENGTH: usize = 120;

/// El texto recibido no es el identificador de un prestamo.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a valid loan identifier")]
pub struct LoanIdError {
    /// Valor recibido.
    pub got: String,
}

/// Identificador de un prestamo.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct LoanId(Uuid);

impl LoanId {
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

    /// Lee un identificador de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`LoanIdError`] si el texto no es un UUID.
    pub fn parse(raw: &str) -> Result<Self, LoanIdError> {
        Uuid::parse_str(raw.trim())
            .map(Self)
            .map_err(|_| LoanIdError {
                got: raw.trim().to_owned(),
            })
    }

    /// UUID subyacente, que es lo que espera el adaptador de persistencia.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

impl fmt::Display for LoanId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Datos con los que nace un prestamo, y con los que se rehidrata desde una fila.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewLoan {
    /// Cuenta a la que pertenece.
    pub user_id: UserId,
    /// Persona del prestamo.
    pub person: PersonName,
    /// Nota libre. Cadena vacia si el usuario no escribio nada.
    pub note: String,
    /// Preste o me prestaron.
    pub direction: LoanDirection,
    /// Fecha del prestamo, sin hora ni zona.
    pub opened_on: NaiveDate,
    /// Importe con sus cuatro campos.
    pub amount: MonetaryAmount,
    /// Instante tecnico de creacion.
    pub created_at: DateTime<Utc>,
}

/// Prestamo con sus abonos.
///
/// El saldo, el estado y el porcentaje no estan aqui porque no se guardan: los
/// deriva el cliente con las tasas vigentes y su moneda principal del momento.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Loan {
    id: LoanId,
    data: NewLoan,
    payments: Vec<LoanPayment>,
    updated_at: DateTime<Utc>,
}

impl Loan {
    /// Abre un prestamo nuevo, todavia sin abonos.
    #[must_use]
    pub fn open(data: NewLoan) -> Self {
        let updated_at = data.created_at;
        Self {
            id: LoanId::generate(),
            data,
            payments: Vec::new(),
            updated_at,
        }
    }

    /// Reconstruye un prestamo ya guardado.
    #[must_use]
    pub const fn rehydrate(
        id: LoanId,
        data: NewLoan,
        payments: Vec<LoanPayment>,
        updated_at: DateTime<Utc>,
    ) -> Self {
        Self {
            id,
            data,
            payments,
            updated_at,
        }
    }

    /// Identificador.
    #[must_use]
    pub const fn id(&self) -> LoanId {
        self.id
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.data.user_id
    }

    /// Persona del prestamo.
    #[must_use]
    pub const fn person(&self) -> &PersonName {
        &self.data.person
    }

    /// Nota libre, vacia si no hay.
    #[must_use]
    pub fn note(&self) -> &str {
        &self.data.note
    }

    /// Preste o me prestaron.
    #[must_use]
    pub const fn direction(&self) -> LoanDirection {
        self.data.direction
    }

    /// Fecha del prestamo.
    #[must_use]
    pub const fn opened_on(&self) -> NaiveDate {
        self.data.opened_on
    }

    /// Importe con sus cuatro campos.
    #[must_use]
    pub const fn amount(&self) -> &MonetaryAmount {
        &self.data.amount
    }

    /// Abonos, del mas reciente al mas antiguo, como los pinta el historial.
    #[must_use]
    pub fn payments(&self) -> &[LoanPayment] {
        &self.payments
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.data.created_at
    }

    /// Instante de la ultima modificacion del prestamo.
    ///
    /// Un abono nuevo no lo mueve: el abono es una fila propia con su propio
    /// instante de creacion.
    #[must_use]
    pub const fn updated_at(&self) -> DateTime<Utc> {
        self.updated_at
    }

    /// Devuelve el prestamo con un abono mas, el primero de la lista.
    ///
    /// Se usa para responder al abono recien registrado sin volver a leer de la
    /// base de datos, y mantiene el orden del historial del diseno.
    #[must_use]
    pub fn with_payment(mut self, payment: LoanPayment) -> Self {
        self.payments.insert(0, payment);
        self
    }
}
