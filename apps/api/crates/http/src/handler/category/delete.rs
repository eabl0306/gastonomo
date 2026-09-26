//! Handler de `DELETE /v1/categories/{id}`.

use std::sync::Arc;

use gastonomo_application::use_case::category::DeleteCategory;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{path_category_id, write_empty};
use crate::auth_context::current_user;
use crate::problem::render_problem;

/// Borra una categoria del usuario y pasa sus movimientos a "Otros".
#[derive(Debug, Clone)]
pub struct DeleteCategoryHandler {
    use_case: Arc<DeleteCategory>,
}

impl DeleteCategoryHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<DeleteCategory>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for DeleteCategoryHandler {
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
        let category_id = match path_category_id(req) {
            Ok(category_id) => category_id,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id, category_id).await {
            Ok(reassigned) => {
                tracing::info!(%category_id, reassigned, "category deleted");
                write_empty(res, StatusCode::NO_CONTENT);
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
