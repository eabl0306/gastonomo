//! Movimientos: ingresos y gastos.
//!
//! El agregado guarda lo que el usuario tecleo y su equivalencia en la moneda
//! principal del momento, en un [`crate::value_object::MonetaryAmount`], mas la
//! categoria completa: la fila de la pantalla de Movimientos pinta el tile con el
//! color y el icono de la categoria, asi que viajan con el movimiento y no en una
//! segunda peticion.
//!
//! El titulo sale de la nota, y si no hay nota es el nombre de la categoria. Esa
//! regla es del diseno y vive en [`MovementTitle`].

mod category;
mod cursor;
mod id;
mod kind;
mod record;
mod record_date;
mod title;

pub use category::{MovementCategory, MovementCategoryError};
// El identificador de categoria es uno solo en todo el dominio: vive en
// `crate::category` y se reexporta aqui porque un movimiento se escribe con el.
// Tenerlo duplicado permitia que dos tipos distintos significaran lo mismo.
pub use crate::category::{CategoryId, CategoryIdError};
pub use cursor::{MovementCursor, MovementCursorError};
pub use id::{MovementId, MovementIdError};
pub use kind::{MovementKind, MovementKindError};
pub use record::{Movement, MovementError, NewMovement};
pub use record_date::{
    FUTURE_TOLERANCE_DAYS, RECORD_DATE_FORMAT, RecordDateError, earliest_record_date,
    parse_record_date, validate_record_date,
};
pub use title::{MAX_TITLE_LENGTH, MovementTitle, MovementTitleError};
