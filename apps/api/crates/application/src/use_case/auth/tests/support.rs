//! Montaje comun de los tests: dobles en memoria y un reloj fijo.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use chrono::{DateTime, Utc};

use crate::testing::{
    FakeOpaqueTokenFactory, FakePasswordHasher, FakeTokenIssuer, FixedClock,
    InMemoryPasswordResetTokenRepository, InMemoryRefreshTokenRepository, InMemoryUserRepository,
    InMemoryUserSettingsRepository, RecordingMailer, RecordingUserProvisioning,
};
use crate::use_case::auth::{
    LoginUser, LoginUserCommand, LogoutSession, PasswordPorts, RefreshSession, RegisterUser,
    RegisterUserCommand, SessionPorts,
};

/// Dias de validez del refresh token en los tests.
pub const REFRESH_TTL_DAYS: i64 = 30;

/// Todo lo que un test necesita tocar, ya cableado.
pub struct Harness {
    /// Ajustes de la cuenta: de aqui sale el idioma del correo de recuperacion.
    pub settings: Arc<InMemoryUserSettingsRepository>,
    pub users: Arc<InMemoryUserRepository>,
    pub refresh_tokens: Arc<InMemoryRefreshTokenRepository>,
    pub reset_tokens: Arc<InMemoryPasswordResetTokenRepository>,
    pub provisioning: Arc<RecordingUserProvisioning>,
    pub mailer: Arc<RecordingMailer>,
    pub clock: Arc<FixedClock>,
    pub opaque_tokens: Arc<FakeOpaqueTokenFactory>,
    pub session_ports: SessionPorts,
    pub password_ports: PasswordPorts,
}

impl Harness {
    /// Cablea los dobles.
    #[must_use]
    pub fn new() -> Self {
        let users = Arc::new(InMemoryUserRepository::new());
        let refresh_tokens = Arc::new(InMemoryRefreshTokenRepository::new());
        let reset_tokens = Arc::new(InMemoryPasswordResetTokenRepository::new());
        let provisioning = Arc::new(RecordingUserProvisioning::new());
        let mailer = Arc::new(RecordingMailer::new());
        let clock = Arc::new(FixedClock::at_fixture());
        let opaque_tokens = Arc::new(FakeOpaqueTokenFactory::new());
        let password_hasher = Arc::new(FakePasswordHasher::new());
        let access_tokens = Arc::new(FakeTokenIssuer::new());
        let settings = Arc::new(InMemoryUserSettingsRepository::new());

        let session_ports = SessionPorts {
            users: users.clone(),
            refresh_tokens: refresh_tokens.clone(),
            password_hasher: password_hasher.clone(),
            access_tokens,
            opaque_tokens: opaque_tokens.clone(),
            clock: clock.clone(),
            refresh_ttl_days: REFRESH_TTL_DAYS,
        };
        let password_ports = PasswordPorts {
            users: users.clone(),
            reset_tokens: reset_tokens.clone(),
            refresh_tokens: refresh_tokens.clone(),
            password_hasher,
            opaque_tokens: opaque_tokens.clone(),
            clock: clock.clone(),
            mailer: mailer.clone(),
            settings: settings.clone(),
            reset_link_template: "https://gastonomo.test/reset?token={token}".to_owned(),
        };

        Self {
            settings,
            users,
            refresh_tokens,
            reset_tokens,
            provisioning,
            mailer,
            clock,
            opaque_tokens,
            session_ports,
            password_ports,
        }
    }

    /// Instante del reloj.
    #[must_use]
    pub fn now(&self) -> DateTime<Utc> {
        use crate::port::Clock as _;
        self.clock.now()
    }

    /// Caso de uso de registro.
    #[must_use]
    pub fn register(&self) -> RegisterUser {
        RegisterUser::new(self.session_ports.clone(), self.provisioning.clone())
    }

    /// Caso de uso de login.
    #[must_use]
    pub fn login(&self) -> LoginUser {
        LoginUser::new(self.session_ports.clone())
    }

    /// Caso de uso de refresco.
    #[must_use]
    pub fn refresh(&self) -> RefreshSession {
        RefreshSession::new(self.session_ports.clone())
    }

    /// Caso de uso de cierre de sesion.
    #[must_use]
    pub fn logout(&self) -> LogoutSession {
        LogoutSession::new(
            self.refresh_tokens.clone(),
            self.opaque_tokens.clone(),
            self.clock.clone(),
        )
    }
}

/// Comando de registro valido.
#[must_use]
pub fn registration() -> RegisterUserCommand {
    RegisterUserCommand {
        name: "Camilo Rojas".to_owned(),
        email: "camilo@correo.com".to_owned(),
        password: "contrasena-larga".to_owned(),
        accepted_terms: true,
        user_agent: Some("android".to_owned()),
    }
}

/// Comando de login valido para la cuenta de [`registration`].
#[must_use]
pub fn credentials() -> LoginUserCommand {
    LoginUserCommand {
        email: "camilo@correo.com".to_owned(),
        password: "contrasena-larga".to_owned(),
        user_agent: None,
    }
}
