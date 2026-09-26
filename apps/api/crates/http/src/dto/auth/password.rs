//! DTOs de recuperacion y cambio de contrasena.

use serde::Deserialize;
use utoipa::ToSchema;

/// Cuerpo de `POST /v1/auth/password/forgot`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ForgotPasswordRequestDto {
    /// Correo de la cuenta que quiere recuperar la contrasena.
    #[schema(example = "camilo@correo.com")]
    pub email: String,
}

/// Cuerpo de `POST /v1/auth/password/reset`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ResetPasswordRequestDto {
    /// Token que viajaba en el enlace del correo.
    pub token: String,
    /// Contrasena nueva, de 8 caracteres o mas.
    pub new_password: String,
}

/// Cuerpo de `POST /v1/auth/password/change`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ChangePasswordRequestDto {
    /// Contrasena actual.
    pub current_password: String,
    /// Contrasena nueva, de 8 caracteres o mas.
    pub new_password: String,
    /// Refresh token de esta sesion, para no cerrarla.
    ///
    /// Si no se manda, el cambio cierra tambien la sesion que lo pidio.
    #[serde(default)]
    pub refresh_token: Option<String>,
}
