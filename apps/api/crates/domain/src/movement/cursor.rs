//! Cursor de paginacion de la lista de movimientos.

use std::fmt;

use chrono::NaiveDate;
use thiserror::Error;

use super::id::MovementId;
use super::record_date::{RECORD_DATE_FORMAT, parse_record_date};

/// Separador entre la fecha y el identificador.
const SEPARATOR: char = '_';

/// El texto recibido no es un cursor valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a valid cursor")]
pub struct MovementCursorError {
    /// Valor recibido.
    pub got: String,
}

/// Posicion exacta desde la que sigue la pagina siguiente.
///
/// La lista se ordena por `(occurred_on DESC, id DESC)`, que es el orden en que la
/// pantalla agrupa por dia. Paginar por desplazamiento se rompe en cuanto el
/// usuario guarda un movimiento mientras baja por la lista: las filas se corren y
/// alguna se repite o se pierde. Con la clave del ultimo elemento leido eso no
/// pasa, porque la condicion es siempre "estrictamente anterior a este par".
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MovementCursor {
    occurred_on: NaiveDate,
    id: MovementId,
}

impl MovementCursor {
    /// Crea el cursor que apunta al ultimo elemento devuelto.
    #[must_use]
    pub const fn new(occurred_on: NaiveDate, id: MovementId) -> Self {
        Self { occurred_on, id }
    }

    /// Lee el cursor del valor que devolvio la pagina anterior.
    ///
    /// El formato es opaco a proposito: el cliente lo copia tal cual y no lo
    /// construye.
    ///
    /// # Errors
    ///
    /// Devuelve [`MovementCursorError`] si el texto no tiene la forma esperada.
    pub fn parse(raw: &str) -> Result<Self, MovementCursorError> {
        let trimmed = raw.trim();
        let invalid = || MovementCursorError {
            got: trimmed.to_owned(),
        };
        let (date, id) = trimmed.split_once(SEPARATOR).ok_or_else(invalid)?;
        Ok(Self {
            occurred_on: parse_record_date(date).map_err(|_| invalid())?,
            id: MovementId::parse(id).map_err(|_| invalid())?,
        })
    }

    /// Fecha del ultimo elemento de la pagina anterior.
    #[must_use]
    pub const fn occurred_on(self) -> NaiveDate {
        self.occurred_on
    }

    /// Identificador del ultimo elemento de la pagina anterior.
    #[must_use]
    pub const fn id(self) -> MovementId {
        self.id
    }
}

impl fmt::Display for MovementCursor {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{}{SEPARATOR}{}",
            self.occurred_on.format(RECORD_DATE_FORMAT),
            self.id
        )
    }
}
