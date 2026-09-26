//! DTOs del contexto de movimientos.
//!
//! Convenciones en [`crate::serialization`]: camelCase, importes como string,
//! fechas de registro `YYYY-MM-DD` e instantes tecnicos en RFC 3339.

mod create_request;
mod response;

pub use create_request::CreateMovementRequestDto;
pub use response::{MovementCategoryDto, MovementDto, MovementPageDto};
