//! Handler de `POST /v1/movements`.

use std::sync::Arc;

use gastonomo_application::use_case::movement::{CreateMovement, CreateMovementCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, write_json};
use crate::auth_context::current_user;
use crate::dto::movement::{CreateMovementRequestDto, MovementDto};
use crate::problem::render_problem;

/// Registra un ingreso o un gasto y devuelve el movimiento creado.
#[derive(Debug, Clone)]
pub struct CreateMovementHandler {
    use_case: Arc<CreateMovement>,
}

impl CreateMovementHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<CreateMovement>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for CreateMovementHandler {
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
        let body = match json_body::<CreateMovementRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = CreateMovementCommand::from(body);
        match self.use_case.execute(user_id, command).await {
            Ok(movement) => write_json(res, StatusCode::CREATED, MovementDto::from(&movement)),
            Err(error) => render_problem(res, &error),
        }
    }
}
