//! Capa de aplicacion de Gastonomo.
//!
//! Declara los puertos que el nucleo necesita y los casos de uso que los
//! orquestan. No conoce Salvo, SQLx ni ningun runtime asincrono concreto: solo
//! `async-trait` para poder usar los puertos como objetos de trait y `chrono` para
//! hablar de instantes.
//!
//! Todos los casos de uso y todos los puertos devuelven [`AppError`], y el
//! adaptador HTTP lo traduce una sola vez a `application/problem+json`.

pub mod error;
pub mod port;
pub mod use_case;

#[cfg(any(test, feature = "testing"))]
pub mod testing;

pub use error::{AppError, FieldError, ValidationErrors};
pub use port::{HealthProbe, ProbeError};
pub use use_case::auth::{
    ChangePassword, ChangePasswordCommand, GetProfile, IssuedSession, LoginUser, LoginUserCommand,
    LogoutSession, LogoutSessionCommand, PasswordPorts, RefreshSession, RefreshSessionCommand,
    RegisterUser, RegisterUserCommand, RequestPasswordReset, RequestPasswordResetCommand,
    ResetPassword, ResetPasswordCommand, SessionPorts, UserProfile,
};
pub use use_case::{CheckLiveness, CheckReadiness};
