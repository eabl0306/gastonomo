//! Handlers de recuperacion y cambio de contrasena.

use std::sync::Arc;

use gastonomo_application::{
    ChangePassword, ChangePasswordCommand, RequestPasswordReset, RequestPasswordResetCommand,
    ResetPassword, ResetPasswordCommand,
};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, write_empty};
use crate::auth_context::current_user;
use crate::dto::auth::{
    ChangePasswordRequestDto, ForgotPasswordRequestDto, ResetPasswordRequestDto,
};
use crate::problem::render_problem;

/// Manda el enlace de recuperacion.
#[derive(Debug, Clone)]
pub struct ForgotPasswordHandler {
    use_case: Arc<RequestPasswordReset>,
}

impl ForgotPasswordHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<RequestPasswordReset>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ForgotPasswordHandler {
    /// Responde 202 siempre que el cuerpo sea valido, exista o no la cuenta.
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let body = match json_body::<ForgotPasswordRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = RequestPasswordResetCommand { email: body.email };
        match self.use_case.execute(command).await {
            Ok(()) => write_empty(res, StatusCode::ACCEPTED),
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Cambia la contrasena con el token del correo.
#[derive(Debug, Clone)]
pub struct ResetPasswordHandler {
    use_case: Arc<ResetPassword>,
}

impl ResetPasswordHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ResetPassword>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ResetPasswordHandler {
    async fn handle(
        &self,
        req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let body = match json_body::<ResetPasswordRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = ResetPasswordCommand {
            token: body.token,
            new_password: body.new_password,
        };
        match self.use_case.execute(command).await {
            Ok(()) => write_empty(res, StatusCode::NO_CONTENT),
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Cambia la contrasena estando dentro.
#[derive(Debug, Clone)]
pub struct ChangePasswordHandler {
    use_case: Arc<ChangePassword>,
}

impl ChangePasswordHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ChangePassword>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ChangePasswordHandler {
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
        let body = match json_body::<ChangePasswordRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = ChangePasswordCommand {
            user_id,
            current_password: body.current_password,
            new_password: body.new_password,
            current_refresh_token: body.refresh_token,
        };
        match self.use_case.execute(command).await {
            Ok(()) => write_empty(res, StatusCode::NO_CONTENT),
            Err(error) => render_problem(res, &error),
        }
    }
}
