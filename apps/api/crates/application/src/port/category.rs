//! Puertos del contexto de categorias.
//!
//! La siembra de las 12 predeterminadas al registrarse no esta aqui: tiene su
//! propio puerto en [`crate::port::UserProvisioning`], porque escribe en dos
//! tablas de dos contextos dentro de la transaccion del registro.

use async_trait::async_trait;
use gastonomo_domain::category::{Category, CategoryId};
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;

/// Una categoria con las veces que la usa un movimiento.
///
/// El conteo alimenta el texto "Sin movimientos / 1 movimiento / N movimientos" de
/// la pantalla de Categorias, asi que viaja con la lista y no en otro endpoint.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CategoryUsage {
    /// La categoria.
    pub category: Category,
    /// Movimientos que la tienen asignada.
    pub movements: u32,
}

/// Lee y escribe las categorias de una cuenta.
///
/// Todos los metodos reciben el `user_id` del token y filtran por el: una
/// categoria de otra cuenta se comporta como si no existiera.
#[async_trait]
pub trait CategoryRepository: Send + Sync {
    /// Lista las categorias de la cuenta con su conteo de uso.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn list_with_usage(&self, user_id: UserId) -> Result<Vec<CategoryUsage>, AppError>;

    /// Busca una categoria de la cuenta con su conteo de uso.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<CategoryUsage>, AppError>;

    /// Busca una categoria de la cuenta por nombre, sin distinguir mayusculas.
    ///
    /// Sirve para resolver "Otros", que es donde acaban los movimientos de una
    /// categoria borrada.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find_by_name(&self, user_id: UserId, name: &str)
    -> Result<Option<Category>, AppError>;

    /// Guarda una categoria nueva.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Conflict`] con codigo `category_name_taken` si el
    /// nombre ya existe en la cuenta, y [`AppError::Dependency`] si falla la base
    /// de datos.
    async fn insert(&self, category: &Category) -> Result<(), AppError>;

    /// Guarda los cambios de una categoria que ya existe.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Conflict`] con codigo `category_name_taken` si el
    /// nombre choca con otra, [`AppError::NotFound`] si la categoria ya no esta y
    /// [`AppError::Dependency`] si falla la base de datos.
    async fn update(&self, category: &Category) -> Result<(), AppError>;

    /// Reasigna los movimientos de una categoria y despues la borra.
    ///
    /// Las dos escrituras van en la misma transaccion: si el borrado falla, los
    /// movimientos no pueden quedarse reasignados a una categoria que sigue viva.
    /// Devuelve cuantos movimientos cambiaron de categoria.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la categoria ya no existe o es de otra
    /// cuenta, [`AppError::Conflict`] si resulto ser una predeterminada y
    /// [`AppError::Dependency`] si falla la base de datos.
    async fn delete_reassigning_movements(
        &self,
        user_id: UserId,
        category_id: CategoryId,
        fallback_id: CategoryId,
    ) -> Result<u32, AppError>;
}
