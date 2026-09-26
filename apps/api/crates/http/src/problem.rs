//! Errores HTTP en `application/problem+json` (RFC 9457).
//!
//! Es el unico sitio que traduce [`AppError`] a codigo de estado y cuerpo. Un
//! handler nunca escribe un error a mano: llama a [`render_problem`].
//!
//! ```ignore
//! match self.use_case.execute(command).await {
//!     Ok(value) => res.render_with_status(StatusCode::OK, Json(Dto::from(value))),
//!     Err(error) => render_problem(res, &error),
//! }
//! ```

use gastonomo_application::error::AppError;
use salvo::http::header::CONTENT_TYPE;
use salvo::prelude::{Response, StatusCode};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Tipo de contenido que exige la RFC 9457.
pub const PROBLEM_CONTENT_TYPE: &str = "application/problem+json";

/// Prefijo de los `type` del contrato.
const TYPE_PREFIX: &str = "https://gastonomo.app/problems/";

/// Error de un campo concreto del cuerpo.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
pub struct FieldProblemDto {
    /// Nombre del campo, en camelCase.
    #[schema(example = "email")]
    pub field: String,
    /// Motivo del rechazo.
    pub message: String,
}

/// Cuerpo de error del contrato publico.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
pub struct ProblemDto {
    /// URI que identifica el tipo de problema.
    #[serde(rename = "type")]
    #[schema(rename = "type", example = "https://gastonomo.app/problems/validation")]
    pub problem_type: String,
    /// Resumen legible, siempre el mismo para un `type`.
    pub title: String,
    /// Codigo de estado HTTP, repetido en el cuerpo como pide la RFC.
    #[schema(example = 422)]
    pub status: u16,
    /// Detalle de esta ocurrencia concreta.
    pub detail: String,
    /// Codigo estable de negocio, presente en los conflictos.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Errores por campo, presentes solo en los errores de validacion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub errors: Option<Vec<FieldProblemDto>>,
}

impl ProblemDto {
    fn new(slug: &str, title: &str, status: StatusCode, detail: String) -> Self {
        Self {
            problem_type: format!("{TYPE_PREFIX}{slug}"),
            title: title.to_owned(),
            status: status.as_u16(),
            detail,
            code: None,
            errors: None,
        }
    }
}

/// Traduce un error de aplicacion a estado y cuerpo.
#[must_use]
pub fn problem_for(error: &AppError) -> (StatusCode, ProblemDto) {
    match error {
        AppError::Validation(errors) => (StatusCode::UNPROCESSABLE_ENTITY, validation(errors)),
        AppError::InvalidCredentials => (
            StatusCode::UNAUTHORIZED,
            ProblemDto::new(
                "invalid-credentials",
                "Credenciales incorrectas",
                StatusCode::UNAUTHORIZED,
                "El correo o la contrasena no coinciden.".to_owned(),
            ),
        ),
        AppError::Unauthorized { reason } => (
            StatusCode::UNAUTHORIZED,
            ProblemDto::new(
                "unauthorized",
                "No autorizado",
                StatusCode::UNAUTHORIZED,
                reason.clone(),
            ),
        ),
        AppError::NotFound { resource } => (
            StatusCode::NOT_FOUND,
            ProblemDto::new(
                "not-found",
                "No encontrado",
                StatusCode::NOT_FOUND,
                format!("No existe el recurso solicitado: {resource}."),
            ),
        ),
        AppError::Conflict { code, message } => (StatusCode::CONFLICT, conflict(code, message)),
        AppError::RateLimited => (
            StatusCode::TOO_MANY_REQUESTS,
            ProblemDto::new(
                "too-many-requests",
                "Demasiadas peticiones",
                StatusCode::TOO_MANY_REQUESTS,
                "Has hecho demasiados intentos seguidos. Prueba en un momento.".to_owned(),
            ),
        ),
        AppError::Dependency { detail } => (StatusCode::INTERNAL_SERVER_ERROR, internal(detail)),
        AppError::Unavailable { detail } => (StatusCode::SERVICE_UNAVAILABLE, unavailable(detail)),
    }
}

