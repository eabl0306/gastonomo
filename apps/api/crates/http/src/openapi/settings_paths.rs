//! Documentacion OpenAPI del contexto de ajustes.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, PathItemBuilder, PathsBuilder,
};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::{ComponentsBuilder, Required};

use crate::dto::settings::{SettingsDto, UpdateSettingsRequestDto};

/// Etiqueta comun de los endpoints de ajustes.
const TAG: &str = "settings";

/// Anade las rutas de `/v1/settings`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder.path(
        "/v1/settings",
        PathItemBuilder::new()
            .operation(HttpMethod::Get, read())
            .operation(HttpMethod::Patch, write())
            .build(),
    )
}

/// Anade los esquemas de ajustes.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<SettingsDto>()
        .schema_from::<UpdateSettingsRequestDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn settings_response(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("SettingsDto")
}

fn read() -> Operation {
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("getSettings"))
        .summary(Some("Ajustes de la cuenta"))
        .description(Some(
            "Devuelve la moneda principal y el idioma, que es todo lo que se sincroniza. El \
             bloqueo con huella o PIN protege un secreto que vive en ese telefono y el ojo que \
             oculta los montos es estado de la pantalla: ninguno de los dos viaja. El \
             indicador de sincronizacion tampoco: es estado de conectividad del cliente.",
        ))
        .responses(
            ResponsesBuilder::new()
                .response("200", settings_response("Ajustes de la cuenta."))
                .response("401", problem("Falta el access token o no vale."))
                .response("500", problem("Fallo interno."))
                .response(
                    "503",
                    problem(
                        "El catalogo de monedas esta vacio todavia: la tarea de tasas no ha \
                         terminado su primera pasada. La moneda que la cuenta ya tiene \
                         puesta se acepta igual.",
                    ),
                )
                .build(),
        )
        .build()
}

fn write() -> Operation {
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("updateSettings"))
        .summary(Some("Cambiar moneda o idioma"))
        .description(Some(
            "Cambia una de las dos o las dos: lo que no viene se queda como estaba. La moneda \
             tiene que existir en `GET /v1/currencies`. Cambiar la moneda NO toca ningun \
             registro guardado: cada movimiento, prestamo y abono conserva su importe original \
             y su equivalencia, y la moneda solo decide como se muestran las cifras y cual se \
             propone en el siguiente registro.",
        ))
        .request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref("UpdateSettingsRequestDto"),
        ))
        .responses(
            ResponsesBuilder::new()
                .response("200", settings_response("Ajustes guardados."))
                .response("401", problem("Falta el access token o no vale."))
                .response(
                    "422",
                    problem("La moneda no esta en el catalogo o el idioma no es `es` ni `en`."),
                )
                .response("500", problem("Fallo interno."))
                .response(
                    "503",
                    problem(
                        "El catalogo de monedas esta vacio todavia: la tarea de tasas no ha \
                         terminado su primera pasada. La moneda que la cuenta ya tiene \
                         puesta se acepta igual.",
                    ),
                )
                .build(),
        )
        .build()
}
