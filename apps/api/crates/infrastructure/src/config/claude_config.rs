//! Configuracion del interprete del dictado.

use std::time::Duration;

use serde::{Deserialize, Serialize};

/// Modelo por defecto.
///
/// Haiku es el pequeno de la familia: interpretar una frase y devolver unos pocos
/// campos no necesita mas, y esta capacidad se paga por palabra.
const DEFAULT_MODEL: &str = "claude-haiku-4-5-20251001";

/// Configuracion de la llamada al modelo que interpreta lo dictado.
///
/// La clave **no tiene valor por defecto util**: sin ella el dictado responde 503 y
/// el resto de la API sigue funcionando igual. Es lo mismo que hace el catalogo de
/// monedas cuando la tarea de tasas no ha dado su primera pasada, y evita que una
/// capacidad opcional pueda impedir que el proceso arranque.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct ClaudeConfig {
    /// Clave de la API de Anthropic. Vacia significa dictado apagado.
    pub api_key: String,
    /// Raiz de la API, sin barra final.
    pub base_url: String,
    /// Modelo que interpreta.
    pub model: String,
    /// Tiempo maximo de la llamada, en milisegundos.
    ///
    /// El cliente ensena "Interpretando..." mientras tanto, asi que el techo es lo
    /// que una persona aguanta mirando una animacion, no lo que el modelo tarda.
    pub request_timeout_ms: u64,
    /// Tope de palabras de la respuesta.
    ///
    /// La respuesta es un objeto de ocho campos cortos. Un tope bajo no la recorta
    /// y pone un limite duro a lo que puede costar una peticion.
    pub max_tokens: u32,
}

impl Default for ClaudeConfig {
    fn default() -> Self {
        Self {
            api_key: String::new(),
            base_url: "https://api.anthropic.com".to_owned(),
            model: DEFAULT_MODEL.to_owned(),
            request_timeout_ms: 12_000,
            max_tokens: 512,
        }
    }
}

impl ClaudeConfig {
    /// Si el dictado se puede ofrecer.
    #[must_use]
    pub fn is_configured(&self) -> bool {
        !self.api_key.trim().is_empty()
    }

    /// Tiempo maximo de la llamada.
    #[must_use]
    pub const fn request_timeout(&self) -> Duration {
        Duration::from_millis(self.request_timeout_ms)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Una clave cualquiera. Lo unico que importa de ella es que no este vacia.
    const ALGUNA_CLAVE: &str = "no-es-real";

    /// Sin clave el dictado no se ofrece, pero la API arranca igual: es una
    /// capacidad opcional y no puede tumbar el proceso.
    #[test]
    fn without_a_key_the_capability_is_off() {
        assert!(!ClaudeConfig::default().is_configured());

        let configured = ClaudeConfig {
            api_key: ALGUNA_CLAVE.to_owned(),
            ..ClaudeConfig::default()
        };
        assert!(configured.is_configured());
    }

    #[test]
    fn a_blank_key_counts_as_no_key() {
        let blank = ClaudeConfig {
            api_key: "   ".to_owned(),
            ..ClaudeConfig::default()
        };
        assert!(!blank.is_configured());
    }
}
