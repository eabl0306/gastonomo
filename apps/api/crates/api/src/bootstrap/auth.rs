//! Cableado del contexto de autenticacion.

use std::sync::Arc;

use gastonomo_application::{
    ChangePassword, GetProfile, LoginUser, LogoutSession, PasswordPorts, RefreshSession,
    RegisterUser, RequestPasswordReset, ResetPassword, SessionPorts,
};
use gastonomo_http::AuthDependencies;
use gastonomo_infrastructure::{
    Argon2PasswordHashing, JwtTokenIssuer, PostgresPasswordResetTokenRepository,
    PostgresRefreshTokenRepository, PostgresUserProvisioning, PostgresUserRepository,
    RandomOpaqueTokenFactory, build_mailer,
};

use gastonomo_infrastructure::persistence::PostgresUserSettingsRepository;

use super::wiring::Wiring;
use crate::error::StartupError;

/// Monta los casos de uso de autenticacion con los adaptadores reales.
///
/// # Errors
///
/// Devuelve [`StartupError::Mail`] si la configuracion de correo no es valida.
pub fn dependencies(wiring: &Wiring) -> Result<AuthDependencies, StartupError> {
    let session = session_ports(wiring);
    let password = password_ports(wiring, &session)?;
    let auth_config = &wiring.config.auth;

    Ok(AuthDependencies {
        register: Arc::new(RegisterUser::new(
            session.clone(),
            Arc::new(PostgresUserProvisioning::new(wiring.pool.clone())),
        )),
        login: Arc::new(LoginUser::new(session.clone())),
        refresh: Arc::new(RefreshSession::new(session.clone())),
        logout: Arc::new(LogoutSession::new(
            session.refresh_tokens.clone(),
            session.opaque_tokens.clone(),
            wiring.clock.clone(),
        )),
        forgot_password: Arc::new(RequestPasswordReset::new(password.clone())),
        reset_password: Arc::new(ResetPassword::new(password.clone())),
        change_password: Arc::new(ChangePassword::new(password)),
        profile: Arc::new(GetProfile::new(session.users.clone())),
        access_tokens: session.access_tokens.clone(),
        clock: wiring.clock.clone(),
        rate_limit_max_requests: auth_config.rate_limit_max_requests,
        rate_limit_window_seconds: auth_config.rate_limit_window_seconds,
    })
}

fn session_ports(wiring: &Wiring) -> SessionPorts {
    let auth_config = &wiring.config.auth;
    SessionPorts {
        users: Arc::new(PostgresUserRepository::new(wiring.pool.clone())),
        refresh_tokens: Arc::new(PostgresRefreshTokenRepository::new(wiring.pool.clone())),
        password_hasher: Arc::new(Argon2PasswordHashing::new()),
        access_tokens: Arc::new(JwtTokenIssuer::new(
            &auth_config.access_token_secret,
            auth_config.access_token_minutes,
        )),
        opaque_tokens: Arc::new(RandomOpaqueTokenFactory::new()),
        clock: wiring.clock.clone(),
        refresh_ttl_days: auth_config.refresh_token_days,
    }
}

fn password_ports(wiring: &Wiring, session: &SessionPorts) -> Result<PasswordPorts, StartupError> {
    Ok(PasswordPorts {
        users: session.users.clone(),
        reset_tokens: Arc::new(PostgresPasswordResetTokenRepository::new(
            wiring.pool.clone(),
        )),
        refresh_tokens: session.refresh_tokens.clone(),
        password_hasher: session.password_hasher.clone(),
        opaque_tokens: session.opaque_tokens.clone(),
        clock: wiring.clock.clone(),
        mailer: build_mailer(&wiring.config.mail)?,
        settings: Arc::new(PostgresUserSettingsRepository::new(wiring.pool.clone())),
        reset_link_template: wiring.config.auth.reset_link_template.clone(),
    })
}
