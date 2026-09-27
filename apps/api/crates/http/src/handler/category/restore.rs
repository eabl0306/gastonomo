//! Handler de `POST /v1/categories/{id}/restore`.

use std::sync::Arc;

use gastonomo_application::use_case::category::RestoreCategory;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{path_category_id, write_json};
use crate::auth_context::current_user;
use crate::dto::category::CategoryDto;
use crate::problem::render_problem;

/// Devuelve a la lista una categoria archivada.
///
/// Es un `POST` y no un `PATCH` porque no es un cambio de campos: es una accion
/// sobre la categoria, y el cuerpo esta vacio.
#[derive(Debug, Clone)]
pub struct RestoreCategoryHandler {
    use_case: Arc<RestoreCategory>,
}

impl RestoreCategoryHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<RestoreCategory>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for RestoreCategoryHandler {
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
            Ok(found) => {
                tracing::info!(%category_id, "category restored");
                write_json(res, StatusCode::OK, CategoryDto::from(&found));
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
