//! Nucleo de dominio de Gastonomo.
//!
//! Este crate no conoce HTTP, SQL, Android ni ningun runtime asincrono. Solo
//! contiene value objects, agregados y reglas de negocio puras. Sus unicas
//! dependencias son librerias igual de puras (`thiserror`, `chrono`,
//! `rust_decimal` y `uuid`), y un test de arquitectura lo comprueba en cada build.
//!
//! Los modulos por contexto (`movement`, `loan`, `category`, `settings`,
//! `currency`) ya estan declarados para que cada contexto anada sus agregados sin
//! tocar este fichero.

pub mod auth;
pub mod category;
pub mod currency;
pub mod dictation;
pub mod health;
pub mod loan;
pub mod movement;
pub mod settings;
pub mod value_object;

pub use auth::{
    MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH, NewPasswordResetToken, NewRefreshToken, NewUser,
    Password, PasswordError, PasswordHash, PasswordResetToken, PasswordResetTokenId,
    RESET_TOKEN_MINUTES, RefreshToken, RefreshTokenId, StoredRefreshToken, TokenDigest,
    TokenDigestError, User,
};
pub use category::{
    CategoryKind, CategoryKindError, DEFAULT_CATEGORIES, DefaultCategory, FALLBACK_CATEGORY_NAME,
};
pub use health::{ComponentHealth, ComponentStatus, HealthReport, HealthStatus};
pub use settings::{DEFAULT_DISPLAY_CURRENCY, Language, LanguageError};
pub use value_object::{
    ComponentName, ComponentNameError, CurrencyCode, CurrencyCodeError, Email, EmailError, Latency,
    MonetaryAmount, MonetaryAmountError, Money, MoneyError, PersonName, PersonNameError,
    ServiceVersion, ServiceVersionError, UserId, UserIdError,
};
