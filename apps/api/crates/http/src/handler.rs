//! Handlers HTTP.
//!
//! Cada contexto tiene ya su modulo declarado.

mod health;
mod openapi_document;

pub mod auth;
pub mod category;
pub mod changes;
pub mod currency;
pub mod loan;
pub mod movement;
pub mod settings;

pub use auth::{
    ChangePasswordHandler, ForgotPasswordHandler, LoginHandler, LogoutHandler, ProfileHandler,
    RefreshHandler, RegisterHandler, ResetPasswordHandler,
};
pub use health::{LivenessHandler, ReadinessHandler};
pub use openapi_document::{OpenApiHandler, SwaggerUiHandler};
