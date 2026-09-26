//! Ayudas comunes de los handlers de autenticacion.

use gastonomo_application::error::AppError;
use salvo::http::header::USER_AGENT;
use salvo::prelude::{Json, Request, Response, StatusCode};
use serde::Serialize;
use serde::de::DeserializeOwned;

/// Longitud maxima que se guarda del `User-Agent`.
const MAX_USER_AGENT: usize = 200;

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

/// `User-Agent` recortado, para que el usuario reconozca la sesion.
#[must_use]
pub fn user_agent(req: &Request) -> Option<String> {
    req.headers()
        .get(USER_AGENT)
        .and_then(|value| value.to_str().ok())
        .map(|value| value.chars().take(MAX_USER_AGENT).collect())
}

/// Escribe una respuesta JSON con el estado dado.
pub fn write_json<T: Serialize + Send + 'static>(res: &mut Response, status: StatusCode, body: T) {
    res.render_with_status(status, Json(body));
}

/// Escribe una respuesta sin cuerpo.
pub fn write_empty(res: &mut Response, status: StatusCode) {
    res.status_code(status);
}
