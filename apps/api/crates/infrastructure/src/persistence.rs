//! Adaptadores de persistencia sobre PostgreSQL.
//!
//! Cada contexto tiene ya su modulo declarado para que nadie tenga que tocar este
//! fichero.

mod mapping;
mod pool;
mod postgres_health_probe;
mod postgres_idempotency_store;
mod postgres_password_reset_token_repository;
mod postgres_refresh_token_repository;
mod postgres_user_provisioning;
mod postgres_user_repository;

mod postgres_category_repository;
mod postgres_change_feed;
mod postgres_dictation_access;
mod postgres_exchange_rate_repository;
mod postgres_loan_repository;
mod postgres_movement_repository;
mod postgres_user_settings_repository;

/// Pool de conexiones de SQLx, reexportado para el composition root.
pub use sqlx::PgPool;

pub use mapping::{database_error, is_unique_violation};
pub use pool::{PersistenceError, connect_pool, run_migrations};
pub use postgres_category_repository::PostgresCategoryRepository;
pub use postgres_change_feed::PostgresChangeFeed;
pub use postgres_dictation_access::PostgresDictationAccess;
pub use postgres_exchange_rate_repository::PostgresExchangeRateRepository;
pub use postgres_health_probe::PostgresHealthProbe;
pub use postgres_idempotency_store::PostgresIdempotencyStore;
pub use postgres_loan_repository::PostgresLoanRepository;
pub use postgres_movement_repository::PostgresMovementRepository;
pub use postgres_password_reset_token_repository::PostgresPasswordResetTokenRepository;
pub use postgres_refresh_token_repository::PostgresRefreshTokenRepository;
pub use postgres_user_provisioning::PostgresUserProvisioning;
pub use postgres_user_repository::PostgresUserRepository;
pub use postgres_user_settings_repository::{
    PostgresDisplayCurrencyCatalog, PostgresUserSettingsRepository,
};
