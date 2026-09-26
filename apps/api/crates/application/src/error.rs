//! Error unico de la capa de aplicacion.
//!
//! Todos los casos de uso y todos los puertos devuelven este error, y el
//! adaptador HTTP lo traduce una sola vez a `application/problem+json`. Asi el
//! mapeo a codigos de estado vive en un sitio y no repartido por los handlers.

use thiserror::Error;

/// Error de validacion de un campo concreto del cuerpo de la peticion.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FieldError {
    /// Nombre del campo en el JSON, en camelCase.
    pub field: String,
    /// Mensaje para el cliente.
    pub message: String,
}

impl FieldError {
    /// Crea un error de campo.
    #[must_use]
    pub fn new(field: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            field: field.into(),
            message: message.into(),
        }
    }
}

/// Coleccion de errores de validacion, uno por campo.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ValidationErrors(Vec<FieldError>);

impl ValidationErrors {
    /// Crea una coleccion vacia.
    #[must_use]
    pub const fn new() -> Self {
        Self(Vec::new())
    }

    /// Anade un error de campo.
    pub fn push(&mut self, field: impl Into<String>, message: impl Into<String>) {
        self.0.push(FieldError::new(field, message));
    }

    /// Indica si no hay ningun error.
    #[must_use]
    pub const fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    /// Errores recogidos.
    #[must_use]
    pub fn entries(&self) -> &[FieldError] {
        &self.0
    }

    /// Convierte la coleccion en `Ok(())` si esta vacia.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] con los errores recogidos.
    pub fn into_result(self) -> Result<(), AppError> {
        if self.is_empty() {
            Ok(())
        } else {
            Err(AppError::Validation(self))
        }
    }
}

/// Fallo de un caso de uso.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum AppError {
    /// El cuerpo de la peticion no cumple las reglas. Se traduce a 422.
    #[error("the request body is invalid")]
    Validation(ValidationErrors),
    /// Correo o contrasena que no coinciden. Se traduce a 401.
    ///
    /// Es un error distinto de [`AppError::Unauthorized`] para no filtrar si el
    /// correo existe: la respuesta es la misma con correo desconocido y con
    /// contrasena equivocada.
    #[error("the credentials do not match")]
    InvalidCredentials,
    /// Falta el token, esta caducado o no vale. Se traduce a 401.
    #[error("unauthorized: {reason}")]
    Unauthorized {
        /// Motivo, para la traza y el campo `detail`.
        reason: String,
    },
    /// El recurso no existe, o existe y no es del usuario del token.
    ///
    /// Se traduce a 404 tambien cuando es de otro usuario: un 403 confirmaria que
    /// el recurso existe.
    #[error("{resource} not found")]
    NotFound {
        /// Recurso buscado.
        resource: String,
    },
    /// La operacion choca con una regla de negocio. Se traduce a 409.
    #[error("{message}")]
    Conflict {
        /// Codigo estable para que el cliente distinga el motivo.
        code: String,
        /// Mensaje para el usuario.
        message: String,
    },
    /// Demasiadas peticiones seguidas. Se traduce a 429.
    #[error("too many requests")]
    RateLimited,
    /// Fallo de una dependencia externa. Se traduce a 500 sin exponer detalles.
    #[error("a dependency failed: {detail}")]
    Dependency {
        /// Detalle tecnico, solo para la traza del servidor.
        detail: String,
    },
    /// El servicio todavia no puede responder a esto. Se traduce a 503.
    ///
    /// Es distinto de [`AppError::Dependency`] a proposito: nada ha fallado, es que
    /// falta un dato que llena una tarea de fondo. El caso real son las tasas de
    /// cambio antes del primer refresco con exito. Un 500 invitaria al cliente a
    /// tratarlo como un fallo del servidor; un 503 dice que vuelva a intentarlo.
    #[error("unavailable: {detail}")]
    Unavailable {
        /// Detalle tecnico, solo para la traza del servidor.
        detail: String,
    },
}

impl AppError {
    /// Error de validacion de un unico campo.
    #[must_use]
    pub fn validation(field: impl Into<String>, message: impl Into<String>) -> Self {
        let mut errors = ValidationErrors::new();
        errors.push(field, message);
        Self::Validation(errors)
    }

    /// Recurso no encontrado.
    #[must_use]
    pub fn not_found(resource: impl Into<String>) -> Self {
        Self::NotFound {
            resource: resource.into(),
        }
    }

    /// Conflicto con una regla de negocio.
    #[must_use]
    pub fn conflict(code: impl Into<String>, message: impl Into<String>) -> Self {
        Self::Conflict {
            code: code.into(),
            message: message.into(),
        }
    }

    /// Token ausente o invalido.
    #[must_use]
    pub fn unauthorized(reason: impl Into<String>) -> Self {
        Self::Unauthorized {
            reason: reason.into(),
        }
    }

    /// Fallo de una dependencia externa.
    #[must_use]
    pub fn dependency(detail: impl Into<String>) -> Self {
        Self::Dependency {
            detail: detail.into(),
        }
    }

    /// Dato que una tarea de fondo todavia no ha llenado.
    #[must_use]
    pub fn unavailable(detail: impl Into<String>) -> Self {
        Self::Unavailable {
            detail: detail.into(),
        }
    }
}
