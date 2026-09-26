//! Handler de `GET /v1/categories`.

use std::sync::Arc;

use gastonomo_application::use_case::category::ListCategories;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::write_json;
use crate::auth_context::current_user;
use crate::dto::category::CategoryListDto;
use crate::problem::render_problem;

/// Devuelve las categorias de la cuenta con su conteo de uso.
#[derive(Debug, Clone)]
pub struct ListCategoriesHandler {
    use_case: Arc<ListCategories>,
}

impl ListCategoriesHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ListCategories>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ListCategoriesHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id).await {
            Ok(found) => write_json(res, StatusCode::OK, CategoryListDto::new(&found)),
            Err(error) => render_problem(res, &error),
        }
    }
}
