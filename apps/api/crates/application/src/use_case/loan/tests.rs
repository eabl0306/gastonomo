//! Tests de los casos de uso de prestamos.
//!
//! Todos van contra el doble en memoria de [`super::testing`]: sin base de datos y
//! con un reloj fijo.

mod create_loan;
mod list_and_get;
mod register_payment;
mod support;
