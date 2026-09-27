//! Puertos del contexto de movimientos.

use async_trait::async_trait;
use chrono::NaiveDate;
use gastonomo_domain::UserId;
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementCursor, MovementKind,
};

use crate::error::AppError;

/// Lo que la lista de movimientos pide a la base de datos.
///
/// `from` y `to` son inclusive y salen del filtro de periodo de Inicio y de
/// Movimientos. `kind` corresponde a las pills Todos / Ingresos / Gastos. `after`
/// es la posicion desde la que sigue la pagina, y `limit` viene ya acotado por el
/// caso de uso: el adaptador no decide cuantas filas caben.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MovementFilter {
    /// Cuenta del token. Toda consulta filtra por ella.
    pub user_id: UserId,
    /// Primer dia del periodo, inclusive.
    pub from: Option<NaiveDate>,
    /// Ultimo dia del periodo, inclusive.
    pub to: Option<NaiveDate>,
    /// Solo ingresos, solo gastos, o todo si va vacio.
    pub kind: Option<MovementKind>,
    /// Ultimo elemento de la pagina anterior.
    pub after: Option<MovementCursor>,
    /// Filas como maximo.
    pub limit: u32,
}

/// Lee y escribe movimientos.
#[async_trait]
pub trait MovementRepository: Send + Sync {
    /// Devuelve los movimientos del filtro, de mas reciente a mas antiguo.
    ///
    /// El orden es `(occurred_on DESC, id DESC)`, que es el que pinta la pantalla
    /// agrupada por dia y el que hace estable el cursor.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn list(&self, filter: &MovementFilter) -> Result<Vec<Movement>, AppError>;

    /// Guarda un movimiento nuevo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn insert(&self, movement: &Movement) -> Result<(), AppError>;
}

/// Lee la categoria que un movimiento nuevo dice usar.
///
/// Es un puerto propio y no el repositorio de categorias porque el contexto de
/// movimientos solo necesita una cosa: la categoria completa de este usuario, o
/// nada. Devolver `None` tanto si no existe como si es de otro usuario evita que la
/// respuesta sirva de oraculo.
#[async_trait]
pub trait MovementCategoryReader: Send + Sync {
    /// Busca una categoria de la cuenta dada.
    ///
    /// **Encuentra tambien las archivadas, a proposito.** Archivada quiere decir
    /// "no se ofrece", no "se rechaza lo que la mencione". Si esto filtrara, un
    /// movimiento que el cliente dejo en la cola sin conexion y cuya categoria se
    /// archivo despues nunca podria subirse, y no hay pantalla para arreglarlo: el
    /// historico es de solo anadir. El movimiento lleva su categoria incrustada, asi
    /// que se lee igual de bien.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<MovementCategory>, AppError>;
}
