//! Handler de `POST /v1/categories`.

use std::sync::Arc;

use gastonomo_application::use_case::category::{CreateCategory, CreateCategoryCommand};
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, write_json};
use crate::auth_context::current_user;
use crate::dto::category::{CategoryDto, CreateCategoryRequestDto};
use crate::problem::render_problem;

/// Crea una categoria del usuario.
#[derive(Debug, Clone)]
pub struct CreateCategoryHandler {
    use_case: Arc<CreateCategory>,
}

impl CreateCategoryHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<CreateCategory>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for CreateCategoryHandler {
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
        let body = match json_body::<CreateCategoryRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        let command = CreateCategoryCommand {
            user_id,
            name: body.name,
            kind: body.kind,
            icon: body.icon,
            color: body.color,
        };
        match self.use_case.execute(command).await {
            Ok(created) => write_json(res, StatusCode::CREATED, CategoryDto::from(&created)),
            Err(error) => render_problem(res, &error),
        }
    }
}
