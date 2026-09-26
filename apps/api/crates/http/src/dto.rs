//! Objetos de transferencia del contrato publico.
//!
//! Las convenciones (camelCase, importes como string, fechas `YYYY-MM-DD`) estan en
//! [`crate::serialization`]. Cada contexto tiene ya su modulo declarado.

mod health_response;

pub mod auth;
pub mod category;
pub mod currency;
pub mod loan;
pub mod movement;
pub mod settings;

pub use auth::{
    ChangePasswordRequestDto, ForgotPasswordRequestDto, LoginRequestDto, RefreshTokenRequestDto,
    RegisterRequestDto, ResetPasswordRequestDto, SessionDto, UserDto,
};
pub use health_response::{ComponentCheckDto, HealthResponseDto};
