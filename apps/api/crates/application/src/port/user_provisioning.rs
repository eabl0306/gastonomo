//! Puerto de siembra de datos iniciales de una cuenta.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::UserId;

use crate::error::AppError;

/// Siembra lo que una cuenta nueva necesita para que la app se pinte.
///
/// Son las 12 categorias predeterminadas de
/// [`gastonomo_domain::category::DEFAULT_CATEGORIES`] y la fila de ajustes con
/// moneda `COP` e idioma `es`. Va detras de un puerto porque son dos tablas de
/// otros contextos: el de categorias y el de ajustes. El adaptador de PostgreSQL
/// lo hace en la misma transaccion que el `INSERT` de la cuenta.
#[async_trait]
pub trait UserProvisioning: Send + Sync {
    /// Siembra categorias y ajustes de una cuenta recien creada.
    ///
    /// Tiene que ser idempotente: si la cuenta ya tiene datos, no duplica nada.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn provision_new_user(&self, user_id: UserId, now: DateTime<Utc>)
    -> Result<(), AppError>;
}
