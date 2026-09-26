//! Caso de uso: listar los movimientos de un periodo.

use std::fmt;
use std::sync::Arc;

use chrono::NaiveDate;
use gastonomo_domain::UserId;
use gastonomo_domain::movement::{Movement, MovementCursor, MovementKind};

use crate::error::{AppError, ValidationErrors};
use crate::port::movement::{MovementFilter, MovementRepository};

/// Filas que se devuelven cuando el cliente no pide un tamano.
///
/// Un mes de una app de finanzas personales son decenas de movimientos, asi que
/// con este valor la pantalla de Inicio y la de Movimientos resuelven el periodo
/// entero en una sola peticion.
pub const DEFAULT_PAGE_LIMIT: u32 = 100;

/// Filas como maximo en una pagina.
///
/// Es generoso a proposito: la exportacion de "Todo el historial" pagina hasta el
/// final y conviene que lo haga en pocas vueltas.
pub const MAX_PAGE_LIMIT: u32 = 500;

/// Lo que pide la pantalla.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ListMovementsQuery {
    /// Primer dia del periodo, inclusive.
    pub from: Option<NaiveDate>,
    /// Ultimo dia del periodo, inclusive.
    pub to: Option<NaiveDate>,
    /// Pills Todos / Ingresos / Gastos.
    pub kind: Option<MovementKind>,
    /// Valor `nextCursor` de la pagina anterior.
    pub cursor: Option<String>,
    /// Filas pedidas. Se acota a [`MAX_PAGE_LIMIT`].
    pub limit: Option<u32>,
}

/// Una pagina de movimientos.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MovementPage {
    /// Movimientos, de mas reciente a mas antiguo.
    pub items: Vec<Movement>,
    /// Cursor de la pagina siguiente, o nada si esta fue la ultima.
    pub next_cursor: Option<MovementCursor>,
}

/// Devuelve los movimientos del periodo, paginados por cursor.
///
/// **No hay endpoint de resumen, y es una decision.** La tarjeta de balance, la
/// tendencia y el grafico de categorias se calculan sobre el valor convertido a la
/// moneda principal, que depende de la moneda que el usuario tenga elegida en ese
/// momento y de las tasas de hoy, y que respeta la tasa que el propio usuario pudo
/// editar al registrar. Ese calculo ya existe en el cliente, que lo necesita para
/// cada fila, para el saldo de cada prestamo y para la exportacion. Repetirlo aqui
/// seria tener dos implementaciones de la regla mas delicada del diseno, y la
/// primera vez que discrepasen el usuario veria una tarjeta de balance que no cuadra
/// con la suma de las filas que tiene debajo. Con una peticion de la lista del
/// periodo el cliente tiene los cuatro datos que la pantalla de Inicio necesita.
#[derive(Clone)]
pub struct ListMovements {
    movements: Arc<dyn MovementRepository>,
}

impl fmt::Debug for ListMovements {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ListMovements").finish_non_exhaustive()
    }
}

impl ListMovements {
    /// Crea el caso de uso sobre su repositorio.
    #[must_use]
    pub const fn new(movements: Arc<dyn MovementRepository>) -> Self {
        Self { movements }
    }

    /// Lee una pagina de movimientos de la cuenta dada.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si el periodo esta al reves o el cursor no
    /// vale, y [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        user_id: UserId,
        query: &ListMovementsQuery,
    ) -> Result<MovementPage, AppError> {
        let page_size = Self::page_size(query.limit);
        let filter = Self::filter(user_id, query, page_size)?;
        let mut items = self.movements.list(&filter).await?;
        Ok(Self::page(&mut items, page_size))
    }

    /// Traduce lo que pidio el cliente a un filtro ya saneado.
    ///
    /// Pide una fila mas de las que va a devolver: si llega, hay pagina siguiente, y
    /// asi no hace falta un `COUNT` del periodo entero para saberlo.
    fn filter(
        user_id: UserId,
        query: &ListMovementsQuery,
        page_size: u32,
    ) -> Result<MovementFilter, AppError> {
        let mut errors = ValidationErrors::new();
        if let (Some(from), Some(to)) = (query.from, query.to)
            && from > to
        {
            errors.push("from", "El inicio del periodo es posterior al final.");
        }
        let after = match query.cursor.as_deref().map(MovementCursor::parse) {
            Some(Ok(cursor)) => Some(cursor),
            None => None,
            Some(Err(_)) => {
                errors.push("cursor", "El cursor no es valido.");
                None
            }
        };
        errors.into_result()?;
        Ok(MovementFilter {
            user_id,
            from: query.from,
            to: query.to,
            kind: query.kind,
            after,
            limit: page_size.saturating_add(1),
        })
    }

    /// Acota el tamano pedido en vez de rechazarlo: `nextCursor` ya le dice al
    /// cliente si la pagina se quedo corta.
    const fn page_size(requested: Option<u32>) -> u32 {
        match requested {
            None | Some(0) => DEFAULT_PAGE_LIMIT,
            Some(limit) if limit > MAX_PAGE_LIMIT => MAX_PAGE_LIMIT,
            Some(limit) => limit,
        }
    }

    /// Corta la fila de sobra que se pidio para saber si hay pagina siguiente.
    fn page(items: &mut Vec<Movement>, limit: u32) -> MovementPage {
        let limit = limit as usize;
        let next_cursor = if items.len() > limit {
            items.truncate(limit);
            items.last().map(Movement::cursor)
        } else {
            None
        };
        MovementPage {
            items: std::mem::take(items),
            next_cursor,
        }
    }
}
