//! Documentacion OpenAPI del contexto de movimientos.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, ParameterBuilder, ParameterIn, PathItemBuilder,
    PathsBuilder,
};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::schema::ObjectBuilder;
use utoipa::openapi::{ComponentsBuilder, Required, Type};

use super::idempotency_docs;
use crate::dto::movement::{
    CreateMovementRequestDto, MovementCategoryDto, MovementDto, MovementPageDto,
};

/// Etiqueta comun de los endpoints de movimientos.
const TAG: &str = "movements";

/// Anade las rutas de `/v1/movements`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder.path(
        "/v1/movements",
        PathItemBuilder::new()
            .operation(HttpMethod::Get, list())
            .operation(HttpMethod::Post, create())
            .build(),
    )
}

/// Anade los esquemas de movimientos.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<MovementPageDto>()
        .schema_from::<MovementDto>()
        .schema_from::<MovementCategoryDto>()
        .schema_from::<CreateMovementRequestDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn query_parameter(name: &str, description: &str) -> ParameterBuilder {
    ParameterBuilder::new()
        .name(name)
        .parameter_in(ParameterIn::Query)
        .required(Required::False)
        .description(Some(description))
        .schema(Some(ObjectBuilder::new().schema_type(Type::String)))
}

fn list() -> Operation {
    let responses = ResponsesBuilder::new()
        .response(
            "200",
            ResponseBuilder::new()
                .description("Pagina de movimientos.")
                .json_schema_ref("MovementPageDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .response("422", problem("Algun parametro de la query no vale."))
        .response("500", problem("Fallo interno."))
        .build();
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("listMovements"))
        .summary(Some("Movimientos del periodo"))
        .description(Some(
            "Devuelve los movimientos de la cuenta del token ordenados por fecha \
             descendente, que es como los agrupa por dia la pantalla de Movimientos. \
             La pantalla de Inicio pide los cuatro ultimos con `limit=4`, y los \
             totales, la tendencia y el desglose por categoria los calcula el \
             cliente sobre esta misma lista: dependen de la moneda principal del \
             momento y de las tasas de hoy, que son decision de presentacion. \
             La paginacion es por cursor sobre (occurred_on desc, id desc), estable \
             aunque se inserten movimientos mientras se pagina.",
        ))
        .parameter(query_parameter(
            "from",
            "Primer dia del periodo, inclusive (YYYY-MM-DD).",
        ))
        .parameter(query_parameter(
            "to",
            "Ultimo dia del periodo, inclusive (YYYY-MM-DD).",
        ))
        .parameter(query_parameter(
            "kind",
            "`in`, `out`, o se omite para las pills Todos.",
        ))
        .parameter(query_parameter(
            "cursor",
            "Valor `nextCursor` de la pagina anterior. Es opaco.",
        ))
        .parameter(query_parameter(
            "limit",
            "Filas por pagina. Por defecto 100 y como maximo 500; un valor mayor se acota.",
        ))
        .responses(responses)
        .build()
}

fn create() -> Operation {
    let responses = ResponsesBuilder::new()
        .response(
            "201",
            ResponseBuilder::new()
                .description("Movimiento registrado.")
                .json_schema_ref("MovementDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .response("409", problem(idempotency_docs::CONFLICT))
        .response(
            "422",
            problem(
                "El cuerpo no cumple las reglas: importe o equivalencia no positivos, \
                 fecha fuera de rango, o una categoria que no es de la cuenta o que no \
                 sirve para este tipo de movimiento.",
            ),
        )
        .response("500", problem("Fallo interno."))
        .build();
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("createMovement"))
        .summary(Some("Nuevo movimiento"))
        .description(Some(
            "Guarda un ingreso o un gasto. Si no hay nota, el titulo es el nombre de \
             la categoria. Los cuatro campos de importe se guardan tal cual, sin \
             recalcular nada: `base` es la equivalencia que el usuario acepto o \
             edito en la caja de conversion.",
        ))
        .parameter(idempotency_docs::parameter())
        .request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref("CreateMovementRequestDto"),
        ))
        .responses(responses)
        .build()
}
