//! Tests de los casos de uso de movimientos, con dobles en memoria.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

mod create_movement;
mod list_movements;
mod support;
