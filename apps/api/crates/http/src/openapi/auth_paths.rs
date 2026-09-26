//! Documentacion OpenAPI de los endpoints de autenticacion.

use utoipa::openapi::path::{HttpMethod, OperationBuilder, PathItem, PathsBuilder};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::{ComponentsBuilder, Required};

use crate::dto::auth::{
    ChangePasswordRequestDto, ForgotPasswordRequestDto, LoginRequestDto, RefreshTokenRequestDto,
    RegisterRequestDto, ResetPasswordRequestDto, SessionDto, UserDto,
};
use crate::problem::{FieldProblemDto, ProblemDto};

/// Etiqueta comun de los endpoints de autenticacion.
const TAG: &str = "auth";

/// Descripcion de una operacion de autenticacion.
struct Operation<'a> {
    id: &'a str,
    summary: &'a str,
    description: &'a str,
    request: Option<&'a str>,
    success: (&'a str, &'a str, Option<&'a str>),
}

/// Anade las rutas de `/v1/auth` y `/v1/me`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder
        .path("/v1/auth/register", register())
        .path("/v1/auth/login", login())
        .path("/v1/auth/refresh", refresh())
        .path("/v1/auth/logout", logout())
        .path("/v1/auth/password/forgot", forgot())
        .path("/v1/auth/password/reset", reset())
        .path("/v1/auth/password/change", change())
        .path("/v1/me", profile())
}

/// Anade los esquemas de autenticacion y el de error.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<ProblemDto>()
        .schema_from::<FieldProblemDto>()
        .schema_from::<RegisterRequestDto>()
        .schema_from::<LoginRequestDto>()
        .schema_from::<RefreshTokenRequestDto>()
        .schema_from::<ForgotPasswordRequestDto>()
        .schema_from::<ResetPasswordRequestDto>()
        .schema_from::<ChangePasswordRequestDto>()
        .schema_from::<SessionDto>()
        .schema_from::<UserDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn build(method: HttpMethod, operation: &Operation<'_>) -> PathItem {
    let (status, description, schema) = operation.success;
    let success = schema.map_or_else(
        || ResponseBuilder::new().description(description),
        |name| {
            ResponseBuilder::new()
                .description(description)
                .json_schema_ref(name)
        },
    );
    let responses = ResponsesBuilder::new()
        .response(status, success)
        .response("422", problem("El cuerpo no cumple las reglas."))
        .response("500", problem("Fallo interno."))
        .build();
    let mut item = OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some(operation.id))
        .summary(Some(operation.summary))
        .description(Some(operation.description))
        .responses(responses);
    if let Some(name) = operation.request {
        item = item.request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref(name),
        ));
    }
    PathItem::new(method, item.build())
}

fn register() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "registerUser",
            summary: "Registro",
            description: "Crea la cuenta, siembra las 12 categorias predeterminadas y los \
                          ajustes iniciales, y devuelve la sesion abierta. Responde 409 con \
                          codigo `email_taken` si el correo ya tiene cuenta.",
            request: Some("RegisterRequestDto"),
            success: ("201", "Cuenta creada.", Some("SessionDto")),
        },
    )
}

fn login() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "loginUser",
            summary: "Inicio de sesion",
            description: "Devuelve un access token de vida corta y un refresh token. Con un \
                          correo desconocido y con una contrasena equivocada responde lo \
                          mismo: 401.",
            request: Some("LoginRequestDto"),
            success: ("200", "Sesion abierta.", Some("SessionDto")),
        },
    )
}

fn refresh() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "refreshSession",
            summary: "Refresco de sesion",
            description: "Rota el refresh token: revoca el presentado y entrega un par nuevo. \
                          Reutilizar uno ya rotado revoca la familia entera.",
            request: Some("RefreshTokenRequestDto"),
            success: ("200", "Sesion renovada.", Some("SessionDto")),
        },
    )
}

fn logout() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "logoutSession",
            summary: "Cierre de sesion",
            description: "Revoca el refresh token recibido. Es idempotente y no dice si el \
                          token existia.",
            request: Some("RefreshTokenRequestDto"),
            success: ("204", "Sesion cerrada.", None),
        },
    )
}

fn forgot() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "requestPasswordReset",
            summary: "Enlace de recuperacion",
            description: "Responde 202 siempre, exista o no el correo: si respondiera distinto \
                          seria un oraculo para saber quien tiene cuenta. El enlace caduca en \
                          30 minutos y sirve una vez.",
            request: Some("ForgotPasswordRequestDto"),
            success: ("202", "Peticion aceptada.", None),
        },
    )
}

fn reset() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "resetPassword",
            summary: "Cambio con el token del correo",
            description: "Cambia la contrasena y revoca todas las sesiones del usuario.",
            request: Some("ResetPasswordRequestDto"),
            success: ("204", "Contrasena cambiada.", None),
        },
    )
}

fn change() -> PathItem {
    build(
        HttpMethod::Post,
        &Operation {
            id: "changePassword",
            summary: "Cambio desde Ajustes",
            description: "Requiere access token. Revoca todas las sesiones menos la que manda \
                          su refresh token en el cuerpo.",
            request: Some("ChangePasswordRequestDto"),
            success: ("204", "Contrasena cambiada.", None),
        },
    )
}

fn profile() -> PathItem {
    build(
        HttpMethod::Get,
        &Operation {
            id: "getProfile",
            summary: "Perfil",
            description: "Nombre y correo de la cuenta del token, para el saludo y el avatar.",
            request: None,
            success: ("200", "Perfil.", Some("UserDto")),
        },
    )
}
