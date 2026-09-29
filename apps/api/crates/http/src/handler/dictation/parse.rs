//! Handler de `POST /v1/dictation/parse`.

use std::sync::Arc;

use gastonomo_application::use_case::dictation::ParseDictation;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode};

use super::support::{json_body, write_json};
use crate::auth_context::current_user;
use crate::dto::dictation::{DictationDraftDto, ParseDictationRequestDto};
use crate::problem::render_problem;

/// Interpreta lo dictado y devuelve el borrador para revisar.
#[derive(Debug, Clone)]
pub struct ParseDictationHandler {
    use_case: Arc<ParseDictation>,
}

impl ParseDictationHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<ParseDictation>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ParseDictationHandler {
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
        let body = match json_body::<ParseDictationRequestDto>(req).await {
            Ok(body) => body,
            Err(error) => return render_problem(res, &error),
        };
        match self.use_case.execute(user_id, body.into_command()).await {
            Ok(draft) => write_json(res, StatusCode::OK, DictationDraftDto::from(&draft)),
            Err(error) => render_problem(res, &error),
        }
    }
}
