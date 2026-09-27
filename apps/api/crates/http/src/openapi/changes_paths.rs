//! Documentacion OpenAPI del feed de cambios.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, ParameterBuilder, ParameterIn, PathItemBuilder,
    PathsBuilder,
};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::schema::ObjectBuilder;
use utoipa::openapi::{ComponentsBuilder, Required, Type};

use crate::dto::changes::ChangeBatchDto;

/// Etiqueta del endpoint de cambios.
const TAG: &str = "changes";

/// Anade la ruta de `/v1/changes`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder.path(
        "/v1/changes",
        PathItemBuilder::new()
            .operation(HttpMethod::Get, list())
            .build(),
    )
}

/// Anade el esquema del lote de cambios.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder.schema_from::<ChangeBatchDto>()
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
                .description("Lote de cambios de la cuenta.")
                .json_schema_ref("ChangeBatchDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .response("422", problem("El cursor o el tamano de lote no valen."))
        .response("500", problem("Fallo interno."))
        .build();
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("getChanges"))
        .summary(Some("Que ha cambiado desde la ultima vez"))
        .description(Some(
            "Devuelve lo que ha cambiado en la cuenta desde el cursor que trae el \
             cliente, para que un arranque no tenga que bajarse el historico entero. \
             Como el historico es de solo anadir, \"cambios\" es casi siempre \"cosas \
             creadas despues de esta marca\"; lo unico que muta son las categorias \
             (renombrar, archivar y recuperar) y los ajustes. Las tasas no entran \
             aqui: tienen su propio endpoint.\n\n\
             **El cursor es opaco.** Es un numero de cambio por cuenta, no una marca \
             de tiempo: dos filas creadas en el mismo milisegundo, o un reloj que se \
             ajusta hacia atras, harian que el cliente se saltara registros sin \
             enterarse. Se guarda tal cual y se devuelve tal cual; un cursor \
             fabricado a mano se rechaza con 422 en vez de leerse como arranque en \
             frio, que haria bajar todo otra vez sin saber por que.\n\n\
             **El cliente avanza su cursor despues de guardar el lote**, nunca al \
             recibirlo: si la escritura local falla y el cursor ya subio, ese lote se \
             pierde para siempre.\n\n\
             Un abono puede llegar dos veces en el mismo lote, dentro de su prestamo \
             y en `loanPayments`. Todo se guarda por identificador, asi que guardarlo \
             dos veces deja lo mismo que guardarlo una.",
        ))
        .parameter(query_parameter(
            "since",
            "Cursor de la respuesta anterior. Ausente significa desde el principio.",
        ))
        .parameter(query_parameter(
            "limit",
            "Cambios por lote, contando las cinco colecciones juntas. 200 por \
             defecto, 1000 como tope.",
        ))
        .responses(responses)
        .build()
}
