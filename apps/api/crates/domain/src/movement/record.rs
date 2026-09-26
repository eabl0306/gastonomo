//! El agregado de movimiento.

use chrono::{DateTime, NaiveDate, Utc};
use thiserror::Error;
use uuid::Uuid;

use super::category::MovementCategory;
use super::id::MovementId;
use super::kind::MovementKind;
use super::record_date::{RecordDateError, validate_record_date};
use super::title::MovementTitle;
use crate::category::CategoryKind;
use crate::value_object::{MonetaryAmount, UserId};

/// Motivo por el que un movimiento no se puede registrar.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MovementError {
    /// La categoria elegida no sirve para este tipo de movimiento.
    #[error("a {movement_kind} movement cannot use a {category_kind} category")]
    CategoryKindMismatch {
        /// Tipo del movimiento.
        movement_kind: MovementKind,
        /// Tipo de la categoria elegida.
        category_kind: CategoryKind,
    },
    /// La fecha de registro no cae en la ventana admitida.
    #[error(transparent)]
    RecordDate(#[from] RecordDateError),
}

/// Datos con los que nace un movimiento.
///
/// Los cuatro campos de importe van juntos en un [`MonetaryAmount`], que ya
/// comprueba sus invariantes. `loan_id` y `loan_payment_id` los rellena el
/// contexto de prestamos cuando el movimiento lo genera un prestamo o un abono;
/// desde `POST /v1/movements` siempre llegan vacios.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewMovement {
    /// Cuenta a la que pertenece.
    pub user_id: UserId,
    /// Fecha del registro, sin hora ni zona.
    pub occurred_on: NaiveDate,
    /// Ingreso o gasto.
    pub kind: MovementKind,
    /// Categoria completa, con icono y color para la fila.
    pub category: MovementCategory,
    /// Titulo que ensena la fila.
    pub title: MovementTitle,
    /// Importe tecleado y su equivalencia en la moneda principal.
    pub amount: MonetaryAmount,
    /// Prestamo que lo genero, si lo genero un prestamo.
    pub loan_id: Option<Uuid>,
    /// Abono que lo genero, si lo genero un abono.
    pub loan_payment_id: Option<Uuid>,
    /// Instante tecnico de creacion.
    pub created_at: DateTime<Utc>,
}

/// Un ingreso o un gasto ya registrado.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Movement {
    id: MovementId,
    data: NewMovement,
    updated_at: DateTime<Utc>,
}

impl Movement {
    /// Registra un movimiento nuevo comprobando las reglas del diseno.
    ///
    /// Son dos: la categoria tiene que admitir el tipo del movimiento (el grid de
    /// Nuevo movimiento solo ensena las que valen) y la fecha tiene que caer en la
    /// ventana razonable alrededor de hoy.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementError`] si la categoria no encaja con el tipo o si la
    /// fecha se sale de la ventana.
    pub fn record(data: NewMovement, today: NaiveDate) -> Result<Self, MovementError> {
        validate_record_date(data.occurred_on, today)?;
        let category_kind = data.category.kind();
        if !category_kind.accepts(data.kind.as_category_kind()) {
            return Err(MovementError::CategoryKindMismatch {
                movement_kind: data.kind,
                category_kind,
            });
        }
        let updated_at = data.created_at;
        Ok(Self {
            id: MovementId::generate(),
            data,
            updated_at,
        })
    }

    /// Reconstruye un movimiento ya guardado.
    ///
    /// No revalida la fecha: una fila vieja es legitima aunque hoy no se pudiera
    /// escribir, y rechazarla aqui convertiria un cambio de reglas en datos
    /// ilegibles.
    #[must_use]
    pub const fn rehydrate(id: MovementId, data: NewMovement, updated_at: DateTime<Utc>) -> Self {
        Self {
            id,
            data,
            updated_at,
        }
    }

    /// Identificador.
    #[must_use]
    pub const fn id(&self) -> MovementId {
        self.id
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.data.user_id
    }

    /// Fecha del registro.
    #[must_use]
    pub const fn occurred_on(&self) -> NaiveDate {
        self.data.occurred_on
    }

    /// Ingreso o gasto.
    #[must_use]
    pub const fn kind(&self) -> MovementKind {
        self.data.kind
    }

    /// Categoria completa.
    #[must_use]
    pub const fn category(&self) -> &MovementCategory {
        &self.data.category
    }

    /// Titulo que ensena la fila.
    #[must_use]
    pub const fn title(&self) -> &MovementTitle {
        &self.data.title
    }

    /// Importe tecleado y su equivalencia.
    #[must_use]
    pub const fn amount(&self) -> &MonetaryAmount {
        &self.data.amount
    }

    /// Prestamo que lo genero.
    #[must_use]
    pub const fn loan_id(&self) -> Option<Uuid> {
        self.data.loan_id
    }

    /// Abono que lo genero.
    #[must_use]
    pub const fn loan_payment_id(&self) -> Option<Uuid> {
        self.data.loan_payment_id
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.data.created_at
    }

    /// Instante de la ultima modificacion.
    #[must_use]
    pub const fn updated_at(&self) -> DateTime<Utc> {
        self.updated_at
    }

    /// Cursor que apunta a este movimiento, para pedir la pagina siguiente.
    #[must_use]
    pub const fn cursor(&self) -> super::cursor::MovementCursor {
        super::cursor::MovementCursor::new(self.data.occurred_on, self.id)
    }
}
