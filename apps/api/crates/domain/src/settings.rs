//! Ajustes sincronizados del usuario.
//!
//! Solo se sincronizan la moneda de visualizacion y el idioma. El bloqueo por PIN
//! o huella protege un secreto que vive en un telefono concreto, y el ojo que
//! oculta los montos es estado efimero de la tarjeta de balance: ninguno de los
//! dos es un campo de la API.
//!
//! El contexto de ajustes completa este modulo con el agregado `UserSettings`.

mod user_settings;

use std::fmt;

use thiserror::Error;

pub use user_settings::UserSettings;

/// Moneda de visualizacion con la que nace una cuenta.
pub const DEFAULT_DISPLAY_CURRENCY: &str = "COP";

/// El texto recibido no es un idioma soportado.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{got:?} is not a supported language")]
pub struct LanguageError {
    /// Valor recibido.
    pub got: String,
}

/// Idioma de la app, que tambien fija el formato de numeros y fechas.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Language {
    /// Espanol, con formato `es-CO`.
    #[default]
    Spanish,
    /// Ingles, con formato `en-US`.
    English,
}

impl Language {
    /// Valor tal y como se guarda en la columna `language`.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Spanish => "es",
            Self::English => "en",
        }
    }

    /// Etiqueta de locale que usa el cliente para formatear.
    #[must_use]
    pub const fn locale(self) -> &'static str {
        match self {
            Self::Spanish => "es-CO",
            Self::English => "en-US",
        }
    }

    /// Lee el idioma de su representacion textual.
    ///
    /// # Errors
    ///
    /// Devuelve [`LanguageError`] si el texto no es `es` ni `en`.
    pub fn parse(raw: &str) -> Result<Self, LanguageError> {
        match raw.trim() {
            "es" => Ok(Self::Spanish),
            "en" => Ok(Self::English),
            other => Err(LanguageError {
                got: other.to_owned(),
            }),
        }
    }
}

impl fmt::Display for Language {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}
