//! Captura y repeticion de la respuesta de una peticion idempotente.

use gastonomo_application::port::IdempotentResponse;
use salvo::http::ResBody;
use salvo::http::header::CONTENT_TYPE;
use salvo::prelude::{Response, StatusCode};

/// Cabecera que marca una respuesta repetida.
///
/// No hace falta para que la aplicacion funcione, pero deja ver en un log o en un
/// test si el segundo intento escribio algo o solo repitio lo de antes.
pub const REPLAY_HEADER: &str = "idempotent-replay";

/// Devuelve la respuesta a guardar, o `None` si no hay nada que repetir.
///
/// Solo se guardan las respuestas de exito con cuerpo. Un rechazo no se guarda a
/// proposito: el cliente tiene que poder corregir el cuerpo y reintentar.
pub fn captured(res: &Response) -> Option<IdempotentResponse> {
    let status = res.status_code.unwrap_or(StatusCode::OK);
    if !status.is_success() {
        return None;
    }
    Some(IdempotentResponse {
        status: status.as_u16(),
        body: body_bytes(&res.body)?,
    })
}

/// Escribe una respuesta ya guardada.
pub fn write(res: &mut Response, stored: &IdempotentResponse) {
    let status = StatusCode::from_u16(stored.status).unwrap_or(StatusCode::OK);
    res.status_code(status);
    // Los tres endpoints protegidos responden siempre JSON; no hay otro tipo de
    // contenido que repetir.
    let _ = res.add_header(CONTENT_TYPE, "application/json; charset=utf-8", true);
    let _ = res.add_header(REPLAY_HEADER, "true", true);
    let _ = res.write_body(stored.body.clone());
}

fn body_bytes(body: &ResBody) -> Option<Vec<u8>> {
    match body {
        ResBody::Once(bytes) => Some(bytes.to_vec()),
        ResBody::Chunks(chunks) => Some(chunks.iter().flatten().copied().collect()),
        // Un cuerpo en streaming no se puede leer sin consumirlo. Ninguno de los
        // endpoints protegidos lo usa, y si algun dia lo usara es mejor no guardar
        // nada que guardar algo a medias.
        _ => None,
    }
}
