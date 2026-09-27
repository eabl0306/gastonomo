//! Dobles de test para los puertos de la aplicacion.
//!
//! Solo se compila con la feature `testing` (o dentro de los tests de este
//! crate). Permite ejercitar los casos de uso sin base de datos, sin red y sin
//! esperar a que caduque nada.

mod block_on;
mod fake_health_probe;
mod fake_opaque_token_factory;
mod fake_password_hasher;
mod fake_token_issuer;
mod fixed_clock;
mod in_memory_category_repository;
mod in_memory_change_feed;
mod in_memory_exchange_rate_repository;
mod in_memory_idempotency_store;
mod in_memory_movement_repository;
mod in_memory_password_reset_token_repository;
mod in_memory_refresh_token_repository;
mod in_memory_user_repository;
mod in_memory_user_settings_repository;
mod recording_mailer;
mod recording_user_provisioning;
mod stub_exchange_rate_provider;
mod stub_movement_category_reader;

pub use block_on::block_on;
pub use fake_health_probe::FakeHealthProbe;
pub use fake_opaque_token_factory::FakeOpaqueTokenFactory;
pub use fake_password_hasher::FakePasswordHasher;
pub use fake_token_issuer::FakeTokenIssuer;
pub use fixed_clock::FixedClock;
pub use in_memory_category_repository::InMemoryCategoryRepository;
pub use in_memory_change_feed::InMemoryChangeFeed;
pub use in_memory_exchange_rate_repository::InMemoryExchangeRateRepository;
pub use in_memory_idempotency_store::InMemoryIdempotencyStore;
pub use in_memory_movement_repository::InMemoryMovementRepository;
pub use in_memory_password_reset_token_repository::InMemoryPasswordResetTokenRepository;
pub use in_memory_refresh_token_repository::InMemoryRefreshTokenRepository;
pub use in_memory_user_repository::InMemoryUserRepository;
pub use in_memory_user_settings_repository::{
    FakeDisplayCurrencyCatalog, InMemoryUserSettingsRepository,
};
pub use recording_mailer::RecordingMailer;
pub use recording_user_provisioning::RecordingUserProvisioning;
pub use stub_exchange_rate_provider::StubExchangeRateProvider;
pub use stub_movement_category_reader::StubMovementCategoryReader;
