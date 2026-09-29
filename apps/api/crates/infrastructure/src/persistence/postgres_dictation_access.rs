//! Adaptador driven: la lista de aprobados del dictado en PostgreSQL.
//!
//! Solo lee, y lee un `EXISTS` contra la clave primaria de `dictation_allowlist`.
//! No hay escritura porque aprobar una cuenta no es una operacion de la aplicacion:
//! las filas las pone y las quita el dueno de la instancia por consola.
//!
//! Sin fila la respuesta es `false`, nunca un error. Que la tabla este vacia es el
//! estado normal de una instancia recien desplegada.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::dictation::DictationAccess;
use gastonomo_domain::value_object::UserId;
use sqlx::PgPool;

use super::mapping::database_error;

/// Lista de aprobados del dictado sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresDictationAccess {
    pool: PgPool,
}

impl PostgresDictationAccess {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl DictationAccess for PostgresDictationAccess {
    async fn is_allowed(&self, user_id: UserId) -> Result<bool, AppError> {
        sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS (SELECT 1 FROM dictation_allowlist WHERE user_id = $1)",
        )
        .bind(user_id.as_uuid())
        .fetch_one(&self.pool)
        .await
        .map_err(|error| database_error("could not read the dictation allowlist", &error))
    }
}
