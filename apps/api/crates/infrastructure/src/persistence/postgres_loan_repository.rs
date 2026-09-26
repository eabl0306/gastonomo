//! Adaptador driven: prestamos y abonos en PostgreSQL.
//!
//! Aqui vive la parte delicada del contexto: crear un prestamo escribe dos filas
//! (el prestamo y su movimiento) y registrar un abono tambien (el abono y su
//! movimiento). Las dos van en la misma transaccion, asi que si la segunda falla la
//! primera no queda. Sin eso el balance de Inicio saldria mal y el usuario no
//! tendria como arreglarlo: el diseno no tiene pantalla de edicion.

mod rows;
mod statements;

use std::collections::HashMap;

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::loan::LoanRepository;
use gastonomo_domain::UserId;
use gastonomo_domain::loan::{
    GeneratedMovement, Loan, LoanDirection, LoanId, LoanPayment, LoanPaymentId,
};
use sqlx::postgres::PgRow;
use sqlx::{PgPool, Postgres, Row as _, Transaction};
use uuid::Uuid;

use super::mapping::database_error;

/// Repositorio de prestamos y abonos.
#[derive(Debug, Clone)]
pub struct PostgresLoanRepository {
    pool: PgPool,
}

impl PostgresLoanRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    /// Lee los abonos de los prestamos dados y los agrupa por prestamo.
    async fn payments_of(
        &self,
        user_id: UserId,
        loan_ids: Vec<Uuid>,
    ) -> Result<HashMap<Uuid, Vec<LoanPayment>>, AppError> {
        let rows = sqlx::query(statements::PAYMENTS_OF_LOANS)
            .bind(user_id.as_uuid())
            .bind(&loan_ids)
            .fetch_all(&self.pool)
            .await
            .map_err(|error| database_error("could not read the loan payments", &error))?;

        let mut grouped: HashMap<Uuid, Vec<LoanPayment>> = HashMap::new();
        for row in &rows {
            let payment = rows::payment(row)?;
            grouped
                .entry(payment.loan_id().as_uuid())
                .or_default()
                .push(payment);
        }
        Ok(grouped)
    }

    /// Pega cada prestamo con sus abonos.
    async fn assemble(&self, user_id: UserId, rows: &[PgRow]) -> Result<Vec<Loan>, AppError> {
        let ids = rows
            .iter()
            .map(|row| {
                row.try_get::<Uuid, _>("id")
                    .map_err(|error| database_error("could not read column id", &error))
            })
            .collect::<Result<Vec<Uuid>, AppError>>()?;
        let mut payments = self.payments_of(user_id, ids).await?;

        rows.iter()
            .map(|row| {
                let id = row
                    .try_get::<Uuid, _>("id")
                    .map_err(|error| database_error("could not read column id", &error))?;
                rows::loan(row, payments.remove(&id).unwrap_or_default())
            })
            .collect()
    }

    /// Resuelve la categoria predeterminada del movimiento generado.
    ///
    /// Se hace dentro de la transaccion. Si la cuenta no la tiene, el error corta la
    /// operacion completa y la fila que ya se habia insertado se deshace.
    async fn category_of(
        transaction: &mut Transaction<'_, Postgres>,
        movement: &GeneratedMovement,
    ) -> Result<Uuid, AppError> {
        let name = movement.category_name();
        sqlx::query(statements::CATEGORY_BY_NAME)
            .bind(movement.user_id().as_uuid())
            .bind(name)
            .fetch_optional(&mut **transaction)
            .await
            .map_err(|error| database_error("could not read the default category", &error))?
            .ok_or_else(|| {
                AppError::dependency(format!(
                    "the account has no default category named {name:?}, \
                     so the generated movement cannot be written"
                ))
            })?
            .try_get::<Uuid, _>("id")
            .map_err(|error| database_error("could not read column id", &error))
    }

    /// Escribe el movimiento que acompana al prestamo o al abono.
    async fn insert_movement(
        transaction: &mut Transaction<'_, Postgres>,
        movement: &GeneratedMovement,
    ) -> Result<(), AppError> {
        let category_id = Self::category_of(transaction, movement).await?;
        let amount = movement.amount();
        sqlx::query(statements::INSERT_MOVEMENT)
            .bind(Uuid::new_v4())
            .bind(movement.user_id().as_uuid())
            .bind(movement.occurred_on())
            .bind(movement.kind().as_str())
            .bind(category_id)
            .bind(movement.title())
            .bind(amount.amount().decimal())
            .bind(amount.currency().as_str())
            .bind(amount.base().decimal())
            .bind(amount.base_currency().as_str())
            .bind(movement.loan_id().as_uuid())
            .bind(movement.loan_payment_id().map(LoanPaymentId::as_uuid))
            .bind(movement.created_at())
            .execute(&mut **transaction)
            .await
            .map(|_| ())
            .map_err(|error| database_error("could not insert the generated movement", &error))
    }
}

#[async_trait]
impl LoanRepository for PostgresLoanRepository {
    async fn list(
        &self,
        user_id: UserId,
        direction: Option<LoanDirection>,
    ) -> Result<Vec<Loan>, AppError> {
        let rows = sqlx::query(statements::LOANS_OF_USER)
            .bind(user_id.as_uuid())
            .bind(direction.map(LoanDirection::as_str))
            .fetch_all(&self.pool)
            .await
            .map_err(|error| database_error("could not read the loans", &error))?;
        self.assemble(user_id, &rows).await
    }

    async fn find(&self, user_id: UserId, loan_id: LoanId) -> Result<Option<Loan>, AppError> {
        let rows = sqlx::query(statements::LOAN_OF_USER)
            .bind(user_id.as_uuid())
            .bind(loan_id.as_uuid())
            .fetch_all(&self.pool)
            .await
            .map_err(|error| database_error("could not read the loan", &error))?;
        Ok(self.assemble(user_id, &rows).await?.into_iter().next())
    }

    async fn create(&self, loan: &Loan, movement: &GeneratedMovement) -> Result<(), AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;

        let amount = loan.amount();
        sqlx::query(statements::INSERT_LOAN)
            .bind(loan.id().as_uuid())
            .bind(loan.user_id().as_uuid())
            .bind(loan.person().as_str())
            .bind(loan.note())
            .bind(loan.direction().as_str())
            .bind(loan.opened_on())
            .bind(amount.amount().decimal())
            .bind(amount.currency().as_str())
            .bind(amount.base().decimal())
            .bind(amount.base_currency().as_str())
            .bind(loan.created_at())
            .execute(&mut *transaction)
            .await
            .map_err(|error| database_error("could not insert the loan", &error))?;

        Self::insert_movement(&mut transaction, movement).await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the loan", &error))
    }

    async fn add_payment(
        &self,
        payment: &LoanPayment,
        movement: &GeneratedMovement,
    ) -> Result<(), AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;

        let amount = payment.amount();
        sqlx::query(statements::INSERT_PAYMENT)
            .bind(payment.id().as_uuid())
            .bind(payment.loan_id().as_uuid())
            .bind(payment.user_id().as_uuid())
            .bind(payment.paid_on())
            .bind(payment.label())
            .bind(amount.amount().decimal())
            .bind(amount.currency().as_str())
            .bind(amount.base().decimal())
            .bind(amount.base_currency().as_str())
            .bind(payment.created_at())
            .execute(&mut *transaction)
            .await
            .map_err(|error| database_error("could not insert the loan payment", &error))?;

        Self::insert_movement(&mut transaction, movement).await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the loan payment", &error))
    }
}
