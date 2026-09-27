//! Handler de `DELETE /v1/categories/{id}`.

use std::sync::Arc;

use gastonomo_application::use_case::category::{ArchiveCategory, CategoryRemoval};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{path_category_id, write_json};
use crate::auth_context::current_user;
use crate::dto::category::CategoryRemovalDto;
use crate::problem::render_problem;

/// Quita de en medio una categoria del usuario.
///
/// Se mantiene el verbo `DELETE` porque desde fuera lo que se pide es "quitala de
/// en medio". Lo que cambia es que **ya no destruye nada**: si algun movimiento la
/// usa, se archiva y se queda donde esta. Solo se borra de verdad cuando no la usa
/// ninguno.
///
/// Responde **200 con `{"outcome": ...}`** y no 204 porque el cliente tiene que
/// decirle al usuario cual de las dos cosas paso.
#[derive(Debug, Clone)]
pub struct DeleteCategoryHandler {
    use_case: Arc<ArchiveCategory>,
}

impl DeleteCategoryHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ArchiveCategory>) -> Self {
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
            Ok(removal) => {
                let archived = removal == CategoryRemoval::Archived;
                tracing::info!(%category_id, archived, "category removed from the list");
                write_json(res, StatusCode::OK, CategoryRemovalDto::from(removal));
            }
            Err(error) => render_problem(res, &error),
        }
    }
}
