//! Handlers del contexto de monedas y tasas.
//!
//! Los dos endpoints sirven datos globales, iguales para todas las cuentas: el
//! catalogo de monedas y las tasas con base USD. Por eso no leen el usuario del
//! `Depot`. Siguen colgados detras del middleware de access token, como el resto
//! de `/v1`, para que el cliente no tenga que tratar unos endpoints de una forma y
//! otros de otra.

use std::sync::Arc;

use gastonomo_application::use_case::currency::{GetExchangeRates, ListCurrencies};
use salvo::prelude::{Depot, FlowCtrl, Handler, Json, Request, Response, StatusCode};

use crate::dto::currency::{CurrencyListDto, ExchangeRatesDto};
use crate::problem::render_problem;

/// Responde a `GET /v1/currencies`.
#[derive(Debug, Clone)]
pub struct CurrencyListHandler {
    use_case: Arc<ListCurrencies>,
}

impl CurrencyListHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ListCurrencies>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for CurrencyListHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        match self.use_case.execute().await {
            Ok(currencies) => res.render_with_status(
                StatusCode::OK,
                Json(CurrencyListDto::from_catalog(&currencies)),
            ),
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Responde a `GET /v1/exchange-rates`.
#[derive(Debug, Clone)]
pub struct ExchangeRatesHandler {
    use_case: Arc<GetExchangeRates>,
}

impl ExchangeRatesHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<GetExchangeRates>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ExchangeRatesHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        match self.use_case.execute().await {
            Ok(view) => {
                res.render_with_status(StatusCode::OK, Json(ExchangeRatesDto::from(&view)));
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
