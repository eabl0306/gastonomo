//! Handler de `GET /v1/categories`.

use std::sync::Arc;

use gastonomo_application::use_case::category::ListCategories;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::write_json;
use crate::auth_context::current_user;
use crate::dto::category::CategoryListDto;
use crate::problem::render_problem;

/// Devuelve las categorias de la cuenta con su conteo de uso.
///
/// Por defecto solo las activas. Con `?includeArchived=true` tambien las
/// archivadas, que es lo que hace falta para resolver la categoria de un movimiento
/// viejo o para recuperarla.
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
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        // Cualquier valor que no sea exactamente "true" cuenta como false: un
        // parametro mal escrito no puede acabar ensenando archivadas sin querer.
        let include_archived = req
            .query::<String>("includeArchived")
            .is_some_and(|value| value == "true");
        match self.use_case.execute(user_id, include_archived).await {
            Ok(found) => write_json(res, StatusCode::OK, CategoryListDto::new(&found)),
            Err(error) => render_problem(res, &error),
        }
    }
}
