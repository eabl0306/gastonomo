//! Handlers de `GET /v1/loans` y `GET /v1/loans/{id}`.

use std::sync::Arc;

use gastonomo_application::use_case::loan::{GetLoan, ListLoans, ListLoansQuery};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{loan_id, write_json};
use crate::auth_context::current_user;
use crate::dto::loan::{LoanDto, LoanListDto};
use crate::problem::render_problem;

/// Lista los prestamos de la cuenta, con el filtro de las pills.
#[derive(Debug, Clone)]
pub struct ListLoansHandler {
    use_case: Arc<ListLoans>,
}

impl ListLoansHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ListLoans>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ListLoansHandler {
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
        let query = ListLoansQuery {
            direction: req.query::<String>("direction"),
        };
        match self.use_case.execute(user_id, &query).await {
            Ok(loans) => write_json(res, StatusCode::OK, LoanListDto::new(&loans)),
            Err(error) => render_problem(res, &error),
        }
    }
}

/// Devuelve un prestamo con sus abonos.
#[derive(Debug, Clone)]
pub struct LoanDetailHandler {
    use_case: Arc<GetLoan>,
}

impl LoanDetailHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<GetLoan>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for LoanDetailHandler {
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
        match self.use_case.execute(user_id, &id).await {
            Ok(loan) => write_json(res, StatusCode::OK, LoanDto::from(&loan)),
            Err(error) => render_problem(res, &error),
        }
    }
}
