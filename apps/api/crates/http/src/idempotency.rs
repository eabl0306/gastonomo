//! Idempotencia de los tres POST que crean dinero.
//!
//! Envuelve un handler y, cuando la peticion trae la cabecera `Idempotency-Key`,
//! garantiza que el mismo intento no se guarda dos veces. Sin cabecera el handler se
//! comporta exactamente como antes: la proteccion es opcional para que ningun
//! cliente existente se rompa, y el movil la manda siempre.
//!
//! Va aqui, en el adaptador driving, y no en el caso de uso: es una propiedad del
//! transporte (repetir una respuesta ya emitida) y no una regla de negocio. El
//! almacen es un puerto de `application`, asi que esto no sabe que hay PostgreSQL
//! detras.
//!
//! El reparto de responsabilidades con el almacen esta en
//! [`gastonomo_application::port::IdempotencyStore`]. Aqui solo queda el protocolo:
//! leer la cabecera, calcular la huella del cuerpo y repetir la respuesta guardada.

mod fingerprint;
mod replay;

use std::fmt;
use std::sync::Arc;

use gastonomo_application::error::AppError;
use gastonomo_application::port::{
    IdempotencyStore, IdempotentRequest, MAX_IDEMPOTENCY_KEY_LENGTH, Reservation,
};
use gastonomo_domain::UserId;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response};

use crate::auth_context::current_user;
use crate::problem::render_problem;

pub use fingerprint::fingerprint_of;
pub use replay::REPLAY_HEADER;

/// Cabecera con la que el cliente identifica un intento del usuario.
pub const IDEMPOTENCY_HEADER: &str = "idempotency-key";

/// Hace idempotente a otro handler.
///
/// Se monta en el router alrededor del handler de creacion, nunca alrededor de una
/// lectura: un `GET` ya es idempotente por definicion.
#[derive(Clone)]
pub struct Idempotent {
    store: Arc<dyn IdempotencyStore>,
    endpoint: String,
    inner: Arc<dyn Handler>,
}

impl fmt::Debug for Idempotent {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Idempotent")
            .field("endpoint", &self.endpoint)
            .finish_non_exhaustive()
    }
}

impl Idempotent {
    /// Envuelve un handler bajo el nombre de operacion dado.
    ///
    /// `endpoint` entra en la clave guardada: la misma `Idempotency-Key` en dos
    /// operaciones distintas son dos reservas distintas, asi que un cliente que
    /// reutilice el valor entre pantallas no se bloquea a si mismo.
    #[must_use]
    pub fn new(
        store: Arc<dyn IdempotencyStore>,
        endpoint: impl Into<String>,
        inner: impl Handler,
    ) -> Self {
        Self {
            store,
            endpoint: endpoint.into(),
            inner: Arc::new(inner),
        }
    }

    async fn guarded(
        &self,
        key: &str,
        user_id: UserId,
        parts: &mut Parts<'_>,
    ) -> Result<(), AppError> {
        let fingerprint = fingerprint_of(parts.req).await?;
        let request = IdempotentRequest {
            user_id,
            endpoint: &self.endpoint,
            key,
            fingerprint: &fingerprint,
        };
        match self.store.reserve(request).await? {
            Reservation::Fresh => {
                self.run_and_record(request, parts).await;
                Ok(())
            }
            Reservation::Replay(response) => {
                replay::write(parts.res, &response);
                Ok(())
            }
            Reservation::InProgress => Err(in_progress()),
            Reservation::Mismatch => Err(mismatch()),
        }
    }

    /// Ejecuta el handler envuelto y guarda su respuesta si creo algo.
    async fn run_and_record(&self, request: IdempotentRequest<'_>, parts: &mut Parts<'_>) {
        self.inner
            .handle(parts.req, parts.depot, parts.res, parts.ctrl)
            .await;
        let recorded = match replay::captured(parts.res) {
            Some(response) => self.store.complete(request, &response).await,
            // Un rechazo no se guarda: el cuerpo es determinista y guardarlo
            // obligaria al cliente a inventarse otra clave para corregir un dato.
            None => self.store.release(request).await,
        };
        if let Err(error) = recorded {
            tracing::error!(%error, "could not record the idempotent response");
        }
    }
}

/// Las cuatro piezas de una peticion en curso, juntas para no pasar cinco
/// parametros.
struct Parts<'a> {
    req: &'a mut Request,
    depot: &'a mut Depot,
    res: &'a mut Response,
    ctrl: &'a mut FlowCtrl,
}

#[salvo::async_trait]
impl Handler for Idempotent {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        ctrl: &mut FlowCtrl,
    ) {
        let key = match read_key(req) {
            Ok(key) => key,
            Err(error) => return render_problem(res, &error),
        };
        let Some(key) = key else {
            return self.inner.handle(req, depot, res, ctrl).await;
        };
        let user_id = match current_user(depot) {
            Ok(user_id) => user_id,
            Err(error) => return render_problem(res, &error),
        };
        let mut parts = Parts {
            req,
            depot,
            res,
            ctrl,
        };
        if let Err(error) = self.guarded(&key, user_id, &mut parts).await {
            render_problem(parts.res, &error);
        }
    }
}

/// Lee y valida la cabecera. `None` significa que no viene, que es legitimo.
fn read_key(req: &Request) -> Result<Option<String>, AppError> {
    let Some(raw) = req.headers().get(IDEMPOTENCY_HEADER) else {
        return Ok(None);
    };
    let value = raw.to_str().map_err(|_| bad_key())?.trim().to_owned();
    if value.is_empty() || value.chars().count() > MAX_IDEMPOTENCY_KEY_LENGTH {
        return Err(bad_key());
    }
    Ok(Some(value))
}

fn bad_key() -> AppError {
    AppError::validation(
        "Idempotency-Key",
        format!(
            "La clave de idempotencia tiene que ser un texto de 1 a {MAX_IDEMPOTENCY_KEY_LENGTH} caracteres."
        ),
    )
}

fn in_progress() -> AppError {
    AppError::conflict(
        "idempotency_key_in_progress",
        "Estamos guardando este registro. Espera un momento antes de reintentar.",
    )
}

fn mismatch() -> AppError {
    AppError::conflict(
        "idempotency_key_reused",
        "Esa clave ya se uso con otros datos. Usa una clave nueva para un registro nuevo.",
    )
}
