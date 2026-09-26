//! Adaptadores driven de Gastonomo.
//!
//! Aqui viven las piezas que hablan con el mundo exterior: configuracion por
//! entorno, telemetria y PostgreSQL. El nucleo nunca importa este crate; es el
//! composition root quien conecta estos adaptadores con los casos de uso.

pub mod clock;
pub mod config;
pub mod exchange_rate;
pub mod mail;
pub mod persistence;
pub mod security;
pub mod telemetry;

pub use clock::SystemClock;
pub use config::{
    AppConfig, AuthConfig, ConfigError, DEVELOPMENT_SECRET, DatabaseConfig, ExchangeRatesConfig,
    MailConfig, MailDriver, ServerConfig, TelemetryConfig,
};
pub use exchange_rate::{ExchangeRateScheduler, FrankfurterExchangeRateProvider, ProviderError};
pub use mail::{LoggingMailer, MailError, SmtpMailer, build_mailer};
pub use persistence::{
    PersistenceError, PgPool, PostgresExchangeRateRepository, PostgresHealthProbe,
    PostgresIdempotencyStore, PostgresPasswordResetTokenRepository, PostgresRefreshTokenRepository,
    PostgresUserProvisioning, PostgresUserRepository, connect_pool, run_migrations,
};
pub use security::{
    Argon2PasswordHashing, JwtTokenIssuer, RandomOpaqueTokenFactory, install_jwt_provider,
};
pub use telemetry::{TelemetryError, init_tracing};
