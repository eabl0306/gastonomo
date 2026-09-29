//! Caso de uso: interpretar un registro dictado.

use std::fmt;
use std::sync::Arc;

use chrono::NaiveDate;
use gastonomo_domain::dictation::{
    AvailableCategory, DictationContext, DictationDraft, Interpretation,
};
use gastonomo_domain::settings::DEFAULT_DISPLAY_CURRENCY;
use gastonomo_domain::value_object::{CurrencyCode, UserId};

use crate::error::{AppError, ValidationErrors};
use crate::port::Clock;
use crate::port::category::CategoryRepository;
use crate::port::currency::ExchangeRateRepository;
use crate::port::dictation::{DictationAccess, DictationInterpreter};
use crate::port::settings::UserSettingsRepository;

/// Lo maximo que se acepta dictar de una vez.
///
/// Un registro dictado cabe en una frase. El limite no es por el modelo, es para
/// que nadie pueda mandar un libro entero a una capacidad que se paga por palabra.
pub const MAX_DICTATION_LENGTH: usize = 500;

/// El texto dictado y el dia del dispositivo.
#[derive(Debug, Clone)]
pub struct ParseDictationCommand {
    /// Lo que el usuario dijo, ya pasado a texto por el cliente.
    pub text: String,
    /// Hoy segun el dispositivo. Sin el se usa el del servidor.
    pub today: Option<NaiveDate>,
}

/// Puertos que necesita el dictado.
#[derive(Clone)]
pub struct ParseDictationPorts {
    /// Quien tiene el dictado aprobado.
    pub access: Arc<dyn DictationAccess>,
    /// Quien interpreta el texto.
    pub interpreter: Arc<dyn DictationInterpreter>,
    /// Ajustes de la cuenta, de donde sale la moneda principal.
    pub settings: Arc<dyn UserSettingsRepository>,
    /// Categorias de la cuenta, contra las que se comprueba lo dictado.
    pub categories: Arc<dyn CategoryRepository>,
    /// Tasas vigentes, para saber que monedas se pueden equivaler.
    pub rates: Arc<dyn ExchangeRateRepository>,
    /// Reloj, para cuando el cliente no manda su dia.
    pub clock: Arc<dyn Clock>,
}

impl fmt::Debug for ParseDictationPorts {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ParseDictationPorts")
            .finish_non_exhaustive()
    }
}

/// Convierte lo dictado en un borrador listo para revisar.
#[derive(Clone)]
pub struct ParseDictation {
    ports: ParseDictationPorts,
}

impl fmt::Debug for ParseDictation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ParseDictation").finish_non_exhaustive()
    }
}

impl ParseDictation {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: ParseDictationPorts) -> Self {
        Self { ports }
    }

    /// Interpreta lo dictado.
    ///
    /// **No escribe nada.** Devuelve un borrador que el usuario revisa, y crear el
    /// registro es una llamada aparte a los endpoints de siempre, con su clave de
    /// idempotencia. El historico es de solo anadir y no puede depender de que un
    /// modelo acierte a la primera.
    ///
    /// # Errors
    ///
    /// - [`AppError::NotFound`] si la cuenta no tiene el dictado aprobado. Es un
    ///   404 y no un 403 por la misma razon que el resto de la API: un 403
    ///   confirmaria que la capacidad existe.
    /// - [`AppError::Validation`] si el texto viene vacio o pasa de
    ///   [`MAX_DICTATION_LENGTH`].
    /// - [`AppError::Dependency`] si falla la base de datos o el interprete.
    pub async fn execute(
        &self,
        user_id: UserId,
        command: ParseDictationCommand,
    ) -> Result<DictationDraft, AppError> {
        if !self.ports.access.is_allowed(user_id).await? {
            return Err(AppError::not_found("dictation"));
        }
        let text = parse_text(&command.text)?;
        let categories = self.categories_of(user_id).await?;
        let convertible = self.convertible().await?;
        let context = DictationContext {
            display_currency: self.display_currency(user_id).await?,
            convertible: &convertible,
            categories: &categories,
            today: command
                .today
                .unwrap_or_else(|| self.ports.clock.now().date_naive()),
        };
        let interpretation: Interpretation =
            self.ports.interpreter.interpret(text, &context).await?;
        Ok(DictationDraft::normalize(&interpretation, &context))
    }

    async fn categories_of(&self, user_id: UserId) -> Result<Vec<AvailableCategory>, AppError> {
        Ok(self
            .ports
            .categories
            .list_with_usage(user_id, true)
            .await?
            .into_iter()
            .map(|usage| AvailableCategory {
                id: usage.category.id(),
                name: usage.category.name().as_str().to_owned(),
                kind: usage.category.kind(),
            })
            .collect())
    }

    /// Las monedas que hoy se pueden equivaler.
    ///
    /// Sin catalogo no se cae la peticion: la lista queda vacia y todo lo dictado va
    /// en la moneda principal, que es lo mismo que hace la aplicacion sin conexion.
    async fn convertible(&self) -> Result<Vec<CurrencyCode>, AppError> {
        Ok(self
            .ports
            .rates
            .offered_currencies()
            .await?
            .iter()
            .map(|currency| currency.code().clone())
            .collect())
    }

    /// La moneda principal de la cuenta, o la que trae una cuenta nueva si todavia
    /// no tiene fila de ajustes.
    async fn display_currency(&self, user_id: UserId) -> Result<CurrencyCode, AppError> {
        if let Some(settings) = self.ports.settings.find(user_id).await? {
            return Ok(settings.display_currency().clone());
        }
        CurrencyCode::parse(DEFAULT_DISPLAY_CURRENCY)
            .map_err(|_| AppError::dependency("the default display currency is not a valid code"))
    }
}

fn parse_text(raw: &str) -> Result<&str, AppError> {
    let text = raw.trim();
    let mut errors = ValidationErrors::new();
    if text.is_empty() {
        errors.push("text", "Escribe o dicta lo que quieres registrar.");
    } else if text.chars().count() > MAX_DICTATION_LENGTH {
        errors.push(
            "text",
            "Es demasiado largo para un solo registro. Divide lo que quieres apuntar.",
        );
    }
    errors.into_result()?;
    Ok(text)
}
