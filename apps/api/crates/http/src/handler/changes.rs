//! Handler de `GET /v1/changes`.

use std::sync::Arc;

use gastonomo_application::error::{AppError, ValidationErrors};
use gastonomo_application::port::changes::ChangeNumber;
use gastonomo_application::use_case::changes::GetChanges;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use crate::auth_context::current_user;
use crate::dto::changes::{ChangeBatchDto, decode_cursor};
use crate::problem::render_problem;

/// Devuelve lo que ha cambiado en la cuenta desde el cursor que trae el cliente.
///
/// Sin `since` devuelve desde el principio, que es el arranque en frio de un
/// dispositivo nuevo.
#[derive(Debug, Clone)]
pub struct GetChangesHandler {
    use_case: Arc<GetChanges>,
}

impl GetChangesHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<GetChanges>) -> Self {
        Self { use_case }
    }
}

/// Lee un parametro de la query, ya recortado y sin vacios.
fn parameter(req: &Request, name: &str) -> Option<String> {
    req.query::<String>(name)
        .map(|value| value.trim().to_owned())
        .filter(|value| !value.is_empty())
}

/// El cursor y el tamano de lote, con los dos fallos de forma en la misma respuesta.
fn query(req: &Request) -> Result<(Option<ChangeNumber>, Option<u32>), AppError> {
    let mut errors = ValidationErrors::new();
    let since = parameter(req, "since").and_then(|raw| {
        decode_cursor(&raw)
            .inspect_err(|_| {
                errors.push(
                    "since",
                    "El cursor no es uno de esta API. Vuelve a sincronizar desde el principio.",
                );
            })
            .ok()
    });
    let limit = parameter(req, "limit").and_then(|raw| {
        raw.parse::<u32>()
            .inspect_err(|_| errors.push("limit", "El tamano de lote tiene que ser un entero."))
            .ok()
    });
    errors.into_result()?;
    Ok((since, limit))
}

#[salvo::async_trait]
impl Handler for GetChangesHandler {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        let (since, limit) = match query(req) {
            Ok(parsed) => parsed,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id, since, limit).await {
            Ok(batch) => {
                res.render_with_status(
                    StatusCode::OK,
                    salvo::writing::Json(ChangeBatchDto::from(&batch)),
                );
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
