//! Tests del adaptador de movimientos contra un PostgreSQL de verdad.
//!
//! Mismas reglas que `postgres_integration.rs`: sin `GASTONOMO_TEST_DATABASE_URL`
//! cada test se salta a si mismo, y con `GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta
//! de base de datos es un fallo.
//!
//! ```sh
//! just api-test-db
//! ```

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

mod movement_fixture;

use chrono::Utc;
use gastonomo_application::port::movement::{
    MovementCategoryReader, MovementFilter, MovementRepository as _,
};
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementKind, MovementTitle, NewMovement,
};
use gastonomo_domain::{CurrencyCode, MonetaryAmount, Money, UserId};
use gastonomo_infrastructure::persistence::PostgresMovementRepository;
use movement_fixture::{Fixture, date};

fn amount(value: &str) -> MonetaryAmount {
    MonetaryAmount::single_currency(
        Money::parse(value).expect("fixture amount"),
        CurrencyCode::parse("COP").expect("fixture currency"),
    )
    .expect("fixture monetary amount")
}

fn movement(
    user_id: UserId,
    category: MovementCategory,
    occurred_on: &str,
    kind: MovementKind,
    value: &str,
) -> Movement {
    let title = MovementTitle::parse(category.name()).expect("fixture title");
    Movement::record(
        NewMovement {
            user_id,
            occurred_on: date(occurred_on),
            kind,
            category,
            title,
            amount: amount(value),
            loan_id: None,
            loan_payment_id: None,
            created_at: Utc::now(),
        },
        date(occurred_on),
    )
    .expect("fixture movement")
}

const fn filter(user_id: UserId) -> MovementFilter {
    MovementFilter {
        user_id,
        from: None,
        to: None,
        kind: None,
        after: None,
        limit: 100,
    }
}

#[tokio::test]
async fn a_movement_round_trips_with_its_category_and_its_four_amounts() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.category("Alimentación").await;
    let stored = movement(
        fixture.user,
        category.clone(),
        "2026-09-15",
        MovementKind::Out,
        "186400",
    );

    repository.insert(&stored).await.expect("should insert");
    let listed = repository
        .list(&filter(fixture.user))
        .await
        .expect("should list");

    assert_eq!(listed.len(), 1);
    let read = &listed[0];
    assert_eq!(read.id(), stored.id());
    assert_eq!(read.occurred_on(), date("2026-09-15"));
    assert_eq!(read.kind(), MovementKind::Out);
    assert_eq!(read.amount().amount().to_storage_string(), "186400.0000");
    assert_eq!(read.amount().base().to_storage_string(), "186400.0000");
    assert_eq!(read.amount().currency().as_str(), "COP");
    assert_eq!(read.category().id(), category.id());
    assert_eq!(read.category().name(), "Alimentación");
    assert_eq!(read.category().icon(), "restaurant");
}

#[tokio::test]
async fn a_movement_whose_category_got_archived_still_comes_back_complete() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.custom_category("Mascotas").await;
    let stored = movement(
        fixture.user,
        category.clone(),
        "2026-09-15",
        MovementKind::Out,
        "186400",
    );
    repository.insert(&stored).await.expect("should insert");

    archive(&fixture, &category).await;

    let listed = repository
        .list(&filter(fixture.user))
        .await
        .expect("should list");

    // El join no filtra por archivada a proposito: si lo hiciera, el movimiento
    // desapareceria de la lista y el gasto se perderia de vista.
    assert_eq!(listed.len(), 1);
    assert_eq!(listed[0].category().name(), "Mascotas");
    assert_eq!(listed[0].category().icon(), "pets");
    assert_eq!(listed[0].category().id(), category.id());
}

#[tokio::test]
async fn an_archived_category_can_still_receive_a_movement_that_was_waiting() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.custom_category("Gimnasio").await;
    archive(&fixture, &category).await;

    let resolved = MovementCategoryReader::find(&repository, fixture.user, category.id())
        .await
        .expect("should read the category");

    // Se encuentra: si no, un movimiento encolado sin conexion antes de archivar no
    // podria subirse nunca, y el historico no tiene como arreglarse a mano.
    assert_eq!(
        resolved.map(|found| found.name().to_owned()),
        Some("Gimnasio".to_owned())
    );
}

