//! Puertos compartidos por los tres casos de uso de contrasena.

use std::fmt;
use std::sync::Arc;

use crate::port::settings::UserSettingsRepository;
use crate::port::{
    Clock, Mailer, OpaqueTokenFactory, PasswordHasher, PasswordResetTokenRepository,
    RefreshTokenRepository, UserRepository,
};

/// Marca que el enlace de recuperacion sustituye por el token.
pub const RESET_LINK_TOKEN_PLACEHOLDER: &str = "{token}";

/// Puertos de recuperacion y cambio de contrasena.
#[derive(Clone)]
pub struct PasswordPorts {
    /// Cuentas.
    pub users: Arc<dyn UserRepository>,
    /// Tokens de recuperacion.
    pub reset_tokens: Arc<dyn PasswordResetTokenRepository>,
    /// Refresh tokens, que se revocan al cambiar la contrasena.
    pub refresh_tokens: Arc<dyn RefreshTokenRepository>,
    /// Hash de contrasenas.
    pub password_hasher: Arc<dyn PasswordHasher>,
    /// Creacion de tokens opacos.
    pub opaque_tokens: Arc<dyn OpaqueTokenFactory>,
    /// Reloj.
    pub clock: Arc<dyn Clock>,
    /// Envio de correo.
    pub mailer: Arc<dyn Mailer>,
    /// Ajustes de la cuenta, de donde sale el idioma del correo.
    pub settings: Arc<dyn UserSettingsRepository>,
    /// Plantilla del enlace del correo, con [`RESET_LINK_TOKEN_PLACEHOLDER`].
    pub reset_link_template: String,
}

impl PasswordPorts {
    /// Construye el enlace que viaja en el correo.
    #[must_use]
    pub fn reset_link(&self, token: &str) -> String {
        self.reset_link_template
            .replace(RESET_LINK_TOKEN_PLACEHOLDER, token)
    }
}

impl fmt::Debug for PasswordPorts {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("PasswordPorts")
            .field("reset_link_template", &self.reset_link_template)
            .finish_non_exhaustive()
    }
}
