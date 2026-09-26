//! Ayudas comunes de los handlers de prestamos.
//!
//! Son las mismas que usan los handlers de autenticacion. Estan repetidas porque
//! aquel modulo de ayudas es privado de su contexto, y compartirlo obligaria a
//! tocar un fichero que no es de este contexto.

use gastonomo_application::error::AppError;
use salvo::prelude::{Json, Request, Response, StatusCode};
use serde::Serialize;
use serde::de::DeserializeOwned;

/// Lee el cuerpo JSON de la peticion.
///
/// # Errors
///
/// Devuelve [`AppError::Validation`] sobre el campo `body` si el JSON no encaja con
/// el DTO. Es un 422 y no un 500: el cuerpo lo manda el cliente.
pub async fn json_body<T: DeserializeOwned>(req: &mut Request) -> Result<T, AppError> {
    req.parse_json::<T>()
        .await
        .map_err(|error| AppError::validation("body", format!("Cuerpo invalido: {error}")))
}

/// Escribe una respuesta JSON con el estado dado.
pub fn write_json<T: Serialize + Send + 'static>(res: &mut Response, status: StatusCode, body: T) {
    res.render_with_status(status, Json(body));
}

/// Identificador del prestamo que viaja en la ruta.
///
/// Si no esta, el recurso no puede existir: 404 y no 400, para no distinguir un
/// prestamo ajeno de uno inexistente.
///
/// # Errors
///
/// Devuelve [`AppError::NotFound`] si la ruta no trae el parametro.
pub fn loan_id(req: &Request) -> Result<String, AppError> {
    req.param::<String>("id")
        .ok_or_else(|| AppError::not_found("loan"))
}