#[tokio::test]
async fn a_converted_amount_keeps_the_rate_the_user_typed() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.category("Freelance").await;
    let mut data = movement(fixture.user, category, "2026-09-05", MovementKind::In, "1");
    data = Movement::rehydrate(
        data.id(),
        NewMovement {
            user_id: data.user_id(),
            occurred_on: data.occurred_on(),
            kind: data.kind(),
            category: data.category().clone(),
            title: data.title().clone(),
            amount: MonetaryAmount::new(
                Money::parse("140").expect("fixture"),
                CurrencyCode::parse("USD").expect("fixture"),
                Money::parse("450000").expect("fixture"),
                CurrencyCode::parse("COP").expect("fixture"),
            )
            .expect("fixture"),
            loan_id: None,
            loan_payment_id: None,
            created_at: data.created_at(),
        },
        data.updated_at(),
    );

    repository.insert(&data).await.expect("should insert");
    let listed = repository
        .list(&filter(fixture.user))
        .await
        .expect("should list");

    let read = &listed[0];
    assert_eq!(read.amount().amount().to_storage_string(), "140.0000");
    assert_eq!(read.amount().currency().as_str(), "USD");
    assert_eq!(read.amount().base().to_storage_string(), "450000.0000");
    assert_eq!(read.amount().base_currency().as_str(), "COP");
    assert!(read.amount().is_converted());
}

#[tokio::test]
async fn the_list_filters_by_period_and_by_kind() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let expense = fixture.category("Alimentación").await;
    let income = fixture.category("Salario").await;
    for (day, kind, category) in [
        ("2026-08-25", MovementKind::Out, expense.clone()),
        ("2026-09-15", MovementKind::Out, expense),
        ("2026-09-01", MovementKind::In, income),
    ] {
        let stored = movement(fixture.user, category, day, kind, "1000");
        repository.insert(&stored).await.expect("should insert");
    }

    let month = repository
        .list(&MovementFilter {
            from: Some(date("2026-09-01")),
            to: Some(date("2026-09-30")),
            ..filter(fixture.user)
        })
        .await
        .expect("should list");
    let income_only = repository
        .list(&MovementFilter {
            kind: Some(MovementKind::In),
            ..filter(fixture.user)
        })
        .await
        .expect("should list");

    assert_eq!(month.len(), 2);
    assert_eq!(month[0].occurred_on(), date("2026-09-15"));
    assert_eq!(income_only.len(), 1);
}

#[tokio::test]
async fn the_cursor_walks_the_list_without_repeating_a_row() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.category("Alimentación").await;
    for day in 1..=5 {
        let stored = movement(
            fixture.user,
            category.clone(),
            &format!("2026-09-0{day}"),
            MovementKind::Out,
            "1000",
        );
        repository.insert(&stored).await.expect("should insert");
    }

    let mut seen = Vec::new();
    let mut after = None;
    loop {
        let page = repository
            .list(&MovementFilter {
                limit: 2,
                after,
                ..filter(fixture.user)
            })
            .await
            .expect("should list");
        if page.is_empty() {
            break;
        }
        after = page.last().map(Movement::cursor);
        seen.extend(page.iter().map(Movement::id));
    }

    let unique: std::collections::BTreeSet<_> = seen.iter().copied().collect();
    assert_eq!(seen.len(), 5);
    assert_eq!(unique.len(), 5);
}

#[tokio::test]
async fn the_list_never_crosses_accounts() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let Some(other) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let category = fixture.category("Alimentación").await;
    let stored = movement(
        fixture.user,
        category,
        "2026-09-15",
        MovementKind::Out,
        "186400",
    );
    repository.insert(&stored).await.expect("should insert");

    let seen_by_the_other = repository
        .list(&filter(other.user))
        .await
        .expect("should list");

    assert!(seen_by_the_other.is_empty());
}

#[tokio::test]
async fn a_category_of_another_account_reads_as_missing() {
    let Some(fixture) = Fixture::new().await else {
        return;
    };
    let Some(other) = Fixture::new().await else {
        return;
    };
    let repository = PostgresMovementRepository::new(fixture.pool.clone());
    let mine = fixture.category("Alimentación").await;

    let found = repository
        .find(fixture.user, mine.id())
        .await
        .expect("should query");
    let foreign = repository
        .find(other.user, mine.id())
        .await
        .expect("should query");
    let unknown = repository
        .find(fixture.user, CategoryId::generate())
        .await
        .expect("should query");

    assert_eq!(found.map(|category| category.id()), Some(mine.id()));
    assert!(foreign.is_none());
    assert!(unknown.is_none());
}

/// Archiva la categoria por debajo, como lo dejaria `DELETE /v1/categories/{id}`.
async fn archive(fixture: &Fixture, category: &MovementCategory) {
    sqlx::query("UPDATE categories SET archived_at = now() WHERE id = $1")
        .bind(category.id().as_uuid())
        .execute(&fixture.pool)
        .await
        .expect("should archive the category");
}
