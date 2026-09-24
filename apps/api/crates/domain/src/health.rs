//! Agregado de salud del servicio.

mod component_health;
mod report;

pub use component_health::{ComponentHealth, ComponentStatus};
pub use report::{HealthReport, HealthStatus};
