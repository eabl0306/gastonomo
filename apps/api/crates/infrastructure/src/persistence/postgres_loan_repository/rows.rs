//! Traduccion de filas de `loans` y `loan_payments` al dominio.
//!
//! Una fila que el dominio rechaza no es culpa del cliente: es una fila corrupta o
//! un esquema movido. Por eso se traduce a [`AppError::Dependency`] y no a un error
//! de validacion, igual que en [`super::super::mapping`].

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_application::error::AppError;
use gastonomo_domain::loan::{
    Loan, LoanDirection, LoanId, LoanPayment, LoanPaymentId, NewLoan, NewLoanPayment,
};
use gastonomo_domain::{CurrencyCode, MonetaryAmount, Money, PersonName, UserId};
use rust_decimal::Decimal;
use sqlx::Row as _;
use sqlx::postgres::PgRow;
use uuid::Uuid;

use super::super::mapping::database_error;

fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

fn read<'r, T>(row: &'r PgRow, column: &str) -> Result<T, AppError>
where
    T: sqlx::Decode<'r, sqlx::Postgres> + sqlx::Type<sqlx::Postgres>,
{
    row.try_get::<T, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

/// Reconstruye el par de importes de una fila.
fn amount(row: &PgRow) -> Result<MonetaryAmount, AppError> {
    let amount = read_money(row, "amount")?;
    let currency = read_currency(row, "currency")?;
    let base = read_money(row, "base")?;
    let base_currency = read_currency(row, "base_currency")?;
    MonetaryAmount::new(amount, currency, base, base_currency)
        .map_err(|error| corrupt("amount", &error.to_string()))
}

fn read_money(row: &PgRow, column: &str) -> Result<Money, AppError> {
    let raw: Decimal = read(row, column)?;
    Money::from_decimal(raw).map_err(|error| corrupt(column, &error.to_string()))
}

fn read_currency(row: &PgRow, column: &str) -> Result<CurrencyCode, AppError> {
    let raw: String = read(row, column)?;
    CurrencyCode::parse(&raw).map_err(|error| corrupt(column, &error.to_string()))
}

/// Reconstruye un prestamo con los abonos que ya se leyeron.
pub fn loan(row: &PgRow, payments: Vec<LoanPayment>) -> Result<Loan, AppError> {
    let person = PersonName::parse(&read::<String>(row, "person")?)
        .map_err(|error| corrupt("person", &error.to_string()))?;
    let direction = LoanDirection::parse(&read::<String>(row, "direction")?)
        .map_err(|error| corrupt("direction", &error.to_string()))?;

    Ok(Loan::rehydrate(
        LoanId::from_uuid(read::<Uuid>(row, "id")?),
        NewLoan {
            user_id: UserId::from_uuid(read::<Uuid>(row, "user_id")?),
            person,
            note: read::<String>(row, "note")?,
            direction,
            opened_on: read::<NaiveDate>(row, "opened_on")?,
            amount: amount(row)?,
            created_at: read::<DateTime<Utc>>(row, "created_at")?,
        },
        payments,
        read::<DateTime<Utc>>(row, "updated_at")?,
    ))
}

/// Reconstruye un abono.
pub fn payment(row: &PgRow) -> Result<LoanPayment, AppError> {
    Ok(LoanPayment::rehydrate(
        LoanPaymentId::from_uuid(read::<Uuid>(row, "id")?),
        NewLoanPayment {
            loan_id: LoanId::from_uuid(read::<Uuid>(row, "loan_id")?),
            user_id: UserId::from_uuid(read::<Uuid>(row, "user_id")?),
            paid_on: read::<NaiveDate>(row, "paid_on")?,
            label: read::<Option<String>>(row, "label")?,
            amount: amount(row)?,
            created_at: read::<DateTime<Utc>>(row, "created_at")?,
        },
    ))
}
