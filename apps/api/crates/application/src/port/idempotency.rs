//! Puerto de idempotencia de las operaciones que crean dinero.
//!
//! El problema es concreto: el usuario guarda un gasto desde el movil, pierde
//! cobertura antes de ver la respuesta y la aplicacion reintenta. Sin esto quedan
//! dos gastos, y como el diseno no tiene pantalla de edicion el usuario no puede
//! arreglarlo. Con esto el segundo intento recibe la misma respuesta que el primero
//! y no se escribe nada nuevo.
//!
//! El cliente manda una cabecera `Idempotency-Key` con un valor que genera el, uno
//! por intento del usuario y el mismo en todos los reintentos de ese intento. La
//! cabecera es opcional: sin ella la peticion se comporta como siempre.
//!
//! La clave se guarda con una huella del cuerpo. Reutilizarla con un cuerpo
//! distinto es un error del cliente, no un reintento, y se rechaza: si no, dos
//! gastos diferentes con la misma clave dejarian uno de los dos sin guardar en
//! silencio.

use async_trait::async_trait;
use gastonomo_domain::UserId;

use crate::error::AppError;

/// Longitud maxima de una clave de idempotencia.
pub const MAX_IDEMPOTENCY_KEY_LENGTH: usize = 200;

/// Horas que se guarda una clave antes de poder reutilizarse.
///
/// Un reintento razonable pasa en segundos o minutos. Un dia deja margen de sobra
/// para un movil que estuvo sin cobertura, y acota lo que crece la tabla.
pub const IDEMPOTENCY_RETENTION_HOURS: i64 = 24;

/// Peticion que se quiere hacer idempotente.
#[derive(Debug, Clone, Copy)]
pub struct IdempotentRequest<'a> {
    /// Cuenta del token. La clave de un usuario no alcanza a otro.
    pub user_id: UserId,
    /// Operacion, por ejemplo `POST /v1/movements`. La misma clave en dos
    /// operaciones distintas son dos reservas distintas.
    pub endpoint: &'a str,
    /// Valor de la cabecera `Idempotency-Key`.
    pub key: &'a str,
    /// Huella del cuerpo, para distinguir un reintento de una reutilizacion.
    pub fingerprint: &'a str,
}

/// Respuesta ya emitida para una clave.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct IdempotentResponse {
    /// Codigo de estado con el que se respondio.
    pub status: u16,
    /// Cuerpo JSON tal y como se envio.
    pub body: Vec<u8>,
}

/// Que hacer con una peticion que trae clave de idempotencia.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Reservation {
    /// La clave es nueva: hay que ejecutar la operacion y guardar su respuesta.
    Fresh,
    /// Ya hay respuesta para esta clave: se repite tal cual, sin escribir nada.
    Replay(IdempotentResponse),
    /// La clave esta reservada y todavia sin respuesta: hay otro intento en vuelo.
    InProgress,
    /// La clave ya existe con otra huella: es reutilizacion, no reintento.
    Mismatch,
}

/// Guarda que claves de idempotencia se han usado y con que respuesta.
#[async_trait]
pub trait IdempotencyStore: Send + Sync {
    /// Reserva la clave y dice que hacer con la peticion.
    ///
    /// La reserva tiene que ser atomica: dos reintentos simultaneos no pueden
    /// recibir los dos [`Reservation::Fresh`].
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn reserve(&self, request: IdempotentRequest<'_>) -> Result<Reservation, AppError>;

    /// Guarda la respuesta de una reserva para poder repetirla.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn complete(
        &self,
        request: IdempotentRequest<'_>,
        response: &IdempotentResponse,
    ) -> Result<(), AppError>;

    /// Suelta una reserva sin respuesta, para que el cliente pueda reintentar.
    ///
    /// Se usa cuando la operacion no llego a crear nada: un cuerpo invalido es
    /// determinista y guardar su rechazo solo obligaria al cliente a inventarse una
    /// clave nueva para corregir un dato.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn release(&self, request: IdempotentRequest<'_>) -> Result<(), AppError>;
}
