//! Cuerpo de `POST /v1/dictation/parse`.

use gastonomo_application::use_case::dictation::ParseDictationCommand;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Lo que el cliente manda despues de pasar la voz a texto.
///
/// El paso de voz a texto lo hace el cliente con el reconocimiento del sistema, que
/// ya sabe el idioma y no cuesta nada. Aqui solo llega el texto.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ParseDictationRequestDto {
    /// Lo que el usuario dijo.
    #[schema(example = "gasté 45 mil en el almuerzo")]
    pub text: String,
    /// Hoy segun el dispositivo, en `YYYY-MM-DD`.
    ///
    /// Lo manda el cliente porque **la zona horaria es suya**: el servidor no sabe
    /// si para quien dicta ya es manana. Solo se usa para resolver "ayer" y para
    /// cortar las fechas futuras. Si no viene, se usa el dia del servidor.
    #[schema(example = "2026-09-29")]
    pub today: Option<String>,
}

impl ParseDictationRequestDto {
    /// Pasa el cuerpo al comando del caso de uso.
    ///
    /// Una fecha ilegible se ignora en vez de rechazar la peticion: el dia del
    /// servidor sirve, y tumbar un dictado por el formato de un campo auxiliar
    /// seria perder lo que el usuario acaba de decir.
    #[must_use]
    pub fn into_command(self) -> ParseDictationCommand {
        ParseDictationCommand {
            text: self.text,
            today: self
                .today
                .as_deref()
                .and_then(|raw| chrono::NaiveDate::parse_from_str(raw.trim(), "%Y-%m-%d").ok()),
        }
    }
}
