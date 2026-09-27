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
    /// Con `include_archived` en false solo devuelve las activas, que es lo que
    /// pinta la pantalla. Las archivadas se piden a proposito, para resolver la
    /// categoria de un movimiento viejo o para recuperarla.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn list_with_usage(
        &self,
        user_id: UserId,
        include_archived: bool,
    ) -> Result<Vec<CategoryUsage>, AppError>;

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
    /// El nombre es unico incluyendo las archivadas, asi que esto encuentra una
    /// archivada igual que una activa. Es deliberado: el nombre tiene que seguir
    /// resolviendo a la categoria que lo llevaba.
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

    /// Guarda los cambios de una categoria que ya existe, incluido si esta
    /// archivada.
    ///
    /// Archivar y desarchivar pasan por aqui y no por un metodo propio: son un
    /// campo mas del agregado, y un solo camino de escritura es un sitio menos
    /// donde olvidarse de algo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Conflict`] con codigo `category_name_taken` si el
    /// nombre choca con otra, [`AppError::NotFound`] si la categoria ya no esta y
    /// [`AppError::Dependency`] si falla la base de datos.
    async fn update(&self, category: &Category) -> Result<(), AppError>;

    /// Borra de verdad una categoria **sin movimientos**.
    ///
    /// Es el unico borrado que queda, y solo porque una categoria que nadie uso no
    /// tiene historico que proteger. Quien llama comprueba el conteo antes: este
    /// metodo no reasigna nada ni puede hacerlo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la categoria ya no existe o es de otra
    /// cuenta, y [`AppError::Dependency`] si falla la base de datos.
    async fn delete_unused(&self, user_id: UserId, category_id: CategoryId)
    -> Result<(), AppError>;
}
