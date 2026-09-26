//! Respuestas de error que no nacen de un caso de uso.
//!
//! Los errores de negocio los escribe [`crate::problem::render_problem`], pero hay
//! dos que nunca llegan a un handler: la ruta que no existe y el metodo que no esta
//! permitido. Salvo los resuelve con su pagina por defecto, que negocia el `Accept`
//! y en la practica sale en HTML. Un cliente de esta API siempre espera JSON, asi
//! que aqui se sustituye esa pagina por el mismo `application/problem+json` que
//! usa el resto del contrato.
//!
//! Se engancha en [`crate::build_service`]. Solo se ejecuta cuando la respuesta ya
//! tiene un estado de error y el cuerpo esta vacio, asi que no puede pisar lo que
//! haya escrito un handler.

use salvo::catcher::Catcher;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response};

use crate::problem::{problem_for_status, write_problem};

/// Convierte en `problem+json` los errores que genera el propio transporte.
#[derive(Debug, Clone, Copy, Default)]
pub struct ProblemCatcher;

impl ProblemCatcher {
    /// Crea el catcher.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }

    /// Catcher listo para pasarle a `Service::catcher`.
    #[must_use]
    pub fn catcher() -> Catcher {
        Catcher::default().hoop(Self::new())
    }
}

#[salvo::async_trait]
impl Handler for ProblemCatcher {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        ctrl: &mut FlowCtrl,
    ) {
        let (status, problem) = problem_for_status(res.status_code);
        write_problem(res, status, &problem);
        // Nadie mas tiene que tocar esta respuesta: el catcher por defecto de
        // Salvo escribiria su pagina HTML encima.
        ctrl.skip_rest();
    }
}
