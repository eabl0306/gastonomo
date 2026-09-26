//! Value objects del dominio.

mod component_name;
mod currency_code;
mod currency_decimals;
mod email;
mod latency;
mod monetary_amount;
mod money;
mod person_name;
mod service_version;
mod user_id;

pub use component_name::{ComponentName, ComponentNameError};
pub use currency_code::{CurrencyCode, CurrencyCodeError};
pub use currency_decimals::{ZERO_DECIMAL_CURRENCIES, decimals_for, is_zero_decimal};
pub use email::{Email, EmailError};
pub use latency::Latency;
pub use monetary_amount::{MonetaryAmount, MonetaryAmountError};
pub use money::{Money, MoneyError, STORAGE_SCALE};
pub use person_name::{PersonName, PersonNameError};
pub use service_version::{ServiceVersion, ServiceVersionError};
pub use user_id::{UserId, UserIdError};
