//! Ayudas comunes de los handlers de categorias.

use gastonomo_application::error::AppError;
use gastonomo_domain::category::CategoryId;
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

/// Lee el identificador de la categoria del camino.
///
/// # Errors
///
/// Devuelve [`AppError::NotFound`] si falta o no es un UUID. Es un 404 a proposito:
/// un identificador que no existe y uno mal escrito son lo mismo para el cliente, y
/// asi la respuesta no distingue entre "no existe" y "no es tuya".
pub fn path_category_id(req: &Request) -> Result<CategoryId, AppError> {
    let raw = req
        .param::<String>("id")
        .ok_or_else(|| AppError::not_found("category"))?;
    CategoryId::parse(&raw).map_err(|_| AppError::not_found("category"))
}

/// Escribe una respuesta JSON con el estado dado.
pub fn write_json<T: Serialize + Send + 'static>(res: &mut Response, status: StatusCode, body: T) {
    res.render_with_status(status, Json(body));
}
