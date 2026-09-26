//! DTOs de registro, login, refresco y cierre de sesion.

use gastonomo_application::UserProfile;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use crate::serialization::timestamp_to_string;

/// Cuerpo de `POST /v1/auth/register`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RegisterRequestDto {
    /// Nombre que se muestra en el saludo de Inicio.
    #[schema(example = "Camilo Rojas")]
    pub name: String,
    /// Correo de la cuenta.
    #[schema(example = "camilo@correo.com")]
    pub email: String,
    /// Contrasena de 8 caracteres o mas.
    pub password: String,
    /// Aceptacion de los terminos, obligatoria.
    pub accepted_terms: bool,
}

/// Cuerpo de `POST /v1/auth/login`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LoginRequestDto {
    /// Correo de la cuenta.
    #[schema(example = "camilo@correo.com")]
    pub email: String,
    /// Contrasena.
    pub password: String,
}

/// Cuerpo de `POST /v1/auth/refresh` y de `POST /v1/auth/logout`.
#[derive(Debug, Clone, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RefreshTokenRequestDto {
    /// Refresh token entregado en el login o en el refresco anterior.
    pub refresh_token: String,
}

/// Perfil que la app usa para el saludo y el avatar.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct UserDto {
    /// Identificador de la cuenta.
    #[schema(example = "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34")]
    pub id: String,
    /// Nombre que se muestra.
    #[schema(example = "Camilo Rojas")]
    pub name: String,
    /// Correo de la cuenta.
    #[schema(example = "camilo@correo.com")]
    pub email: String,
    /// Instante de creacion, en RFC 3339 UTC.
    #[schema(example = "2026-09-22T10:00:00Z")]
    pub created_at: String,
}

impl From<&UserProfile> for UserDto {
    fn from(profile: &UserProfile) -> Self {
        Self {
            id: profile.id.to_string(),
            name: profile.name.clone(),
            email: profile.email.clone(),
            created_at: timestamp_to_string(profile.created_at),
        }
    }
}

/// Respuesta de registro, login y refresco.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct SessionDto {
    /// Access token para la cabecera `Authorization: Bearer`.
    pub access_token: String,
    /// Segundos de validez del access token.
    #[schema(example = 900)]
    pub expires_in: i64,
    /// Refresh token, que se guarda en el dispositivo tras el cerrojo local.
    pub refresh_token: String,
    /// Perfil de quien entra.
    pub user: UserDto,
}

impl From<gastonomo_application::IssuedSession> for SessionDto {
    fn from(session: gastonomo_application::IssuedSession) -> Self {
        Self {
            access_token: session.access.value,
            expires_in: session.access.expires_in_seconds,
            refresh_token: session.refresh_token,
            user: UserDto::from(&session.user),
        }
    }
}
