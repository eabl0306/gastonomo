//! Handler de `POST /v1/loans`.

use std::sync::Arc;

use gastonomo_application::use_case::loan::{CreateLoan, CreateLoanCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, write_json};
use crate::auth_context::current_user;
use crate::dto::loan::{CreateLoanRequestDto, LoanDto};
use crate::problem::render_problem;

/// Crea el prestamo y, en la misma transaccion, su movimiento.
#[derive(Debug, Clone)]
pub struct CreateLoanHandler {
    use_case: Arc<CreateLoan>,
}

impl CreateLoanHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<CreateLoan>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for CreateLoanHandler {
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
        let body = match json_body::<CreateLoanRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = CreateLoanCommand {
            person: body.person,
            note: body.note,
            direction: body.direction,
            opened_on: body.opened_on,
            amount: body.amount,
            currency: body.currency,
            base: body.base,
            base_currency: body.base_currency,
        };
        match self.use_case.execute(user_id, command).await {
            Ok(loan) => write_json(res, StatusCode::CREATED, LoanDto::from(&loan)),
            Err(error) => render_problem(res, &error),
        }
    }
}
