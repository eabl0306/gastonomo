//! Puertos del contexto de ajustes.

use async_trait::async_trait;
use gastonomo_domain::settings::UserSettings;
use gastonomo_domain::value_object::{CurrencyCode, UserId};

use crate::error::AppError;

/// Lee y escribe los ajustes de una cuenta.
#[async_trait]
pub trait UserSettingsRepository: Send + Sync {
    /// Busca los ajustes de la cuenta.
    ///
    /// Devuelve `None` si la fila no existe, que solo pasa si la siembra del
    /// registro se quedo a medias.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find(&self, user_id: UserId) -> Result<Option<UserSettings>, AppError>;

    /// Guarda los ajustes, creando la fila si no estaba.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn save(&self, settings: &UserSettings) -> Result<(), AppError>;
}

/// Comprueba que una moneda se puede elegir como moneda principal.
///
/// Es un puerto aparte del catalogo de monedas a proposito: el contexto de ajustes
/// solo necesita responder si o no, y asi no depende de como se lea el catalogo ni
/// de cuando lo refresque la tarea de tasas.
#[async_trait]
pub trait DisplayCurrencyCatalog: Send + Sync {
    /// Indica si la moneda existe en el catalogo y se sigue ofreciendo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn is_selectable(&self, currency: &CurrencyCode) -> Result<bool, AppError>;

    /// Indica si el catalogo tiene ya alguna moneda que ofrecer.
    ///
    /// Sirve para distinguir dos rechazos que se ven iguales y no lo son: una
    /// moneda que no se ofrece es culpa del cuerpo de la peticion y es un 422, y un
    /// catalogo todavia vacio es que la tarea de tasas no ha conseguido su primera
    /// pasada y es un 503.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn is_ready(&self) -> Result<bool, AppError>;
}
