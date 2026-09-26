//! Handler de `GET /v1/me`.

use std::sync::Arc;

use gastonomo_application::GetProfile;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::write_json;
use crate::auth_context::current_user;
use crate::dto::auth::UserDto;
use crate::problem::render_problem;

/// Devuelve el nombre y el correo de la cuenta del token.
#[derive(Debug, Clone)]
pub struct ProfileHandler {
    use_case: Arc<GetProfile>,
}

impl ProfileHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<GetProfile>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ProfileHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id).await {
            Ok(profile) => write_json(res, StatusCode::OK, UserDto::from(&profile)),
            Err(error) => render_problem(res, &error),
        }
    }
}
