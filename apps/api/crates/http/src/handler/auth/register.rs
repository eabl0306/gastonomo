//! Handler de `POST /v1/auth/register`.

use std::sync::Arc;

use gastonomo_application::{RegisterUser, RegisterUserCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, user_agent, write_json};
use crate::dto::auth::{RegisterRequestDto, SessionDto};
use crate::problem::render_problem;

/// Registra una cuenta y devuelve la sesion abierta.
#[derive(Debug, Clone)]
pub struct RegisterHandler {
    use_case: Arc<RegisterUser>,
}

impl RegisterHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<RegisterUser>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for RegisterHandler {
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let agent = user_agent(req);
        let body = match json_body::<RegisterRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = RegisterUserCommand {
            name: body.name,
            email: body.email,
            password: body.password,
            accepted_terms: body.accepted_terms,
            user_agent: agent,
        };
        match self.use_case.execute(command).await {
            Ok(session) => write_json(res, StatusCode::CREATED, SessionDto::from(session)),
            Err(error) => render_problem(res, &error),
        }
    }
}
