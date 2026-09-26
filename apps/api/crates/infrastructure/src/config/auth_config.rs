//! Configuracion de autenticacion.

use serde::{Deserialize, Serialize};

/// Secreto por defecto, solo valido en desarrollo.
///
/// El arranque avisa por log si sigue puesto: con este secreto cualquiera que lea
/// el repositorio puede firmar un access token.
pub const DEVELOPMENT_SECRET: &str = "development-only-secret-change-me";

/// Configuracion de tokens y limites de la autenticacion.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct AuthConfig {
    /// Secreto HMAC con el que se firma el access token.
    pub access_token_secret: String,
    /// Minutos de validez del access token.
    pub access_token_minutes: i64,
    /// Dias de validez del refresh token.
    pub refresh_token_days: i64,
    /// Plantilla del enlace de recuperacion, con el marcador `{token}`.
    pub reset_link_template: String,
    /// Peticiones permitidas por ventana en login, registro y recuperacion.
    pub rate_limit_max_requests: u32,
    /// Tamano de la ventana del limite, en segundos.
    pub rate_limit_window_seconds: u64,
}

impl Default for AuthConfig {
    fn default() -> Self {
        Self {
            access_token_secret: DEVELOPMENT_SECRET.to_owned(),
            access_token_minutes: 15,
            refresh_token_days: 30,
            reset_link_template: "http://localhost:5173/reset-password?token={token}".to_owned(),
            rate_limit_max_requests: 10,
            rate_limit_window_seconds: 60,
        }
    }
}

impl AuthConfig {
    /// Indica si se esta usando el secreto de desarrollo.
    #[must_use]
    pub fn uses_development_secret(&self) -> bool {
        self.access_token_secret == DEVELOPMENT_SECRET
    }
}
