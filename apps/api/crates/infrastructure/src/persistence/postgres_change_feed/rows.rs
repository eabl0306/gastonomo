//! Lectura de las cinco fuentes del feed y armado del lote.
//!
//! Cada fila viaja con su numero de cambio hasta que se sabe donde corta el lote.
//! Los mapeos al dominio son los de cada contexto, no una copia: un movimiento del
//! feed es el mismo movimiento que devuelve `GET /v1/movements`.

use gastonomo_application::error::AppError;
use gastonomo_application::port::changes::{ChangeBatch, ChangeNumber};
use gastonomo_domain::loan::LoanPayment;
use gastonomo_domain::value_object::UserId;
use sqlx::Row as _;
use sqlx::postgres::PgRow;
use sqlx::{Postgres, Transaction};
use uuid::Uuid;

use super::statements;
use crate::persistence::mapping::database_error;
use crate::persistence::{
    postgres_category_repository::row as category_row, postgres_loan_repository::rows as loan_rows,
    postgres_movement_repository::row as movement_row, postgres_user_settings_repository,
};

/// Una fila con el numero de cambio que le toca.
pub struct Numbered {
    seq: ChangeNumber,
    row: PgRow,
}

impl Numbered {
    /// Lee una columna de numero de cambio.
    pub fn column(row: &PgRow, name: &str) -> Result<ChangeNumber, AppError> {
        row.try_get::<ChangeNumber, _>(name)
            .map_err(|error| database_error(&format!("could not read column {name}"), &error))
    }

    fn of(rows: Vec<PgRow>) -> Result<Vec<Self>, AppError> {
        rows.into_iter()
            .map(|row| {
                let seq = Self::column(&row, "change_seq")?;
                Ok(Self { seq, row })
            })
            .collect()
    }
}

/// Las cinco consultas del lote, todavia sin cortar.
pub struct Sources {
    movements: Vec<Numbered>,
    loans: Vec<Numbered>,
    payments: Vec<Numbered>,
    categories: Vec<Numbered>,
    settings: Vec<Numbered>,
}

impl Sources {
    /// Lee de las cinco tablas una fila mas de las que caben en el lote.
    pub async fn read(
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        since: Option<ChangeNumber>,
        limit: u32,
    ) -> Result<Self, AppError> {
        let window = i64::from(limit).saturating_add(1);
        Ok(Self {
            movements: page(transaction, statements::MOVEMENTS, user_id, since, window).await?,
            loans: page(transaction, statements::LOANS, user_id, since, window).await?,
            payments: page(transaction, statements::PAYMENTS, user_id, since, window).await?,
            categories: page(transaction, statements::CATEGORIES, user_id, since, window).await?,
            // Los ajustes son una sola fila: no hay nada que acotar.
            settings: Numbered::of(
                sqlx::query(statements::SETTINGS)
                    .bind(user_id.as_uuid())
                    .bind(since)
                    .fetch_all(&mut **transaction)
                    .await
                    .map_err(|error| {
                        database_error("could not read the settings change", &error)
                    })?,
            )?,
        })
    }

    /// Todos los numeros leidos, para decidir donde corta el lote.
    pub fn numbers(&self) -> Vec<ChangeNumber> {
        [
            &self.movements,
            &self.loans,
            &self.payments,
            &self.categories,
            &self.settings,
        ]
        .iter()
        .flat_map(|group| group.iter().map(|entry| entry.seq))
        .collect()
    }

    /// Arma el lote con lo que cae por debajo del cursor.
    pub async fn batch(
        self,
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        cursor: ChangeNumber,
        has_more: bool,
    ) -> Result<ChangeBatch, AppError> {
        let loans = take(self.loans, cursor);
        let loan_ids = identifiers(&loans)?;
        let payments_of_loans = payments_of(transaction, user_id, &loan_ids).await?;
        Ok(ChangeBatch {
            movements: map(&take(self.movements, cursor), movement_row::movement)?,
            loans: loans
                .iter()
                .zip(loan_ids.iter())
                .map(|(entry, id)| loan_rows::loan(&entry.row, payments_of_loans.of(*id)))
                .collect::<Result<_, _>>()?,
            payments: map(&take(self.payments, cursor), loan_rows::payment)?,
            categories: map(
                &take(self.categories, cursor),
                category_row::category_with_usage,
            )?,
            settings: take(self.settings, cursor)
                .first()
                .map(|entry| postgres_user_settings_repository::settings(&entry.row))
                .transpose()?,
            cursor,
            has_more,
        })
    }
}

async fn page(
    transaction: &mut Transaction<'_, Postgres>,
    query: &'static str,
    user_id: UserId,
    since: Option<ChangeNumber>,
    limit: i64,
) -> Result<Vec<Numbered>, AppError> {
    let rows = sqlx::query(query)
        .bind(user_id.as_uuid())
        .bind(since)
        .bind(limit)
        .fetch_all(&mut **transaction)
        .await
        .map_err(|error| database_error("could not read the change feed", &error))?;
    Numbered::of(rows)
}

fn take(entries: Vec<Numbered>, cursor: ChangeNumber) -> Vec<Numbered> {
    entries
        .into_iter()
        .filter(|entry| entry.seq <= cursor)
        .collect()
}

fn map<T>(
    entries: &[Numbered],
    mapper: impl Fn(&PgRow) -> Result<T, AppError>,
) -> Result<Vec<T>, AppError> {
    entries.iter().map(|entry| mapper(&entry.row)).collect()
}

fn identifiers(entries: &[Numbered]) -> Result<Vec<Uuid>, AppError> {
    entries
        .iter()
        .map(|entry| {
            entry
                .row
                .try_get::<Uuid, _>("id")
                .map_err(|error| database_error("could not read column id", &error))
        })
        .collect()
}

/// Abonos agrupados por prestamo, para reconstruir cada uno con los suyos.
struct PaymentsByLoan(Vec<(Uuid, LoanPayment)>);

impl PaymentsByLoan {
    fn of(&self, loan_id: Uuid) -> Vec<LoanPayment> {
        self.0
            .iter()
            .filter(|(id, _)| *id == loan_id)
            .map(|(_, payment)| payment.clone())
            .collect()
    }
}

async fn payments_of(
    transaction: &mut Transaction<'_, Postgres>,
    user_id: UserId,
    loan_ids: &[Uuid],
) -> Result<PaymentsByLoan, AppError> {
    if loan_ids.is_empty() {
        return Ok(PaymentsByLoan(Vec::new()));
    }
    let rows = sqlx::query(statements::PAYMENTS_OF_LOANS)
        .bind(user_id.as_uuid())
        .bind(loan_ids)
        .fetch_all(&mut **transaction)
        .await
        .map_err(|error| database_error("could not read the payments of the loans", &error))?;
    rows.iter()
        .map(|row| {
            let loan_id = row
                .try_get::<Uuid, _>("loan_id")
                .map_err(|error| database_error("could not read column loan_id", &error))?;
            Ok((loan_id, loan_rows::payment(row)?))
        })
        .collect::<Result<Vec<_>, AppError>>()
        .map(PaymentsByLoan)
}
