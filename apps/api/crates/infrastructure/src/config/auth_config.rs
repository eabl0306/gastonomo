//! Configuracion de autenticacion.

use gastonomo_application::use_case::auth::RESET_LINK_TOKEN_PLACEHOLDER;
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
    ///
    /// Sin el marcador el correo sale igual, pero con un enlace sin token y sin
    /// que falle nada, asi que el arranque avisa por log. No tumba el proceso: es
    /// una capacidad opcional, y no arrancar dejaria sin servicio los 25 endpoints
    /// por un defecto que solo afecta a quien olvida la contrasena.
    pub reset_link_template: String,
    /// Peticiones permitidas por ventana en los cuatro endpoints limitados.
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

    /// Indica si la plantilla del enlace se comeria el token.
    ///
    /// `reset_link` es un `replace` a secas: una plantilla sin el marcador no
    /// sustituye nada y manda un enlace inservible sin quejarse.
    #[must_use]
    pub fn reset_link_drops_the_token(&self) -> bool {
        !self
            .reset_link_template
            .contains(RESET_LINK_TOKEN_PLACEHOLDER)
    }
}

#[cfg(test)]
mod tests {
    use super::{AuthConfig, DEVELOPMENT_SECRET};

    #[test]
    fn the_default_template_carries_the_marker() {
        assert!(!AuthConfig::default().reset_link_drops_the_token());
    }

    #[test]
    fn a_template_without_the_marker_is_caught() {
        let config = AuthConfig {
            reset_link_template: "https://gastonomo.test/reset-password".to_owned(),
            ..AuthConfig::default()
        };
        assert!(config.reset_link_drops_the_token());
    }

    #[test]
    fn the_development_secret_is_still_recognised() {
        assert!(AuthConfig::default().uses_development_secret());
        let config = AuthConfig {
            access_token_secret: format!("{DEVELOPMENT_SECRET}-pero-no"),
            ..AuthConfig::default()
        };
        assert!(!config.uses_development_secret());
    }
}
