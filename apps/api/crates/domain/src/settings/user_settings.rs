//! Agregado de ajustes de una cuenta.

use chrono::{DateTime, Utc};

use super::{DEFAULT_DISPLAY_CURRENCY, Language};
use crate::value_object::{CurrencyCode, UserId};

/// Ajustes que se sincronizan entre dispositivos.
///
/// Cambiar `display_currency` no toca ningun registro guardado: cada movimiento,
/// prestamo y abono conserva su importe original y su equivalencia. La moneda solo
/// decide como se muestran las cifras y cual se propone en el siguiente registro.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UserSettings {
    user_id: UserId,
    display_currency: CurrencyCode,
    language: Language,
    updated_at: DateTime<Utc>,
}

impl UserSettings {
    /// Ajustes con los que nace una cuenta: moneda `COP` e idioma espanol.
    ///
    /// # Panics
    ///
    /// No entra en panico: [`DEFAULT_DISPLAY_CURRENCY`] es un codigo valido y la
    /// rama imposible se cierra con `unreachable!`.
    #[must_use]
    pub fn defaults_for(user_id: UserId, now: DateTime<Utc>) -> Self {
        let Ok(display_currency) = CurrencyCode::parse(DEFAULT_DISPLAY_CURRENCY) else {
            unreachable!("the default display currency is a valid iso 4217 code")
        };
        Self {
            user_id,
            display_currency,
            language: Language::default(),
            updated_at: now,
        }
    }

    /// Reconstruye los ajustes leidos de la base de datos.
    #[must_use]
    pub const fn rehydrate(
        user_id: UserId,
        display_currency: CurrencyCode,
        language: Language,
        updated_at: DateTime<Utc>,
    ) -> Self {
        Self {
            user_id,
            display_currency,
            language,
            updated_at,
        }
    }

    /// Cambia la moneda de visualizacion.
    pub fn set_display_currency(&mut self, currency: CurrencyCode, now: DateTime<Utc>) {
        self.display_currency = currency;
        self.updated_at = now;
    }

    /// Cambia el idioma.
    pub const fn set_language(&mut self, language: Language, now: DateTime<Utc>) {
        self.language = language;
        self.updated_at = now;
    }

    /// Cuenta a la que pertenecen.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.user_id
    }

    /// Moneda con la que se muestran las cifras.
    #[must_use]
    pub const fn display_currency(&self) -> &CurrencyCode {
        &self.display_currency
    }

    /// Idioma de la app, que tambien fija el formato de numeros y fechas.
    #[must_use]
    pub const fn language(&self) -> Language {
        self.language
    }

    /// Instante del ultimo cambio.
    #[must_use]
    pub const fn updated_at(&self) -> DateTime<Utc> {
        self.updated_at
    }
}
