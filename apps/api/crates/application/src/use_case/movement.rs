//! Casos de uso del contexto de movimientos.
//!
//! Son dos, los que el diseno necesita: listar el periodo y registrar uno nuevo.
//! Editar y borrar no existen todavia porque el handoff los deja sin pantalla.
//!
//! No hay caso de uso de resumen. La razon esta en
//! [`ListMovements`]: los totales, la tendencia y el desglose por categoria se
//! calculan sobre el valor convertido a la moneda principal del momento, que solo
//! conoce el cliente.

mod create_movement;
mod list_movements;

#[cfg(test)]
mod tests;

pub use create_movement::{CreateMovement, CreateMovementCommand};
pub use list_movements::{
    DEFAULT_PAGE_LIMIT, ListMovements, ListMovementsQuery, MAX_PAGE_LIMIT, MovementPage,
};
