//! Puertos declarados por el nucleo e implementados por los adaptadores.

mod health_probe;

pub use health_probe::{HealthProbe, ProbeError};
