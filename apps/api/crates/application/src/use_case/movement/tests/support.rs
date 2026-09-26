//! Montaje comun de los tests de movimientos.

use std::sync::Arc;

use chrono::NaiveDate;
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementKind, MovementTitle, NewMovement,
};
use gastonomo_domain::{CategoryKind, CurrencyCode, MonetaryAmount, Money, UserId};

use crate::port::Clock as _;
use crate::testing::{FixedClock, InMemoryMovementRepository, StubMovementCategoryReader};
use crate::use_case::movement::{CreateMovement, CreateMovementCommand, ListMovements};

/// Fecha de "hoy" del prototipo, la misma que trae [`FixedClock::at_fixture`].
pub const TODAY: &str = "2026-09-22";

/// Todo lo que un test necesita tocar.
pub struct World {
    pub movements: Arc<InMemoryMovementRepository>,
    pub categories: Arc<StubMovementCategoryReader>,
    pub clock: Arc<FixedClock>,
    pub user: UserId,
    pub other_user: UserId,
}

impl World {
    pub fn new() -> Self {
        let world = Self {
            movements: Arc::new(InMemoryMovementRepository::new()),
            categories: Arc::new(StubMovementCategoryReader::new()),
            clock: Arc::new(FixedClock::at_fixture()),
            user: UserId::generate(),
            other_user: UserId::generate(),
        };
        world.categories.add(world.user, groceries());
        world.categories.add(world.user, salary());
        world.categories.add(world.other_user, other_groceries());
        world
    }

    pub fn create(&self) -> CreateMovement {
        CreateMovement::new(
            self.movements.clone(),
            self.categories.clone(),
            self.clock.clone(),
        )
    }

    pub fn list(&self) -> ListMovements {
        ListMovements::new(self.movements.clone())
    }
}

pub fn category(id: CategoryId, name: &str, kind: CategoryKind) -> MovementCategory {
    MovementCategory::new(id, name, kind, "restaurant", "#cf5f35").expect("fixture is valid")
}

/// Categoria de gasto del usuario del test.
pub fn groceries() -> MovementCategory {
    category(fixed_id(1), "Alimentación", CategoryKind::Out)
}

/// Categoria de ingreso del usuario del test.
pub fn salary() -> MovementCategory {
    category(fixed_id(2), "Salario", CategoryKind::In)
}

/// Categoria que pertenece a otra cuenta.
pub fn other_groceries() -> MovementCategory {
    category(fixed_id(3), "Alimentación", CategoryKind::Out)
}

/// Identificador estable, para que un test pueda escribirlo en el comando.
pub fn fixed_id(seed: u8) -> CategoryId {
    CategoryId::parse(&format!("00000000-0000-4000-8000-0000000000{seed:02}"))
        .expect("fixture is a valid uuid")
}

/// Comando valido de gasto en pesos.
pub fn expense() -> CreateMovementCommand {
    CreateMovementCommand {
        occurred_on: TODAY.to_owned(),
        kind: "out".to_owned(),
        category_id: fixed_id(1).to_string(),
        note: Some("Mercado".to_owned()),
        amount: "186400".to_owned(),
        currency: "COP".to_owned(),
        base: None,
        base_currency: None,
    }
}

/// Movimiento ya guardado, para precargar el repositorio.
pub fn stored(user_id: UserId, date: &str, kind: MovementKind, amount: &str) -> Movement {
    let occurred_on = NaiveDate::parse_from_str(date, "%Y-%m-%d").expect("fixture date");
    let currency = CurrencyCode::parse("COP").expect("fixture currency");
    let amount = Money::parse(amount).expect("fixture amount");
    let category = if kind == MovementKind::In {
        salary()
    } else {
        groceries()
    };
    let title = MovementTitle::parse(category.name()).expect("fixture title");
    Movement::record(
        NewMovement {
            user_id,
            occurred_on,
            kind,
            category,
            title,
            amount: MonetaryAmount::single_currency(amount, currency).expect("fixture amount"),
            loan_id: None,
            loan_payment_id: None,
            created_at: FixedClock::at_fixture().now(),
        },
        occurred_on,
    )
    .expect("fixture movement")
}
