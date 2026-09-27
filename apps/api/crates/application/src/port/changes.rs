//! Puerto del feed de cambios.
//!
//! El historico es de solo anadir, asi que "cambios" es casi siempre "cosas
//! creadas despues de esta marca". Las tres excepciones que si mutan son las
//! categorias (renombrar, archivar y recuperar) y los ajustes. Las tasas de cambio
//! no entran: tienen su propio endpoint y cambian a diario para todos.

use async_trait::async_trait;
use gastonomo_domain::loan::{Loan, LoanPayment};
use gastonomo_domain::movement::Movement;
use gastonomo_domain::settings::UserSettings;
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::category::CategoryUsage;

/// Numero de cambio de una cuenta.
///
/// Es un contador por usuario, no una marca de tiempo: dos filas creadas en el
/// mismo milisegundo, o un reloj que se ajusta hacia atras, harian que el cliente
/// se saltara registros sin enterarse. Viaja al cliente **opaco**, y quien lo hace
/// opaco es la capa HTTP: aqui dentro es un numero y se compara como tal.
pub type ChangeNumber = i64;

/// Un lote de cambios de una cuenta.
///
/// Un abono puede llegar dos veces en el mismo lote, dentro de su prestamo y
/// tambien en [`Self::payments`], si los dos cambiaron en el rango. No es un
/// problema: todo lo del lote se guarda por identificador, asi que guardarlo dos
/// veces deja lo mismo que guardarlo una.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChangeBatch {
    /// Movimientos nuevos.
    pub movements: Vec<Movement>,
    /// Prestamos nuevos, con sus abonos.
    pub loans: Vec<Loan>,
    /// Abonos cuyo prestamo puede ser mas viejo que el lote.
    pub payments: Vec<LoanPayment>,
    /// Categorias nuevas, renombradas, archivadas o recuperadas, con su conteo de
    /// uso: es el mismo DTO que `GET /v1/categories`, asi que el cliente tiene un
    /// solo mapeo por tipo y no dos que se puedan desincronizar.
    pub categories: Vec<CategoryUsage>,
    /// Los ajustes, solo si cambiaron.
    pub settings: Option<UserSettings>,
    /// Numero al que llega este lote. El cliente lo guarda **despues** de escribir.
    pub cursor: ChangeNumber,
    /// True si quedan cambios por encima de [`Self::cursor`].
    pub has_more: bool,
}

impl ChangeBatch {
    /// Un lote sin nada, que es lo que recibe un cliente ya al dia.
    #[must_use]
    pub const fn empty(cursor: ChangeNumber) -> Self {
        Self {
            movements: Vec::new(),
            loans: Vec::new(),
            payments: Vec::new(),
            categories: Vec::new(),
            settings: None,
            cursor,
            has_more: false,
        }
    }
}

/// Lee los cambios de una cuenta a partir de un numero de cambio.
#[async_trait]
pub trait ChangeFeedReader: Send + Sync {
    /// Devuelve hasta `limit` cambios con numero mayor que `since`.
    ///
    /// Con `since` en `None` devuelve desde el principio, que es el arranque en
    /// frio. El lote es un tramo cerrado del orden total de la cuenta: todo lo que
    /// tenga numero menor o igual que el cursor devuelto esta dentro.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn changes_since(
        &self,
        user_id: UserId,
        since: Option<ChangeNumber>,
        limit: u32,
    ) -> Result<ChangeBatch, AppError>;
}
