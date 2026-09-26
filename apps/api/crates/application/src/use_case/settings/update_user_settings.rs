//! Caso de uso: cambiar los ajustes de la cuenta.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::settings::{Language, UserSettings};
use gastonomo_domain::value_object::{CurrencyCode, UserId};

use crate::error::{AppError, ValidationErrors};
use crate::port::Clock;
use crate::port::settings::{DisplayCurrencyCatalog, UserSettingsRepository};

/// Cambio que llega en el `PATCH`.
///
/// Un campo ausente o nulo se queda como estaba: el sheet de moneda y el de idioma
/// son dos pantallas distintas y cada una manda solo lo suyo.
#[derive(Debug, Clone)]
pub struct UpdateUserSettingsCommand {
    /// Cuenta del token.
    pub user_id: UserId,
    /// Moneda principal nueva.
    pub display_currency: Option<String>,
    /// Idioma nuevo: `es` o `en`.
    pub language: Option<String>,
}

/// Cambia la moneda principal, el idioma o los dos.
///
/// Cambiar la moneda no toca ningun registro: solo cambia como se muestran las
/// cifras y cual se propone en el siguiente registro.
#[derive(Clone)]
pub struct UpdateUserSettings {
    settings: Arc<dyn UserSettingsRepository>,
    currencies: Arc<dyn DisplayCurrencyCatalog>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for UpdateUserSettings {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("UpdateUserSettings").finish_non_exhaustive()
    }
}

impl UpdateUserSettings {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(
        settings: Arc<dyn UserSettingsRepository>,
        currencies: Arc<dyn DisplayCurrencyCatalog>,
        clock: Arc<dyn Clock>,
    ) -> Self {
        Self {
            settings,
            currencies,
            clock,
        }
    }

    /// Guarda el cambio y devuelve los ajustes resultantes.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si la moneda no esta en el catalogo o el
    /// idioma no es `es` ni `en`, [`AppError::Unavailable`] si el catalogo todavia
    /// esta vacio y [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        command: UpdateUserSettingsCommand,
    ) -> Result<UserSettings, AppError> {
        let language = parse_language(command.language.as_deref())?;

        let now = self.clock.now();
        let mut settings = self
            .settings
            .find(command.user_id)
            .await?
            .unwrap_or_else(|| UserSettings::defaults_for(command.user_id, now));
        let currency = self
            .parse_currency(command.display_currency.as_deref(), &settings)
            .await?;
        if currency.is_none() && language.is_none() {
            return Ok(settings);
        }
        if let Some(currency) = currency {
            settings.set_display_currency(currency, now);
        }
        if let Some(language) = language {
            settings.set_language(language, now);
        }
        self.settings.save(&settings).await?;
        Ok(settings)
    }

    /// Lee la moneda pedida y comprueba que se puede elegir.
    ///
    /// La que la cuenta ya tiene puesta se acepta sin mirar el catalogo. El sheet de
    /// Ajustes ensena la moneda actual marcada y volver a tocarla no puede fallar,
    /// ni siquiera en un despliegue en limpio donde la tarea de tasas todavia no ha
    /// llenado `currencies`.
    async fn parse_currency(
        &self,
        raw: Option<&str>,
        settings: &UserSettings,
    ) -> Result<Option<CurrencyCode>, AppError> {
        let Some(raw) = raw else { return Ok(None) };
        let currency = CurrencyCode::parse(raw)
            .map_err(|_| unknown_currency("La moneda tiene que ser un codigo ISO de 3 letras."))?;
        if &currency == settings.display_currency()
            || self.currencies.is_selectable(&currency).await?
        {
            return Ok(Some(currency));
        }
        if self.currencies.is_ready().await? {
            return Err(unknown_currency(
                "Esa moneda no esta en el catalogo de monedas disponibles.",
            ));
        }
        // El catalogo esta vacio: no es que la moneda no valga, es que todavia no
        // hay con que compararla. Decir 422 aqui culparia al cliente de algo que no
        // puede arreglar.
        Err(AppError::unavailable(
            "the currency catalog is empty: the exchange rate task has not completed a pass yet",
        ))
    }
}

fn parse_language(raw: Option<&str>) -> Result<Option<Language>, AppError> {
    let Some(raw) = raw else { return Ok(None) };
    Language::parse(raw)
        .map(Some)
        .map_err(|_| AppError::validation("language", "El idioma tiene que ser `es` o `en`."))
}

fn unknown_currency(message: &str) -> AppError {
    let mut errors = ValidationErrors::new();
    errors.push("displayCurrency", message);
    AppError::Validation(errors)
}
