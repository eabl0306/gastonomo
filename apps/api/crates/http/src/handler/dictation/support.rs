//! Ayudas comunes de los handlers del dictado.
//!
//! Son las mismas que usan los demas contextos. Estan repetidas porque aquellos
//! modulos de ayudas son privados de su contexto, y compartirlo obligaria a tocar
//! un fichero que no es de este contexto.

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
