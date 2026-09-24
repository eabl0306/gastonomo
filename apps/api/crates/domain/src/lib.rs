//! Nucleo de dominio de Gastonomo.
//!
//! Este crate no conoce HTTP, SQL, Android ni ningun runtime asincrono. Solo
//! contiene value objects, agregados y reglas de negocio puras. La unica
//! dependencia externa permitida es `thiserror`, y un test de arquitectura lo
//! comprueba en cada build.

pub mod health;
pub mod value_object;

pub use health::{ComponentHealth, ComponentStatus, HealthReport, HealthStatus};
pub use value_object::{
    ComponentName, ComponentNameError, Latency, ServiceVersion, ServiceVersionError,
};
