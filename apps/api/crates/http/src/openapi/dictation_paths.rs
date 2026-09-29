//! Documentacion OpenAPI del dictado.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, PathItemBuilder, PathsBuilder,
};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::{ComponentsBuilder, Required};

use crate::dto::dictation::{
    DictatedLoanDto, DictatedMovementDto, DictationDraftDto, DraftCategoryDto,
    ParseDictationRequestDto,
};

/// Etiqueta comun del dictado.
const TAG: &str = "dictation";

/// Anade la ruta de `/v1/dictation/parse`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder.path(
        "/v1/dictation/parse",
        PathItemBuilder::new()
            .operation(HttpMethod::Post, parse())
            .build(),
    )
}

/// Anade los esquemas del dictado.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<ParseDictationRequestDto>()
        .schema_from::<DictationDraftDto>()
        .schema_from::<DictatedMovementDto>()
        .schema_from::<DictatedLoanDto>()
        .schema_from::<DraftCategoryDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn parse() -> Operation {
    let responses = ResponsesBuilder::new()
        .response(
            "200",
            ResponseBuilder::new()
                .description("Borrador listo para revisar. No se ha creado nada.")
                .json_schema_ref("DictationDraftDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .response(
            "404",
            problem(
                "La cuenta no tiene el dictado aprobado. Es un 404 y no un 403 por la \
                 misma razon que el resto de la API: un 403 confirmaria que la \
                 capacidad existe.",
            ),
        )
        .response("422", problem("El texto viene vacio o es demasiado largo."))
        .response(
            "503",
            problem("El dictado no esta configurado en esta instancia."),
        )
        .response("500", problem("Fallo interno o el interprete no responde."))
        .build();
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("parseDictation"))
        .summary(Some("Interpretar un registro dictado"))
        .description(Some(
            "Convierte una frase dictada en un borrador de movimiento o de prestamo. \
             **No crea nada**: devuelve lo interpretado para que el usuario lo revise, \
             y confirmarlo es una llamada aparte a `POST /v1/movements` o \
             `POST /v1/loans` con su `Idempotency-Key`.\n\n\
             El borrador viene ya comprobado contra la cuenta: una categoria que no \
             existe cae en Otros, una moneda sin tasa cae en la principal y una fecha \
             futura cae en hoy. Lo que no se entendio se lista en `missing`, y el \
             cliente deja el boton de guardar apagado mientras no este vacio.\n\n\
             Solo esta disponible para las cuentas aprobadas.",
        ))
        .request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref("ParseDictationRequestDto"),
        ))
        .responses(responses)
        .build()
}
