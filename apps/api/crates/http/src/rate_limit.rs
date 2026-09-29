//! Limite de peticiones por cliente y ruta.
//!
//! Se cuelga en los cuatro endpoints publicos que tocan credenciales: `register` y
//! `login`, que prueban contrasenas contra un hash de Argon2; `password/forgot`,
//! que manda correo; y `password/reset`, que cambia una contrasena y revoca todas
//! las sesiones sin pedir token de acceso.
//!
//! `refresh` y `logout` se quedan fuera a proposito: los dos exigen un refresh
//! token valido en el cuerpo, asi que no son una puerta abierta, y limitarlos
//! cortaria a un cliente legitimo que renueva a menudo.
//!
//! Que quede claro lo que **no** es: no es una defensa contra adivinar un token.
//! Los tokens opacos son 256 bits de CSPRNG y no se adivinan ni con limite ni sin
//! el. Lo que acota es el ruido, la carga y el correo.
//!
//! Es una ventana fija en memoria del proceso. Con varias replicas cada una lleva
//! su cuenta, asi que el limite real es el configurado por replica. Para algo mas
//! fino hace falta un contador compartido, y eso ya es otra pieza de
//! infraestructura.

use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use gastonomo_application::error::AppError;
use salvo::http::header::HeaderName;
use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response};

use crate::problem::render_problem;

/// Numero de claves a partir del cual se limpian las ventanas caducadas.
const PRUNE_THRESHOLD: usize = 1_024;

/// Cabecera que pone el gateway con la IP original del cliente.
const FORWARDED_FOR: &str = "x-forwarded-for";

/// Cliente al que se le cuentan las peticiones.
///
/// Es la IP, nunca el `SocketAddr` completo: el puerto de origen cambia en cada
/// conexion y con el dentro no se limitaria nada.
///
/// Detras del gateway todas las peticiones llegan con la IP del gateway, asi que
/// hace falta `X-Forwarded-For` para distinguir clientes. De esa cabecera se lee
/// **la ultima** entrada, no la primera, y esa es la parte que importa: nginx la
/// compone con `$proxy_add_x_forwarded_for`, que anade la IP real del par al final
/// de lo que hubiera mandado el cliente. La primera entrada la escribe quien llama
/// y vale cualquier cosa; la ultima la pone el proxy. Leyendo la primera, cualquiera
/// se saltaba el limite cambiando la cabecera en cada intento.
///
/// Sigue siendo una cabecera que solo vale si la pone un proxy de confianza: si la
/// API se expone directamente a internet, el borde tiene que sobreescribir o
/// eliminar `X-Forwarded-For` antes de dejar pasar la peticion.
fn client(req: &Request) -> String {
    forwarded_for(req).unwrap_or_else(|| {
        req.remote_addr()
            .ip()
            .map_or_else(|| "unknown".to_owned(), |ip| ip.to_string())
    })
}

fn forwarded_for(req: &Request) -> Option<String> {
    let name = HeaderName::from_static(FORWARDED_FOR);
    let raw = req.headers().get(name)?.to_str().ok()?;
    let last = raw.split(',').map(str::trim).rfind(|hop| !hop.is_empty())?;
    Some(last.to_owned())
}

/// Ventana de conteo de un cliente.
#[derive(Debug, Clone, Copy)]
struct Window {
    started_at: Instant,
    hits: u32,
}

/// Limita las peticiones de un mismo cliente a una misma ruta.
#[derive(Debug, Clone)]
pub struct RateLimit {
    max_requests: u32,
    window: Duration,
    windows: Arc<Mutex<HashMap<String, Window>>>,
}

impl RateLimit {
    /// Crea el middleware.
    #[must_use]
    pub fn new(max_requests: u32, window_seconds: u64) -> Self {
        Self {
            max_requests,
            window: Duration::from_secs(window_seconds.max(1)),
            windows: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    fn key(req: &Request) -> String {
        format!("{}|{}", client(req), req.uri().path())
    }

    /// Cuenta una peticion y dice si se pasa del limite.
    fn exceeded(&self, key: String) -> bool {
        let now = Instant::now();
        let Ok(mut windows) = self.windows.lock() else {
            // Con el mutex envenenado se deja pasar: un limite de peticiones no es
            // motivo para tumbar el servicio.
            return false;
        };
        if windows.len() > PRUNE_THRESHOLD {
            windows.retain(|_, window| now.duration_since(window.started_at) < self.window);
        }
        let window = windows.entry(key).or_insert(Window {
            started_at: now,
            hits: 0,
        });
        if now.duration_since(window.started_at) >= self.window {
            window.started_at = now;
            window.hits = 0;
        }
        window.hits += 1;
        window.hits > self.max_requests
    }
}

#[salvo::async_trait]
impl Handler for RateLimit {
    async fn handle(
        &self,
        req: &mut Request,
        depot: &mut Depot,
        res: &mut Response,
        ctrl: &mut FlowCtrl,
    ) {
        if self.exceeded(Self::key(req)) {
            tracing::warn!(path = %req.uri().path(), "rate limit exceeded");
            render_problem(res, &AppError::RateLimited);
            ctrl.skip_rest();
            return;
        }
        ctrl.call_next(req, depot, res).await;
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use salvo::prelude::Request;

    use super::{RateLimit, client};

    fn request_with_forwarded_for(value: &str) -> Request {
        let mut request = Request::default();
        let header = value.parse().expect("the test value is a valid header");
        request
            .headers_mut()
            .insert(super::HeaderName::from_static(super::FORWARDED_FOR), header);
        request
    }

    #[test]
    fn the_client_is_the_last_hop_of_forwarded_for() {
        // Es la que anade el proxy con `$proxy_add_x_forwarded_for`.
        assert_eq!(
            client(&request_with_forwarded_for("203.0.113.9, 10.0.0.7")),
            "10.0.0.7"
        );
    }

    #[test]
    fn spoofing_the_first_hop_does_not_change_the_bucket() {
        // El intento de saltarse el limite: cambiar la cabecera en cada peticion
        // para que cada una caiga en una ventana distinta.
        let limiter = RateLimit::new(2, 60);
        let spoofed = [
            "1.1.1.1, 10.0.0.7",
            "2.2.2.2, 10.0.0.7",
            "3.3.3.3, 10.0.0.7",
        ];
        let mut outcomes = spoofed
            .iter()
            .map(|value| limiter.exceeded(client(&request_with_forwarded_for(value))));
        assert_eq!(outcomes.next(), Some(false));
        assert_eq!(outcomes.next(), Some(false));
        assert_eq!(outcomes.next(), Some(true));
    }

    #[test]
    fn the_first_requests_pass_and_the_rest_do_not() {
        let limiter = RateLimit::new(2, 60);
        assert!(!limiter.exceeded("a".to_owned()));
        assert!(!limiter.exceeded("a".to_owned()));
        assert!(limiter.exceeded("a".to_owned()));
    }

    #[test]
    fn each_client_and_route_has_its_own_window() {
        let limiter = RateLimit::new(1, 60);
        assert!(!limiter.exceeded("a".to_owned()));
        assert!(!limiter.exceeded("b".to_owned()));
        assert!(limiter.exceeded("a".to_owned()));
    }
}
