//! Handlers de `POST /v1/auth/refresh` y `POST /v1/auth/logout`.

use std::sync::Arc;

use gastonomo_application::{
    LogoutSession, LogoutSessionCommand, RefreshSession, RefreshSessionCommand,
};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, user_agent, write_empty, write_json};
use crate::dto::auth::{RefreshTokenRequestDto, SessionDto};
use crate::problem::render_problem;

/// Rota el refresh token y devuelve un par nuevo.
#[derive(Debug, Clone)]
pub struct RefreshHandler {
    use_case: Arc<RefreshSession>,
}

impl RefreshHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<RefreshSession>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for RefreshHandler {
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let agent = user_agent(req);
        let body = match json_body::<RefreshTokenRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = RefreshSessionCommand {
            refresh_token: body.refresh_token,
            user_agent: agent,
        };
        match self.use_case.execute(command).await {
            Ok(session) => write_json(res, StatusCode::OK, SessionDto::from(session)),
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Revoca el refresh token recibido.
#[derive(Debug, Clone)]
pub struct LogoutHandler {
    use_case: Arc<LogoutSession>,
}

impl LogoutHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<LogoutSession>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for LogoutHandler {
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let body = match json_body::<RefreshTokenRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = LogoutSessionCommand {
            refresh_token: body.refresh_token,
        };
        match self.use_case.execute(command).await {
            Ok(()) => write_empty(res, StatusCode::NO_CONTENT),
            Err(error) => render_problem(res, &error),
        }
    }
}
