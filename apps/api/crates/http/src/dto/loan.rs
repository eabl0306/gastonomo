//! DTOs del contexto de prestamos.
//!
//! Convenciones en [`crate::serialization`]: claves en camelCase, importes como
//! string, fechas de registro `YYYY-MM-DD` e instantes tecnicos en RFC 3339.
//!
//! Los abonos viajan dentro del prestamo. No es una comodidad: la tarjeta del
//! diseno ensena el saldo, y el saldo es el prestamo menos la suma de los abonos
//! convertidos con las tasas de hoy, asi que sin ellos el cliente no puede pintar
//! ni la lista.

mod request;
mod response;

pub use request::{CreateLoanPaymentRequestDto, CreateLoanRequestDto};
pub use response::{LoanDto, LoanListDto, LoanPaymentDto};
