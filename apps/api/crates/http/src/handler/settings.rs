//! Handlers del contexto de ajustes.
//!
//! Los dos son cortos y no necesitan modulo aparte. El usuario autenticado se lee
//! con [`crate::auth_context::current_user`] y los errores se escriben con
//! [`crate::problem::render_problem`].

use std::sync::Arc;

use gastonomo_application::error::AppError;
use gastonomo_application::use_case::settings::{
    GetUserSettings, UpdateUserSettings, UpdateUserSettingsCommand,
};
use salvo::prelude::{Depot, FlowCtrl, Handler, Json, Request, Response, StatusCode};

use crate::auth_context::current_user;
use crate::dto::settings::{SettingsDto, UpdateSettingsRequestDto};
use crate::problem::render_problem;

/// Devuelve la moneda principal y el idioma de la cuenta.
#[derive(Debug, Clone)]
pub struct GetSettingsHandler {
    use_case: Arc<GetUserSettings>,
}

impl GetSettingsHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<GetUserSettings>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for GetSettingsHandler {
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
            Ok(settings) => {
                res.render_with_status(StatusCode::OK, Json(SettingsDto::from(&settings)));
            }
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Cambia la moneda principal, el idioma o los dos.
#[derive(Debug, Clone)]
pub struct UpdateSettingsHandler {
    use_case: Arc<UpdateUserSettings>,
}

impl UpdateSettingsHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<UpdateUserSettings>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for UpdateSettingsHandler {
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
        let body = match req.parse_json::<UpdateSettingsRequestDto>().await {
            Ok(body) => body,
            Err(error) => {
                let invalid = AppError::validation("body", format!("Cuerpo invalido: {error}"));
                return render_problem(res, &invalid);
            }
        };
        let command = UpdateUserSettingsCommand {
            user_id,
            display_currency: body.display_currency,
            language: body.language,
        };
        match self.use_case.execute(command).await {
            Ok(settings) => {
                res.render_with_status(StatusCode::OK, Json(SettingsDto::from(&settings)));
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
