//! Handler de `POST /v1/auth/login`.

use std::sync::Arc;

use gastonomo_application::{LoginUser, LoginUserCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, user_agent, write_json};
use crate::dto::auth::{LoginRequestDto, SessionDto};
use crate::problem::render_problem;

/// Comprueba las credenciales y abre una sesion.
#[derive(Debug, Clone)]
pub struct LoginHandler {
    use_case: Arc<LoginUser>,
}

impl LoginHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<LoginUser>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for LoginHandler {
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let agent = user_agent(req);
        let body = match json_body::<LoginRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = LoginUserCommand {
            email: body.email,
            password: body.password,
            user_agent: agent,
        };
        match self.use_case.execute(command).await {
            Ok(session) => write_json(res, StatusCode::OK, SessionDto::from(session)),
            Err(error) => render_problem(res, &error),
        }
    }
}
