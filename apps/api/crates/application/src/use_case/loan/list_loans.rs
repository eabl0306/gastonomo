//! Caso de uso: lista de prestamos con sus abonos.

use gastonomo_domain::UserId;
use gastonomo_domain::loan::{Loan, LoanDirection};

use super::ports::LoanPorts;
use crate::error::AppError;

/// Filtro de la lista, el de las pills Todos / Me deben / Debo.
#[derive(Debug, Clone, Default)]
pub struct ListLoansQuery {
    /// `lent`, `borrowed` o nada para todos.
    pub direction: Option<String>,
}

/// Lista los prestamos del usuario.
///
/// El orden que pide el diseno (abiertos primero, pagados al final) lo hace el
/// cliente, porque saber si un prestamo esta pagado exige convertir con las tasas
/// vigentes. Aqui salen del mas reciente al mas antiguo.
#[derive(Debug, Clone)]
pub struct ListLoans {
    ports: LoanPorts,
}

impl ListLoans {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: LoanPorts) -> Self {
        Self { ports }
    }

    /// Devuelve los prestamos de la cuenta, filtrados por direccion si se pide.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si la direccion no es `lent` ni
    /// `borrowed`, y [`AppError::Dependency`] si falla la persistencia.
    pub async fn execute(
        &self,
        user_id: UserId,
        query: &ListLoansQuery,
    ) -> Result<Vec<Loan>, AppError> {
        let direction = parse_direction(query.direction.as_deref())?;
        self.ports.loans.list(user_id, direction).await
    }
}

/// Un filtro ausente y un filtro vacio son lo mismo: todos.
fn parse_direction(raw: Option<&str>) -> Result<Option<LoanDirection>, AppError> {
    let Some(value) = raw.map(str::trim).filter(|value| !value.is_empty()) else {
        return Ok(None);
    };
    LoanDirection::parse(value).map(Some).map_err(|_| {
        AppError::validation("direction", "El tipo tiene que ser `lent` o `borrowed`.")
    })
}
