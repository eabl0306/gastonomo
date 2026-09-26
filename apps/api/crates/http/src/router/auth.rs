//! Sub-router de autenticacion.
//!
//! Se parte en dos: lo publico (`/v1/auth/...`), que no lleva token, y lo protegido
//! (`/v1/auth/password/change` y `/v1/me`), que cuelga del router con el middleware
//! de access token.

use std::fmt;
use std::sync::Arc;

use gastonomo_application::port::{Clock, TokenIssuer};
use gastonomo_application::{
    ChangePassword, GetProfile, LoginUser, LogoutSession, RefreshSession, RegisterUser,
    RequestPasswordReset, ResetPassword,
};
use salvo::prelude::Router;

use crate::handler::auth::{
    ChangePasswordHandler, ForgotPasswordHandler, LoginHandler, LogoutHandler, ProfileHandler,
    RefreshHandler, RegisterHandler, ResetPasswordHandler,
};
use crate::rate_limit::RateLimit;

/// Casos de uso y piezas que necesita el router de autenticacion.
#[derive(Clone)]
pub struct AuthDependencies {
    /// Registro.
    pub register: Arc<RegisterUser>,
    /// Login.
    pub login: Arc<LoginUser>,
    /// Rotacion del refresh token.
    pub refresh: Arc<RefreshSession>,
    /// Cierre de sesion.
    pub logout: Arc<LogoutSession>,
    /// Peticion del enlace de recuperacion.
    pub forgot_password: Arc<RequestPasswordReset>,
    /// Cambio de contrasena con el token del correo.
    pub reset_password: Arc<ResetPassword>,
    /// Cambio de contrasena estando dentro.
    pub change_password: Arc<ChangePassword>,
    /// Perfil del usuario autenticado.
    pub profile: Arc<GetProfile>,
    /// Validacion del access token, que usa el middleware de `/v1`.
    pub access_tokens: Arc<dyn TokenIssuer>,
    /// Reloj, que usa el middleware para saber si el token caduco.
    pub clock: Arc<dyn Clock>,
    /// Peticiones permitidas por ventana en login, registro y recuperacion.
    pub rate_limit_max_requests: u32,
    /// Tamano de la ventana del limite, en segundos.
    pub rate_limit_window_seconds: u64,
}

impl fmt::Debug for AuthDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("AuthDependencies")
            .field("rate_limit_max_requests", &self.rate_limit_max_requests)
            .field("rate_limit_window_seconds", &self.rate_limit_window_seconds)
            .finish_non_exhaustive()
    }
}

/// Monta los endpoints publicos de `/v1/auth`.
#[must_use]
pub fn public_router(dependencies: &AuthDependencies) -> Router {
    let limit = RateLimit::new(
        dependencies.rate_limit_max_requests,
        dependencies.rate_limit_window_seconds,
    );
    Router::with_path("auth")
        .push(
            Router::with_path("register")
                .hoop(limit.clone())
                .post(RegisterHandler::new(dependencies.register.clone())),
        )
        .push(
            Router::with_path("login")
                .hoop(limit.clone())
                .post(LoginHandler::new(dependencies.login.clone())),
        )
        .push(Router::with_path("refresh").post(RefreshHandler::new(dependencies.refresh.clone())))
        .push(Router::with_path("logout").post(LogoutHandler::new(dependencies.logout.clone())))
        .push(
            Router::with_path("password/forgot")
                .hoop(limit)
                .post(ForgotPasswordHandler::new(
                    dependencies.forgot_password.clone(),
                )),
        )
        .push(
            Router::with_path("password/reset").post(ResetPasswordHandler::new(
                dependencies.reset_password.clone(),
            )),
        )
}

/// Monta lo que exige access token: el cambio de contrasena y el perfil.
#[must_use]
pub fn protected_router(dependencies: &AuthDependencies) -> Router {
    Router::new()
        .push(
            Router::with_path("auth/password/change").post(ChangePasswordHandler::new(
                dependencies.change_password.clone(),
            )),
        )
        .push(Router::with_path("me").get(ProfileHandler::new(dependencies.profile.clone())))
}
