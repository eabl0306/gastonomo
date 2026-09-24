//! Middleware que garantiza un identificador de peticion.

use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response};
use uuid::Uuid;

/// Cabecera que transporta el identificador de peticion.
pub const REQUEST_ID_HEADER: &str = "x-request-id";

/// Clave con la que el identificador se guarda en el `Depot`.
pub const REQUEST_ID_KEY: &str = "request_id";

/// Propaga el `x-request-id` entrante o genera uno nuevo.
///
/// Tambien deja una traza por peticion con metodo, ruta y estado final, que es
/// lo minimo para poder correlacionar logs entre la web, Android y la API.
#[derive(Debug, Clone, Copy, Default)]
pub struct RequestId;

impl RequestId {
    /// Crea el middleware.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

#[salvo::async_trait]
impl Handler for RequestId {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        ctrl: &mut FlowCtrl,
    ) {
        let request_id = incoming_id(req).unwrap_or_else(|| Uuid::new_v4().to_string());
        let method = req.method().to_string();
        let path = req.uri().path().to_owned();
        depot.insert(REQUEST_ID_KEY, request_id.clone());

        ctrl.call_next(req, depot, res).await;

        if let Ok(value) = request_id.parse() {
            res.headers_mut().insert(REQUEST_ID_HEADER, value);
        }
        tracing::info!(
            request_id = %request_id,
            method = %method,
            path = %path,
            status = res.status_code.map_or(0, |code| code.as_u16()),
            "handled request"
        );
    }
}

fn incoming_id(req: &Request) -> Option<String> {
    let raw = req.headers().get(REQUEST_ID_HEADER)?.to_str().ok()?.trim();
    if raw.is_empty() || raw.len() > 128 {
        return None;
    }
    Some(raw.to_owned())
}
