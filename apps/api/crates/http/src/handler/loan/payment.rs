//! Handler de `POST /v1/loans/{id}/payments`.

use std::sync::Arc;

use gastonomo_application::use_case::loan::{RegisterLoanPayment, RegisterLoanPaymentCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, loan_id, write_json};
use crate::auth_context::current_user;
use crate::dto::loan::{CreateLoanPaymentRequestDto, LoanDto};
use crate::problem::render_problem;

/// Registra el abono y, en la misma transaccion, su movimiento.
///
/// Responde con el prestamo entero, ya con el abono dentro: el cliente necesita el
/// saldo nuevo para repintar la tarjeta sin volver a pedir la lista.
#[derive(Debug, Clone)]
pub struct RegisterLoanPaymentHandler {
    use_case: Arc<RegisterLoanPayment>,
}

impl RegisterLoanPaymentHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<RegisterLoanPayment>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for RegisterLoanPaymentHandler {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        let id = match loan_id(req) {
            Ok(id) => id,
            Err(error) => return render_problem(res, &error),
        };
        let body = match json_body::<CreateLoanPaymentRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = RegisterLoanPaymentCommand {
            paid_on: body.paid_on,
            label: body.label,
            amount: body.amount,
            currency: body.currency,
            base: body.base,
            base_currency: body.base_currency,
        };
        match self.use_case.execute(user_id, &id, command).await {
            Ok(loan) => write_json(res, StatusCode::CREATED, LoanDto::from(&loan)),
            Err(error) => render_problem(res, &error),
        }
    }
}
