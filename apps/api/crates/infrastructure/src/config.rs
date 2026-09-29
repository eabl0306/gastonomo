//! Configuracion tipada leida del entorno.

mod app_config;
mod auth_config;
mod claude_config;
mod exchange_rates_config;
mod mail_config;

pub use app_config::{AppConfig, ConfigError, DatabaseConfig, ServerConfig, TelemetryConfig};
pub use auth_config::{AuthConfig, DEVELOPMENT_SECRET};
pub use claude_config::ClaudeConfig;
pub use exchange_rates_config::ExchangeRatesConfig;
pub use mail_config::{MailConfig, MailDriver};
