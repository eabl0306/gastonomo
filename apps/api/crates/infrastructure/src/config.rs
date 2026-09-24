//! Configuracion tipada leida del entorno.

mod app_config;

pub use app_config::{AppConfig, ConfigError, DatabaseConfig, ServerConfig, TelemetryConfig};
