//! Puertos del dictado.

use async_trait::async_trait;
use gastonomo_domain::dictation::{DictationContext, Interpretation};
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;

/// Convierte lo que el usuario dicto en un registro interpretado.
///
/// El puerto habla **tipos de dominio**, no JSON ni prompts: como se le pregunta al
/// modelo, que formato devuelve y como se lee es cosa del adaptador. Asi el nucleo
/// no sabe que hay un modelo de lenguaje detras, y cambiarlo no le llega.
#[async_trait]
pub trait DictationInterpreter: Send + Sync {
    /// Interpreta el texto con lo que la cuenta aporta como contexto.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si no se pudo interpretar. No devuelve
    /// error porque el texto sea raro: un dictado que no se entiende acaba en un
    /// borrador incompleto, que el usuario completa.
    async fn interpret(
        &self,
        text: &str,
        context: &DictationContext<'_>,
    ) -> Result<Interpretation, AppError>;
}

/// Quien tiene el dictado disponible.
///
/// Es un puerto aparte y no un campo de la cuenta a proposito: la aprobacion es una
/// decision del dueno de la instancia, no un atributo del usuario, y asi conceder o
/// quitar el acceso es una fila y no una migracion del agregado.
#[async_trait]
pub trait DictationAccess: Send + Sync {
    /// Indica si la cuenta tiene el dictado aprobado.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn is_allowed(&self, user_id: UserId) -> Result<bool, AppError>;
}
