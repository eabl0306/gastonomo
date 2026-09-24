//! Middlewares compartidos por todas las rutas.

mod request_id;

pub use request_id::{REQUEST_ID_HEADER, RequestId};
