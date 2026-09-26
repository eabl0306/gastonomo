//! Handler de `PATCH /v1/categories/{id}`.

use std::sync::Arc;

use gastonomo_application::use_case::category::{UpdateCategory, UpdateCategoryCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, path_category_id, write_json};
use crate::auth_context::current_user;
use crate::dto::category::{CategoryDto, UpdateCategoryRequestDto};
use crate::problem::render_problem;

/// Edita una categoria del usuario.
#[derive(Debug, Clone)]
pub struct UpdateCategoryHandler {
    use_case: Arc<UpdateCategory>,
}

impl UpdateCategoryHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<UpdateCategory>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for UpdateCategoryHandler {
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
        let body = match json_body::<UpdateCategoryRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = UpdateCategoryCommand {
            user_id,
            category_id,
            name: body.name,
            kind: body.kind,
            icon: body.icon,
            color: body.color,
        };
        match self.use_case.execute(command).await {
            Ok(updated) => write_json(res, StatusCode::OK, CategoryDto::from(&updated)),
            Err(error) => render_problem(res, &error),
        }
    }
}
