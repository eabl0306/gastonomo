//! Documentacion OpenAPI del contexto de categorias.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, Parameter, ParameterBuilder, ParameterIn, PathItem,
    PathItemBuilder, PathsBuilder,
};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::schema::{ObjectBuilder, Type};
use utoipa::openapi::{ComponentsBuilder, Required};

use crate::dto::category::{
    CategoryDto, CategoryListDto, CreateCategoryRequestDto, UpdateCategoryRequestDto,
};

/// Etiqueta comun de los endpoints de categorias.
const TAG: &str = "categories";

/// Descripcion de una operacion de categorias.
struct Endpoint<'a> {
    id: &'a str,
    summary: &'a str,
    description: &'a str,
    request: Option<&'a str>,
    success: (&'a str, &'a str, Option<&'a str>),
    errors: &'a [(&'a str, &'a str)],
    with_path_id: bool,
}

/// Anade las rutas de `/v1/categories`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder
        .path("/v1/categories", collection())
        .path("/v1/categories/{id}", item())
}

/// Anade los esquemas de categorias.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<CategoryDto>()
        .schema_from::<CategoryListDto>()
        .schema_from::<CreateCategoryRequestDto>()
        .schema_from::<UpdateCategoryRequestDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn path_id() -> Parameter {
    ParameterBuilder::new()
        .name("id")
        .parameter_in(ParameterIn::Path)
        .required(Required::True)
        .description(Some("Identificador de la categoria."))
        .schema(Some(ObjectBuilder::new().schema_type(Type::String)))
        .build()
}

fn operation(endpoint: &Endpoint<'_>) -> Operation {
    let (status, description, schema) = endpoint.success;
    let success = schema.map_or_else(
        || ResponseBuilder::new().description(description),
        |name| {
            ResponseBuilder::new()
                .description(description)
                .json_schema_ref(name)
        },
    );
    let mut responses = ResponsesBuilder::new()
        .response(status, success)
        .response("401", problem("Falta el access token o no vale."))
        .response("500", problem("Fallo interno."));
    for (code, text) in endpoint.errors {
        responses = responses.response(*code, problem(text));
    }
    let mut builder = OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some(endpoint.id))
        .summary(Some(endpoint.summary))
        .description(Some(endpoint.description))
        .responses(responses.build());
    if endpoint.with_path_id {
        builder = builder.parameter(path_id());
    }
    if let Some(name) = endpoint.request {
        builder = builder.request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref(name),
        ));
    }
    builder.build()
}

fn collection() -> PathItem {
    PathItemBuilder::new()
        .operation(HttpMethod::Get, list())
        .operation(HttpMethod::Post, create())
        .build()
}

fn item() -> PathItem {
    PathItemBuilder::new()
        .operation(HttpMethod::Patch, update())
        .operation(HttpMethod::Delete, delete())
        .build()
}

fn list() -> Operation {
    operation(&Endpoint {
        id: "listCategories",
        summary: "Categorias de la cuenta",
        description: "Devuelve las 12 predeterminadas mas las del usuario, cada una con \
                      `movementCount`, que es lo que pinta \"Sin movimientos / 1 movimiento / \
                      N movimientos\". El orden que llega es estable (predeterminadas primero \
                      y luego por nombre) y el cliente lo reordena como pinta la rejilla.",
        request: None,
        success: ("200", "Categorias de la cuenta.", Some("CategoryListDto")),
        errors: &[],
        with_path_id: false,
    })
}

fn create() -> Operation {
    operation(&Endpoint {
        id: "createCategory",
        summary: "Nueva categoria",
        description: "El nombre es obligatorio, admite 22 caracteres y no puede repetirse con \
                      ninguna categoria de la cuenta, sin distinguir tipo ni mayusculas. El \
                      tipo es `in` u `out`: `both` esta reservado a las predeterminadas. El \
                      icono y el color tienen que ser de los que ofrece el sheet.",
        request: Some("CreateCategoryRequestDto"),
        success: ("201", "Categoria creada.", Some("CategoryDto")),
        errors: &[
            ("409", "El nombre ya existe: codigo `category_name_taken`."),
            ("422", "El nombre, el tipo, el icono o el color no valen."),
        ],
        with_path_id: false,
    })
}

fn update() -> Operation {
    operation(&Endpoint {
        id: "updateCategory",
        summary: "Editar una categoria",
        description: "Cambia el nombre, el tipo, el icono o el color. Lo que no viene se queda \
                      como estaba. Renombrar no toca los movimientos, porque apuntan al \
                      identificador. Una predeterminada responde 409 y una categoria de otra \
                      cuenta, 404.",
        request: Some("UpdateCategoryRequestDto"),
        success: ("200", "Categoria editada.", Some("CategoryDto")),
        errors: &[
            ("404", "No existe o es de otra cuenta."),
            (
                "409",
                "Es predeterminada (`category_is_default`) o el nombre ya existe \
                 (`category_name_taken`).",
            ),
            ("422", "Algun campo del cuerpo no vale."),
        ],
        with_path_id: true,
    })
}

fn delete() -> Operation {
    operation(&Endpoint {
        id: "deleteCategory",
        summary: "Borrar una categoria",
        description: "Pasa sus movimientos a \"Otros\" y despues borra la fila, todo en una \
                      transaccion. Las predeterminadas responden 409.",
        request: None,
        success: ("204", "Categoria borrada.", None),
        errors: &[
            ("404", "No existe o es de otra cuenta."),
            ("409", "Es predeterminada: codigo `category_is_default`."),
        ],
        with_path_id: true,
    })
}
