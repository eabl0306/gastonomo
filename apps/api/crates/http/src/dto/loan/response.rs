//! Respuestas de prestamos y abonos.

use gastonomo_domain::loan::{Loan, LoanPayment};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use crate::serialization::{date_to_string, timestamp_to_string};

/// Un abono, tal y como lo pinta el historial del detalle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct LoanPaymentDto {
    /// Identificador del abono.
    pub id: String,
    /// Prestamo al que pertenece.
    pub loan_id: String,
    /// Fecha del abono.
    #[schema(example = "2026-07-29")]
    pub paid_on: String,
    /// Concepto, si el usuario escribio uno.
    #[schema(example = "Intereses")]
    pub label: Option<String>,
    /// Importe tecleado, como string.
    #[schema(example = "200000.0000")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia guardada en la moneda principal del momento.
    #[schema(example = "200000.0000")]
    pub base: String,
    /// Moneda de la equivalencia.
    #[schema(example = "COP")]
    pub base_currency: String,
    /// Instante de creacion.
    #[schema(example = "2026-07-29T10:00:00Z")]
    pub created_at: String,
}

impl From<&LoanPayment> for LoanPaymentDto {
    fn from(payment: &LoanPayment) -> Self {
        let amount = payment.amount();
        Self {
            id: payment.id().to_string(),
            loan_id: payment.loan_id().to_string(),
            paid_on: date_to_string(payment.paid_on()),
            label: payment.label().map(ToOwned::to_owned),
            amount: amount.amount().to_storage_string(),
            currency: amount.currency().as_str().to_owned(),
            base: amount.base().to_storage_string(),
            base_currency: amount.base_currency().as_str().to_owned(),
            created_at: timestamp_to_string(payment.created_at()),
        }
    }
}

/// Un prestamo con sus abonos dentro.
///
/// No trae saldo, ni estado, ni porcentaje, ni intereses ganados: todo eso depende
/// de la moneda principal que el usuario tenga puesta y de las tasas del momento,
/// asi que lo calcula el cliente.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct LoanDto {
    /// Identificador del prestamo.
    pub id: String,
    /// Persona del prestamo.
    #[schema(example = "Rosa")]
    pub person: String,
    /// Nota libre, vacia si no hay.
    #[schema(example = "Teléfono")]
    pub note: String,
    /// `lent` si preste, `borrowed` si me prestaron.
    #[schema(example = "lent")]
    pub direction: String,
    /// Fecha del prestamo.
    #[schema(example = "2026-06-28")]
    pub opened_on: String,
    /// Importe tecleado, como string.
    #[schema(example = "600000.0000")]
    pub amount: String,
    /// Moneda del importe tecleado.
    #[schema(example = "COP")]
    pub currency: String,
    /// Equivalencia guardada en la moneda principal del momento.
    #[schema(example = "600000.0000")]
    pub base: String,
    /// Moneda de la equivalencia.
    #[schema(example = "COP")]
    pub base_currency: String,
    /// Abonos, del mas reciente al mas antiguo.
    pub payments: Vec<LoanPaymentDto>,
    /// Instante de creacion.
    #[schema(example = "2026-06-28T10:00:00Z")]
    pub created_at: String,
    /// Instante de la ultima modificacion del prestamo.
    #[schema(example = "2026-06-28T10:00:00Z")]
    pub updated_at: String,
}

impl From<&Loan> for LoanDto {
    fn from(loan: &Loan) -> Self {
        let amount = loan.amount();
        Self {
            id: loan.id().to_string(),
            person: loan.person().as_str().to_owned(),
            note: loan.note().to_owned(),
            direction: loan.direction().as_str().to_owned(),
            opened_on: date_to_string(loan.opened_on()),
            amount: amount.amount().to_storage_string(),
            currency: amount.currency().as_str().to_owned(),
            base: amount.base().to_storage_string(),
            base_currency: amount.base_currency().as_str().to_owned(),
            payments: loan.payments().iter().map(LoanPaymentDto::from).collect(),
            created_at: timestamp_to_string(loan.created_at()),
            updated_at: timestamp_to_string(loan.updated_at()),
        }
    }
}

/// Respuesta de `GET /v1/loans`.
///
/// Va envuelta en un objeto y no como array suelto para poder crecer sin romper el
/// contrato, por ejemplo con totales o paginacion.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct LoanListDto {
    /// Prestamos de la cuenta.
    pub items: Vec<LoanDto>,
}

impl LoanListDto {
    /// Envuelve los prestamos de una consulta.
    #[must_use]
    pub fn new(loans: &[Loan]) -> Self {
        Self {
            items: loans.iter().map(LoanDto::from).collect(),
        }
    }
}
