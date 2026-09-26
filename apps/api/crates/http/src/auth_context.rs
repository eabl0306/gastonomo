//! Autenticacion de las peticiones: extractor y ayuda para los handlers.
//!
//! El middleware [`RequireAccessToken`] valida la cabecera `Authorization: Bearer`
//! y deja el usuario en el `Depot`. Cualquier handler colgado detras lo lee con
//! [`current_user`]:
//!
//! ```ignore
//! let user_id = match current_user(depot) {
//!     Ok(user_id) => user_id,
//!     Err(error) => return render_problem(res, &error),
//! };
//! ```
//!
//! No hace falta volver a comprobar el token: si el handler se ejecuta, el token
//! era valido. Y no hace falta colgar el middleware: el router de `/v1` ya lo tiene
//! puesto para todo lo que no sea publico.

use std::sync::Arc;

use gastonomo_application::error::AppError;
use gastonomo_application::port::{Clock, TokenIssuer};
use gastonomo_domain::UserId;
use salvo::http::header::AUTHORIZATION;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response};

use crate::problem::render_problem;

/// Prefijo del esquema de autorizacion.
const BEARER: &str = "Bearer ";

/// Usuario del access token, tal y como queda en el `Depot`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct AuthenticatedUser(UserId);

impl AuthenticatedUser {
    /// Identificador de la cuenta autenticada.
    #[must_use]
    pub const fn id(self) -> UserId {
        self.0
    }
}

/// Lee el usuario autenticado que dejo el middleware.
///
/// # Errors
///
/// Devuelve [`AppError::Unauthorized`] si el handler se colgo fuera del router
/// protegido, que es un error de montaje y no del cliente.
pub fn current_user(depot: &Depot) -> Result<UserId, AppError> {
    depot
        .get_typed::<AuthenticatedUser>()
        .map(|user| user.id())
        .map_err(|_| AppError::unauthorized("this endpoint requires an access token"))
}

/// Valida el access token y deja el usuario en el `Depot`.
#[derive(Clone)]
pub struct RequireAccessToken {
    tokens: Arc<dyn TokenIssuer>,
    clock: Arc<dyn Clock>,
}

impl std::fmt::Debug for RequireAccessToken {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("RequireAccessToken").finish_non_exhaustive()
    }
}

impl RequireAccessToken {
    /// Crea el middleware.
    #[must_use]
    pub const fn new(tokens: Arc<dyn TokenIssuer>, clock: Arc<dyn Clock>) -> Self {
        Self { tokens, clock }
    }

    fn bearer(req: &Request) -> Result<&str, AppError> {
        let raw = req
            .headers()
            .get(AUTHORIZATION)
            .and_then(|value| value.to_str().ok())
            .ok_or_else(|| AppError::unauthorized("the Authorization header is missing"))?;
        raw.strip_prefix(BEARER)
            .map(str::trim)
            .filter(|token| !token.is_empty())
            .ok_or_else(|| {
                AppError::unauthorized("the Authorization header must be `Bearer <token>`")
            })
    }
}

#[salvo::async_trait]
impl Handler for RequireAccessToken {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        ctrl: &mut FlowCtrl,
    ) {
        let outcome = Self::bearer(req)
            .and_then(|token| self.tokens.verify(token, self.clock.now()))
            .map(AuthenticatedUser);
        match outcome {
            Ok(user) => {
                depot.insert_typed(user);
                ctrl.call_next(req, depot, res).await;
            }
            Err(error) => {
                render_problem(res, &error);
                ctrl.skip_rest();
            }
        }
    }
}