fn validation(errors: &gastonomo_application::error::ValidationErrors) -> ProblemDto {
    let mut problem = ProblemDto::new(
        "validation",
        "Datos invalidos",
        StatusCode::UNPROCESSABLE_ENTITY,
        "Revisa los campos marcados.".to_owned(),
    );
    problem.errors = Some(
        errors
            .entries()
            .iter()
            .map(|entry| FieldProblemDto {
                field: entry.field.clone(),
                message: entry.message.clone(),
            })
            .collect(),
    );
    problem
}

fn conflict(code: &str, message: &str) -> ProblemDto {
    let mut problem = ProblemDto::new(
        "conflict",
        "Conflicto",
        StatusCode::CONFLICT,
        message.to_owned(),
    );
    problem.code = Some(code.to_owned());
    problem
}

/// Falta un dato que llena una tarea de fondo. No es un fallo, asi que no se
/// registra como error: se avisa y se invita a reintentar.
fn unavailable(detail: &str) -> ProblemDto {
    tracing::warn!(detail = %detail, "a request needed data a background task has not produced yet");
    ProblemDto::new(
        "service-unavailable",
        "Servicio no disponible todavia",
        StatusCode::SERVICE_UNAVAILABLE,
        "Todavia no tenemos este dato. Vuelve a intentarlo en un momento.".to_owned(),
    )
}

/// El detalle tecnico se queda en la traza: al cliente no se le cuenta que ha
/// fallado por dentro.
fn internal(detail: &str) -> ProblemDto {
    tracing::error!(detail = %detail, "a dependency failed while handling a request");
    ProblemDto::new(
        "internal-error",
        "Error interno",
        StatusCode::INTERNAL_SERVER_ERROR,
        "No hemos podido completar la operacion. Intentalo de nuevo.".to_owned(),
    )
}

/// Traduce a `problem+json` un estado de error que no nace de un caso de uso.
///
/// Son los que produce el propio transporte antes de llegar a un handler: la ruta
/// que no existe, el metodo que no esta permitido y el cuerpo ilegible. Sin esto
/// Salvo responde su pagina por defecto, que negocia el `Accept` y sale en HTML,
/// y un cliente de esta API siempre espera JSON.
#[must_use]
pub fn problem_for_status(status: Option<StatusCode>) -> (StatusCode, ProblemDto) {
    let status = status.unwrap_or(StatusCode::INTERNAL_SERVER_ERROR);
    let (slug, title, detail) = match status {
        StatusCode::NOT_FOUND => (
            "not-found",
            "No encontrado",
            "La ruta solicitada no existe en esta API.",
        ),
        StatusCode::METHOD_NOT_ALLOWED => (
            "method-not-allowed",
            "Metodo no permitido",
            "Esa ruta no acepta este metodo HTTP.",
        ),
        StatusCode::UNSUPPORTED_MEDIA_TYPE => (
            "unsupported-media-type",
            "Tipo de contenido no admitido",
            "El cuerpo tiene que viajar como application/json.",
        ),
        StatusCode::PAYLOAD_TOO_LARGE => (
            "payload-too-large",
            "Cuerpo demasiado grande",
            "El cuerpo de la peticion supera el maximo admitido.",
        ),
        other if other.is_client_error() => (
            "bad-request",
            "Peticion invalida",
            "No hemos podido interpretar la peticion.",
        ),
        _ => (
            "internal-error",
            "Error interno",
            "No hemos podido completar la operacion. Intentalo de nuevo.",
        ),
    };
    (
        status,
        ProblemDto::new(slug, title, status, detail.to_owned()),
    )
}

/// Escribe el error en la respuesta con el tipo de contenido de la RFC 9457.
pub fn render_problem(res: &mut Response, error: &AppError) {
    let (status, problem) = problem_for(error);
    write_problem(res, status, &problem);
}

/// Serializa un problema ya resuelto sobre la respuesta.
pub fn write_problem(res: &mut Response, status: StatusCode, problem: &ProblemDto) {
    let body = serde_json::to_vec(problem).unwrap_or_else(|_| {
        br#"{"type":"about:blank","title":"Error interno","status":500,"detail":""}"#.to_vec()
    });
    res.status_code(status);
    let _ = res.add_header(CONTENT_TYPE, PROBLEM_CONTENT_TYPE, true);
    // Si el cuerpo no se puede escribir la conexion ya esta perdida; no hay nada
    // mejor que hacer que dejar el estado puesto.
    let _ = res.write_body(body);
}
