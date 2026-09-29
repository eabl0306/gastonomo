//! Puertos declarados por el nucleo e implementados por los adaptadores.
//!
//! Cada contexto tiene ya su modulo declarado para que nadie tenga que tocar este
//! fichero: `movement`, `loan`, `category`, `settings` y `currency`.

mod clock;
mod health_probe;
mod idempotency;
mod mailer;
mod opaque_token_factory;
mod password_hasher;
mod password_reset_token_repository;
mod refresh_token_repository;
mod token_issuer;
mod user_provisioning;
mod user_repository;

pub mod category;
pub mod changes;
pub mod currency;
pub mod dictation;
pub mod loan;
pub mod movement;
pub mod settings;

pub use clock::Clock;
pub use health_probe::{HealthProbe, ProbeError};
pub use idempotency::{
    IDEMPOTENCY_RETENTION_HOURS, IdempotencyStore, IdempotentRequest, IdempotentResponse,
    MAX_IDEMPOTENCY_KEY_LENGTH, Reservation,
};
pub use mailer::{MailMessage, Mailer};
pub use opaque_token_factory::{OpaqueToken, OpaqueTokenFactory};
pub use password_hasher::PasswordHasher;
pub use password_reset_token_repository::PasswordResetTokenRepository;
pub use refresh_token_repository::RefreshTokenRepository;
pub use token_issuer::{AccessToken, TokenIssuer};
pub use user_provisioning::UserProvisioning;
pub use user_repository::UserRepository;
