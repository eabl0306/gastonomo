//! Adaptador driven: interpretar lo dictado con un modelo de Anthropic.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::dictation::DictationInterpreter;
use gastonomo_domain::dictation::{DictationContext, Interpretation};
use thiserror::Error;

use super::claude_payload::ClaudeResponse;
use super::prompt::{TOOL_NAME, system_prompt, tool_schema, user_message};
use crate::config::ClaudeConfig;

/// Identificacion que la API manda al modelo.
const USER_AGENT: &str = concat!("gastonomo-api/", env!("CARGO_PKG_VERSION"));

/// Version del protocolo de mensajes que se le pide a la API de Anthropic.
const API_VERSION: &str = "2023-06-01";

/// Fallo al construir el cliente del modelo.
#[derive(Debug, Error)]
pub enum InterpreterError {
    /// El cliente HTTP no se pudo construir.
    #[error("could not build the http client for the dictation model: {0}")]
    Client(#[source] Box<reqwest::Error>),
}

/// Interpreta lo dictado preguntandole a un modelo.
///
/// Es el unico sitio de la aplicacion que sabe que detras hay un modelo de
/// lenguaje: el nucleo solo conoce el puerto, que habla tipos de dominio. Cambiar
/// de proveedor no le llega.
#[derive(Debug, Clone)]
pub struct ClaudeDictationInterpreter {
    http: reqwest::Client,
    config: ClaudeConfig,
}

impl ClaudeDictationInterpreter {
    /// Crea el interprete con el tiempo maximo de la configuracion.
    ///
    /// # Errors
    ///
    /// Devuelve [`InterpreterError::Client`] si el cliente HTTP no se puede
    /// construir.
    pub fn new(config: ClaudeConfig) -> Result<Self, InterpreterError> {
        let http = reqwest::Client::builder()
            .timeout(config.request_timeout())
            .user_agent(USER_AGENT)
            .build()
            .map_err(|error| InterpreterError::Client(Box::new(error)))?;
        Ok(Self { http, config })
    }

    fn body(&self, text: &str, context: &DictationContext<'_>) -> serde_json::Value {
        serde_json::json!({
            "model": self.config.model,
            "max_tokens": self.config.max_tokens,
            "system": system_prompt(),
            "tools": [{
                "name": TOOL_NAME,
                "description": "Registra lo que el usuario dicto",
                "input_schema": tool_schema(),
            }],
            // Se le obliga a usar la herramienta. Sin esto contesta texto libre
            // cuando la frase es rara, y habria que adivinar donde esta el JSON.
            "tool_choice": { "type": "tool", "name": TOOL_NAME },
            "messages": [{ "role": "user", "content": user_message(text, context) }],
        })
    }
}

#[async_trait]
impl DictationInterpreter for ClaudeDictationInterpreter {
    async fn interpret(
        &self,
        text: &str,
        context: &DictationContext<'_>,
    ) -> Result<Interpretation, AppError> {
        if !self.config.is_configured() {
            return Err(AppError::unavailable("the dictation model has no api key"));
        }
        let url = format!("{}/v1/messages", self.config.base_url.trim_end_matches('/'));
        let response = self
            .http
            .post(url)
            .header("x-api-key", self.config.api_key.trim())
            .header("anthropic-version", API_VERSION)
            .json(&self.body(text, context))
            .send()
            .await
            .map_err(|error| {
                AppError::dependency(format!("the dictation model is unreachable: {error}"))
            })?;

        let status = response.status();
        if !status.is_success() {
            return Err(AppError::dependency(format!(
                "the dictation model answered {status}"
            )));
        }
        let payload = response.json::<ClaudeResponse>().await.map_err(|error| {
            AppError::dependency(format!(
                "the dictation model answered something unreadable: {error}"
            ))
        })?;
        Ok(payload.into_interpretation(TOOL_NAME))
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use chrono::NaiveDate;
    use gastonomo_domain::value_object::CurrencyCode;

    use super::*;

    /// Una clave cualquiera. Lo unico que importa de ella es que no este vacia.
    const ALGUNA_CLAVE: &str = "no-es-real";

    fn a_context() -> DictationContext<'static> {
        DictationContext {
            display_currency: CurrencyCode::parse("COP").expect("COP es valido"),
            convertible: &[],
            categories: &[],
            today: NaiveDate::from_ymd_opt(2026, 9, 29).expect("fecha valida"),
        }
    }

    /// Sin clave el dictado no se ofrece, y se dice con un 503: no ha fallado nada,
    /// falta un dato de configuracion. Y **no se sale a la red** a comprobarlo.
    #[tokio::test]
    async fn without_an_api_key_it_answers_unavailable() {
        let interpreter =
            ClaudeDictationInterpreter::new(ClaudeConfig::default()).expect("deberia construirse");

        let error = interpreter
            .interpret("gaste 45 mil", &a_context())
            .await
            .expect_err("deberia rechazar");

        assert!(
            matches!(error, AppError::Unavailable { .. }),
            "fue {error:?}"
        );
    }

    /// El puerto 9 es el descarte de TCP: nadie escucha y la conexion se cae
    /// enseguida, asi que este test no sale a internet ni espera al timeout.
    #[tokio::test]
    async fn a_model_that_does_not_answer_is_a_dependency_failure() {
        let interpreter = ClaudeDictationInterpreter::new(ClaudeConfig {
            api_key: ALGUNA_CLAVE.to_owned(),
            base_url: "http://127.0.0.1:9".to_owned(),
            request_timeout_ms: 300,
            ..ClaudeConfig::default()
        })
        .expect("deberia construirse");

        let error = interpreter
            .interpret("gaste 45 mil", &a_context())
            .await
            .expect_err("deberia fallar");

        assert!(
            matches!(error, AppError::Dependency { .. }),
            "fue {error:?}"
        );
    }

    /// El cuerpo se comprueba sin salir a la red: es una funcion pura y es donde
    /// esta lo que de verdad importa, que se obligue a usar la herramienta.
    #[test]
    fn the_request_forces_the_tool_and_carries_the_context() {
        let interpreter = ClaudeDictationInterpreter::new(ClaudeConfig {
            api_key: ALGUNA_CLAVE.to_owned(),
            ..ClaudeConfig::default()
        })
        .expect("deberia construirse");

        let body = interpreter.body("gaste 45 mil en almuerzo", &a_context());

        assert_eq!(body["tool_choice"]["type"], "tool");
        assert_eq!(body["tool_choice"]["name"], TOOL_NAME);
        let message = body["messages"][0]["content"]
            .as_str()
            .expect("el mensaje es texto");
        assert!(message.contains("2026-09-29"), "falta la fecha: {message}");
        assert!(message.contains("COP"), "falta la moneda: {message}");
        assert!(message.contains("gaste 45 mil en almuerzo"));
    }
}
