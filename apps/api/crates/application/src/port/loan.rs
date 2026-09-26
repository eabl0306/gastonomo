//! Puertos del contexto de prestamos.

use async_trait::async_trait;
use gastonomo_domain::UserId;
use gastonomo_domain::loan::{GeneratedMovement, Loan, LoanDirection, LoanId, LoanPayment};

use crate::error::AppError;

/// Lee y escribe prestamos y sus abonos.
///
/// Crear un prestamo y crear un abono son operaciones de **dos** escrituras: la
/// fila del prestamo (o del abono) y el movimiento que el diseno exige. Por eso el
/// puerto tiene un metodo por operacion y no uno por tabla: el adaptador es el
/// unico que puede garantizar que las dos van en la misma transaccion, y si la
/// segunda falla no puede quedar la primera.
#[async_trait]
pub trait LoanRepository: Send + Sync {
    /// Lista los prestamos del usuario con sus abonos embebidos.
    ///
    /// Los abonos vienen dentro porque la tarjeta del diseno necesita el saldo, y
    /// el saldo es el prestamo menos la suma de los abonos.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn list(
        &self,
        user_id: UserId,
        direction: Option<LoanDirection>,
    ) -> Result<Vec<Loan>, AppError>;

    /// Busca un prestamo del usuario, con sus abonos.
    ///
    /// Filtra por usuario en la propia consulta: un prestamo de otra cuenta se
    /// comporta como inexistente.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find(&self, user_id: UserId, loan_id: LoanId) -> Result<Option<Loan>, AppError>;

    /// Guarda el prestamo y su movimiento en una sola transaccion.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos o si a la
    /// cuenta le falta la categoria predeterminada del movimiento. En los dos
    /// casos la transaccion se deshace y no queda ni el prestamo ni el
    /// movimiento.
    async fn create(&self, loan: &Loan, movement: &GeneratedMovement) -> Result<(), AppError>;

    /// Guarda el abono y su movimiento en una sola transaccion.
    ///
    /// # Errors
    ///
    /// Igual que [`LoanRepository::create`]: si la segunda escritura falla no
    /// queda ninguna de las dos.
    async fn add_payment(
        &self,
        payment: &LoanPayment,
        movement: &GeneratedMovement,
    ) -> Result<(), AppError>;
}
