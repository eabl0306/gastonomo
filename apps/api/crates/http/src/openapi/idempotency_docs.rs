//! Documentacion compartida de la cabecera `Idempotency-Key`.
//!
//! La llevan los tres endpoints que crean dinero, y se describe una sola vez para
//! que las tres descripciones no se separen.

use utoipa::openapi::path::{ParameterBuilder, ParameterIn};
use utoipa::openapi::schema::ObjectBuilder;
use utoipa::openapi::{Required, Type};

/// Texto de la cabecera, el mismo en los tres endpoints.
const DESCRIPTION: &str = "Clave del intento del usuario, opcional pero recomendada. \
     El cliente genera un valor por intento y lo repite en todos los reintentos de \
     ese intento: si el primero llego a guardar, el reintento recibe la misma \
     respuesta con el mismo identificador y no se crea un segundo registro. La \
     respuesta repetida lleva la cabecera `Idempotent-Replay: true`. La clave vale \
     24 horas y hasta 200 caracteres.";

/// Texto del 409 que produce reutilizar una clave.
pub const CONFLICT: &str = "`idempotency_key_reused`: la clave ya se uso con otro \
     cuerpo, asi que no es un reintento. `idempotency_key_in_progress`: hay otro \
     intento con esa clave todavia en vuelo.";

/// Parametro de cabecera para la operacion.
#[must_use]
pub fn parameter() -> ParameterBuilder {
    ParameterBuilder::new()
        .name("Idempotency-Key")
        .parameter_in(ParameterIn::Header)
        .required(Required::False)
        .description(Some(DESCRIPTION))
        .schema(Some(ObjectBuilder::new().schema_type(Type::String)))
}
