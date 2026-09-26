//! Handler de `GET /v1/movements`.

use std::sync::Arc;

use gastonomo_application::error::{AppError, ValidationErrors};
use gastonomo_application::use_case::movement::{ListMovements, ListMovementsQuery};
use gastonomo_domain::movement::MovementKind;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::write_json;
use crate::auth_context::current_user;
use crate::dto::movement::MovementPageDto;
use crate::problem::render_problem;
use crate::serialization::parse_date;

/// Devuelve los movimientos del periodo, de mas reciente a mas antiguo.
#[derive(Debug, Clone)]
pub struct ListMovementsHandler {
    use_case: Arc<ListMovements>,
}

impl ListMovementsHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ListMovements>) -> Self {
        Self { use_case }
    }
}

/// Lee un parametro de la query, ya recortado y sin vacios.
fn parameter(req: &Request, name: &str) -> Option<String> {
    req.query::<String>(name)
        .map(|value| value.trim().to_owned())
        .filter(|value| !value.is_empty())
}

/// Traduce la query string al filtro del caso de uso.
///
/// Los fallos de forma se juntan en una sola respuesta: es mas util que rechazar el
/// primero y dejar que el cliente descubra el resto a base de reintentos.
fn query(req: &Request) -> Result<ListMovementsQuery, AppError> {
    let mut errors = ValidationErrors::new();
    let from = date(&mut errors, req, "from");
    let to = date(&mut errors, req, "to");
    let kind = parameter(req, "kind").and_then(|raw| {
        MovementKind::parse(&raw)
            .inspect_err(|_| errors.push("kind", "El tipo tiene que ser `in` o `out`."))
            .ok()
    });
    let limit = parameter(req, "limit").and_then(|raw| {
        raw.parse::<u32>()
            .inspect_err(|_| errors.push("limit", "El tamano de pagina tiene que ser un entero."))
            .ok()
    });
    errors.into_result()?;
    Ok(ListMovementsQuery {
        from,
        to,
        kind,
        cursor: parameter(req, "cursor"),
        limit,
    })
}

fn date(errors: &mut ValidationErrors, req: &Request, name: &str) -> Option<chrono::NaiveDate> {
    parameter(req, name).and_then(|raw| {
        parse_date(&raw)
            .inspect_err(|_| errors.push(name, "La fecha tiene que ser YYYY-MM-DD."))
            .ok()
    })
}

#[salvo::async_trait]
impl Handler for ListMovementsHandler {
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
        let query = match query(req) {
            Ok(query) => query,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id, &query).await {
            Ok(page) => write_json(res, StatusCode::OK, MovementPageDto::from(&page)),
            Err(error) => render_problem(res, &error),
        }
    }
}
