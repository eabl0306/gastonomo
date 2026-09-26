//! Ejecuta un futuro sin runtime asincrono.

use std::future::Future;
use std::pin::pin;
use std::task::{Context, Poll, Waker};

/// Ejecuta un futuro hasta completarlo sin depender de un runtime asincrono.
///
/// Los dobles de test terminan en el primer `poll`, asi que un bucle simple es
/// suficiente y mantiene el nucleo libre de Tokio.
///
/// # Panics
///
/// No entra en panico: si el futuro devuelve `Pending` cede el hilo y reintenta.
pub fn block_on<F: Future>(future: F) -> F::Output {
    let mut future = pin!(future);
    let mut context = Context::from_waker(Waker::noop());
    loop {
        match future.as_mut().poll(&mut context) {
            Poll::Ready(value) => return value,
            Poll::Pending => std::thread::yield_now(),
        }
    }
}
