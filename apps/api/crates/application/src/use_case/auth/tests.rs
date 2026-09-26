//! Tests de los casos de uso de autenticacion.
//!
//! Todos usan los dobles de [`crate::testing`]: sin base de datos, sin red y sin
//! esperar a que caduque nada.

mod change_password;
mod login_user;
mod password_reset;
mod refresh_session;
mod register_user;
mod support;
