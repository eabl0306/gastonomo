//! Documentacion OpenAPI del contexto de prestamos.

use utoipa::openapi::path::{
    HttpMethod, Operation, OperationBuilder, Parameter, ParameterBuilder, ParameterIn, PathItem,
    PathsBuilder,
};
use utoipa::openapi::request_body::{RequestBodyBuilder, RequestBodyExt as _};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};
use utoipa::openapi::schema::{ObjectBuilder, Type};
use utoipa::openapi::{ComponentsBuilder, Required};

use super::idempotency_docs;
use crate::dto::loan::{
    CreateLoanPaymentRequestDto, CreateLoanRequestDto, LoanDto, LoanListDto, LoanPaymentDto,
};

/// Etiqueta comun de los endpoints de prestamos.
const TAG: &str = "loans";

/// Anade las rutas de `/v1/loans`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder
        .path("/v1/loans", collection())
        .path("/v1/loans/{id}", detail())
        .path("/v1/loans/{id}/payments", payments())
}

/// Anade los esquemas de prestamos.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<LoanDto>()
        .schema_from::<LoanListDto>()
        .schema_from::<LoanPaymentDto>()
        .schema_from::<CreateLoanRequestDto>()
        .schema_from::<CreateLoanPaymentRequestDto>()
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

fn text_parameter(name: &str, place: ParameterIn, description: &str) -> Parameter {
    let required = if place == ParameterIn::Path {
        Required::True
    } else {
        Required::False
    };
    ParameterBuilder::new()
        .name(name)
        .parameter_in(place)
        .required(required)
        .description(Some(description))
        .schema(Some(ObjectBuilder::new().schema_type(Type::String)))
        .build()
}

fn collection() -> PathItem {
    let mut item = PathItem::new(HttpMethod::Get, list_operation());
    item.post = Some(create_operation());
    item
}

fn list_operation() -> Operation {
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("listLoans"))
        .summary(Some("Prestamos con sus abonos"))
        .description(Some(
            "Devuelve los prestamos de la cuenta con los abonos embebidos. El saldo, \
             el estado (Pagado, Parcial, Pendiente) y el orden (abiertos primero) los \
             deriva el cliente, porque dependen de la moneda principal del momento y \
             de las tasas vigentes.",
        ))
        .parameter(text_parameter(
            "direction",
            ParameterIn::Query,
            "`lent`, `borrowed` o se omite, que es la pill Todos.",
        ))
        .responses(
            ResponsesBuilder::new()
                .response(
                    "200",
                    ResponseBuilder::new()
                        .description("Prestamos de la cuenta.")
                        .json_schema_ref("LoanListDto"),
                )
                .response("401", problem("Falta el access token o no vale."))
                .response("422", problem("El filtro de direccion no es valido."))
                .build(),
        )
        .build()
}

fn create_operation() -> Operation {
    OperationBuilder::new()
        .tag(TAG)
        .operation_id(Some("createLoan"))
        .summary(Some("Nuevo prestamo"))
        .description(Some(
            "Crea el prestamo y, en la misma transaccion, el movimiento que el diseno \
             exige: categoria Prestamo, gasto si preste e ingreso si me prestaron, con \
             titulo \"Prestamo · {persona}\". Si el movimiento falla no queda el \
             prestamo.",
        ))
        .parameter(idempotency_docs::parameter())
        .request_body(Some(
            RequestBodyBuilder::new()
                .required(Some(Required::True))
                .build()
                .json_schema_ref("CreateLoanRequestDto"),
        ))
        .responses(
            ResponsesBuilder::new()
                .response(
                    "201",
                    ResponseBuilder::new()
                        .description("Prestamo creado.")
                        .json_schema_ref("LoanDto"),
                )
                .response("401", problem("Falta el access token o no vale."))
                .response("409", problem(idempotency_docs::CONFLICT))
                .response("422", problem("El formulario no cumple las reglas."))
                .response("500", problem("La transaccion no se pudo completar."))
                .build(),
        )
        .build()
}

fn detail() -> PathItem {
    PathItem::new(
        HttpMethod::Get,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("getLoan"))
            .summary(Some("Detalle del prestamo"))
            .description(Some(
                "Devuelve un prestamo con su historial de abonos. Un prestamo de otra \
                 cuenta responde 404 y no 403: un 403 confirmaria que existe.",
            ))
            .parameter(text_parameter(
                "id",
                ParameterIn::Path,
                "Identificador del prestamo.",
            ))
            .responses(
                ResponsesBuilder::new()
                    .response(
                        "200",
                        ResponseBuilder::new()
                            .description("Prestamo.")
                            .json_schema_ref("LoanDto"),
                    )
                    .response("401", problem("Falta el access token o no vale."))
                    .response("404", problem("No existe o es de otra cuenta."))
                    .build(),
            )
            .build(),
    )
}

fn payments() -> PathItem {
    PathItem::new(
        HttpMethod::Post,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("registerLoanPayment"))
            .summary(Some("Registrar abono"))
            .description(Some(
                "Anade un abono al prestamo y, en la misma transaccion, su movimiento: \
                 categoria Abono prestamo, ingreso si preste y gasto si me prestaron, \
                 con titulo \"Abono · {persona}\". Un abono puede superar el saldo, \
                 porque el diseno ensena el exceso como intereses ganados. Responde el \
                 prestamo completo con el abono dentro.",
            ))
            .parameter(text_parameter(
                "id",
                ParameterIn::Path,
                "Identificador del prestamo.",
            ))
            .parameter(idempotency_docs::parameter())
            .request_body(Some(
                RequestBodyBuilder::new()
                    .required(Some(Required::True))
                    .build()
                    .json_schema_ref("CreateLoanPaymentRequestDto"),
            ))
            .responses(
                ResponsesBuilder::new()
                    .response(
                        "201",
                        ResponseBuilder::new()
                            .description("Abono registrado.")
                            .json_schema_ref("LoanDto"),
                    )
                    .response("401", problem("Falta el access token o no vale."))
                    .response("404", problem("No existe o es de otra cuenta."))
                    .response("409", problem(idempotency_docs::CONFLICT))
                    .response("422", problem("El sheet no cumple las reglas."))
                    .response("500", problem("La transaccion no se pudo completar."))
                    .build(),
            )
            .build(),
    )
}
