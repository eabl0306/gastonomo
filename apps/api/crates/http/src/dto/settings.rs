//! DTOs del contexto de ajustes.
//!
//! Solo viajan la moneda principal y el idioma. El bloqueo por huella o PIN y el
//! ojo que oculta los montos son del dispositivo y no tienen campo aqui.

use gastonomo_domain::settings::UserSettings;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Respuesta de `GET /v1/settings` y de `PATCH /v1/settings`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct SettingsDto {
    /// Moneda con la que la app muestra las cifras.
    #[schema(example = "COP")]
    pub display_currency: String,
    /// Idioma de la app: `es` o `en`.
    #[schema(example = "es")]
    pub language: String,
}

impl From<&UserSettings> for SettingsDto {
    fn from(settings: &UserSettings) -> Self {
        Self {
            display_currency: settings.display_currency().to_string(),
            language: settings.language().as_str().to_owned(),
        }
    }
}

/// Cuerpo de `PATCH /v1/settings`.
///
/// Los dos campos son opcionales porque el sheet de moneda y el de idioma son dos
/// pantallas distintas y cada una manda solo lo suyo.
#[derive(Debug, Clone, Default, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateSettingsRequestDto {
    /// Moneda principal nueva, un codigo del catalogo de `GET /v1/currencies`.
    #[serde(default)]
    #[schema(example = "USD")]
    pub display_currency: Option<String>,
    /// Idioma nuevo: `es` o `en`.
    #[serde(default)]
    #[schema(example = "en")]
    pub language: Option<String>,
}
