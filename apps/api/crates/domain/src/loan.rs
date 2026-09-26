//! Prestamos y abonos.
//!
//! Un prestamo guarda a quien se le presto (o quien presto), la direccion y su
//! importe con los cuatro campos de [`crate::value_object::MonetaryAmount`]. Los
//! abonos son filas aparte que viajan embebidas en el prestamo, porque la tarjeta
//! del diseno necesita el saldo y el saldo sale de restarlos.
//!
//! Lo que este modulo **no** hace, a proposito: no calcula el saldo, ni el estado
//! (Pagado, Parcial, Pendiente), ni el porcentaje, ni los intereses ganados. Todo
//! eso depende de la moneda principal que el usuario tenga puesta en ese momento y
//! de las tasas vigentes, asi que lo deriva el cliente.
//!
//! Crear un prestamo o un abono escribe tambien un movimiento. Ese movimiento se
//! describe aqui con [`GeneratedMovement`], que es solo la fila que hay que
//! insertar: el agregado de movimientos pertenece a su propio contexto.

mod agreement;
mod direction;
mod generated_movement;
mod payment;

pub use agreement::{Loan, LoanId, LoanIdError, MAX_LOAN_NOTE_LENGTH, NewLoan};
pub use direction::{LoanDirection, LoanDirectionError};
pub use generated_movement::GeneratedMovement;
pub use payment::{LoanPayment, LoanPaymentId, MAX_PAYMENT_LABEL_LENGTH, NewLoanPayment};
