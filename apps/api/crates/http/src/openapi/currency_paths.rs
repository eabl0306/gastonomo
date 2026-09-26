//! Documentacion OpenAPI del contexto de monedas y tasas.

use utoipa::openapi::ComponentsBuilder;
use utoipa::openapi::path::{HttpMethod, OperationBuilder, PathItem, PathsBuilder};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};

use crate::dto::currency::{CurrencyDto, CurrencyListDto, ExchangeRatesDto};

/// Etiqueta comun de los endpoints de monedas y tasas.
const TAG: &str = "currencies";

/// Anade las rutas de monedas y tasas.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder
        .path("/v1/currencies", currencies())
        .path("/v1/exchange-rates", exchange_rates())
}

/// Anade los esquemas de monedas y tasas.
#[must_use]
pub fn schemas(builder: ComponentsBuilder) -> ComponentsBuilder {
    builder
        .schema_from::<CurrencyDto>()
        .schema_from::<CurrencyListDto>()
        .schema_from::<ExchangeRatesDto>()
}

fn currencies() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response(
            "200",
            ResponseBuilder::new()
                .description("Catalogo de monedas que se pueden elegir.")
                .json_schema_ref("CurrencyListDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .build();
    PathItem::new(
        HttpMethod::Get,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("listCurrencies"))
            .summary(Some("Catalogo de monedas"))
            .description(Some(
                "Devuelve solo las monedas que se pueden elegir: sin los metales (XAU, \
                 XAG, XPT, XPD) ni los XDR, y sin las que no tienen tasa. El filtro lo \
                 hace el servidor una vez, asi que el cliente pinta la lista tal cual.",
            ))
            .responses(responses)
            .build(),
    )
}

fn exchange_rates() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response(
            "200",
            ResponseBuilder::new()
                .description("Tasas vigentes con base USD.")
                .json_schema_ref("ExchangeRatesDto"),
        )
        .response("401", problem("Falta el access token o no vale."))
        .response(
            "503",
            problem(
                "Todavia no hay ninguna pasada guardada de la fuente: no es un fallo, \
                 la tarea de refresco no ha terminado su primera pasada.",
            ),
        )
        .build();
    PathItem::new(
        HttpMethod::Get,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("getExchangeRates"))
            .summary(Some("Tasas de cambio"))
            .description(Some(
                "Las tasas las guarda una tarea de la API que consulta Frankfurter dos \
                 veces al dia: los clientes no llaman nunca a Frankfurter. Las tasas \
                 viajan como string para que ningun cliente las degrade a double. Con \
                 `status` en `stale` los datos tienen mas de 48 horas y se sirven \
                 igualmente.",
            ))
            .responses(responses)
            .build(),
    )
}

fn problem(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("ProblemDto")
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use utoipa::openapi::ComponentsBuilder;
    use utoipa::openapi::path::PathsBuilder;

    use super::{paths, schemas};

    fn documented_paths() -> serde_json::Value {
        let built = paths(PathsBuilder::new()).build();
        serde_json::to_value(built).expect("the paths should serialize")
    }

    fn documented_schemas() -> serde_json::Value {
        let built = schemas(ComponentsBuilder::new()).build();
        serde_json::to_value(built).expect("the components should serialize")
    }

    #[test]
    fn both_endpoints_are_documented() {
        let paths = documented_paths();
        assert_eq!(
            paths["/v1/currencies"]["get"]["operationId"],
            "listCurrencies"
        );
        assert_eq!(
            paths["/v1/exchange-rates"]["get"]["operationId"],
            "getExchangeRates"
        );
    }

    #[test]
    fn the_rates_document_the_empty_database_case() {
        let paths = documented_paths();
        assert!(paths["/v1/exchange-rates"]["get"]["responses"]["503"].is_object());
        assert!(paths["/v1/currencies"]["get"]["responses"]["401"].is_object());
    }

    #[test]
    fn the_response_schemas_are_exposed() {
        let schemas = documented_schemas();
        for name in ["CurrencyDto", "CurrencyListDto", "ExchangeRatesDto"] {
            assert!(schemas["schemas"][name].is_object(), "{name}");
        }
    }

    #[test]
    fn the_rates_are_documented_as_strings() {
        let schemas = documented_schemas();
        let rates = &schemas["schemas"]["ExchangeRatesDto"]["properties"]["rates"];
        assert_eq!(rates["additionalProperties"]["type"], "string");
    }
}
