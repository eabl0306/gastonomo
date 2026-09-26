//! Casos de uso del contexto de ajustes.
//!
//! Solo dos, y a proposito: la pantalla de Ajustes sincroniza la moneda principal y
//! el idioma, y nada mas. El bloqueo por huella o PIN protege un secreto que vive
//! en un telefono concreto, y el ojo que oculta los montos es estado efimero de la
//! tarjeta de balance.

mod get_user_settings;
mod update_user_settings;

#[cfg(test)]
mod tests;

pub use get_user_settings::GetUserSettings;
pub use update_user_settings::{UpdateUserSettings, UpdateUserSettingsCommand};
