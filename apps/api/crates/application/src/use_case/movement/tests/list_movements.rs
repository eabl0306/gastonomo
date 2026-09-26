//! Tests de `ListMovements`.

use chrono::NaiveDate;
use gastonomo_domain::movement::{Movement, MovementKind};

use super::support::{World, stored};
use crate::testing::block_on;
use crate::use_case::movement::{ListMovementsQuery, MAX_PAGE_LIMIT};

fn date(raw: &str) -> NaiveDate {
    NaiveDate::parse_from_str(raw, "%Y-%m-%d").expect("fixture date")
}

fn query() -> ListMovementsQuery {
    ListMovementsQuery::default()
}

#[test]
fn the_list_goes_from_the_most_recent_to_the_oldest() {
    let world = World::new();
    world.movements.seed(vec![
        stored(world.user, "2026-09-01", MovementKind::In, "2800000"),
        stored(world.user, "2026-09-20", MovementKind::Out, "32000"),
        stored(world.user, "2026-09-15", MovementKind::Out, "186400"),
    ]);

    let page = block_on(world.list().execute(world.user, &query())).expect("should list");

    let dates: Vec<String> = page
        .items
        .iter()
        .map(|movement| movement.occurred_on().to_string())
        .collect();
    assert_eq!(dates, ["2026-09-20", "2026-09-15", "2026-09-01"]);
    assert!(page.next_cursor.is_none());
}

#[test]
fn the_period_filter_is_inclusive_on_both_ends() {
    let world = World::new();
    world.movements.seed(vec![
        stored(world.user, "2026-08-31", MovementKind::Out, "1000"),
        stored(world.user, "2026-09-01", MovementKind::Out, "1000"),
        stored(world.user, "2026-09-30", MovementKind::Out, "1000"),
        stored(world.user, "2026-10-01", MovementKind::Out, "1000"),
    ]);

    let page = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            from: Some(date("2026-09-01")),
            to: Some(date("2026-09-30")),
            ..query()
        },
    ))
    .expect("should list");

    assert_eq!(page.items.len(), 2);
}

#[test]
fn the_kind_filter_matches_the_pills() {
    let world = World::new();
    world.movements.seed(vec![
        stored(world.user, "2026-09-01", MovementKind::In, "2800000"),
        stored(world.user, "2026-09-02", MovementKind::Out, "1200000"),
    ]);

    let page = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            kind: Some(MovementKind::In),
            ..query()
        },
    ))
    .expect("should list");

    assert_eq!(page.items.len(), 1);
    assert_eq!(page.items[0].kind(), MovementKind::In);
}

#[test]
fn another_account_never_appears_in_the_list() {
    let world = World::new();
    world.movements.seed(vec![stored(
        world.other_user,
        "2026-09-20",
        MovementKind::Out,
        "99999",
    )]);

    let page = block_on(world.list().execute(world.user, &query())).expect("should list");

    assert!(page.items.is_empty());
}

#[test]
fn the_cursor_walks_the_whole_period_without_repeating_a_row() {
    let world = World::new();
    world.movements.seed(
        (1..=5)
            .map(|day| {
                stored(
                    world.user,
                    &format!("2026-09-0{day}"),
                    MovementKind::Out,
                    "1000",
                )
            })
            .collect(),
    );

    let mut seen = Vec::new();
    let mut cursor = None;
    loop {
        let page = block_on(world.list().execute(
            world.user,
            &ListMovementsQuery {
                limit: Some(2),
                cursor: cursor.clone(),
                ..query()
            },
        ))
        .expect("should list");
        seen.extend(page.items.iter().map(Movement::id));
        match page.next_cursor {
            Some(next) => cursor = Some(next.to_string()),
            None => break,
        }
    }

    let unique: std::collections::BTreeSet<_> = seen.iter().copied().collect();
    assert_eq!(seen.len(), 5);
    assert_eq!(unique.len(), 5);
}

#[test]
fn a_full_page_reports_the_cursor_and_the_last_one_does_not() {
    let world = World::new();
    world.movements.seed(vec![
        stored(world.user, "2026-09-01", MovementKind::Out, "1000"),
        stored(world.user, "2026-09-02", MovementKind::Out, "1000"),
    ]);

    let first = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            limit: Some(1),
            ..query()
        },
    ))
    .expect("should list");
    assert!(first.next_cursor.is_some());

    let second = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            limit: Some(1),
            cursor: first.next_cursor.map(|cursor| cursor.to_string()),
            ..query()
        },
    ))
    .expect("should list");
    assert!(second.next_cursor.is_none());
}

#[test]
fn a_period_the_wrong_way_round_is_rejected() {
    let world = World::new();

    let error = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            from: Some(date("2026-09-30")),
            to: Some(date("2026-09-01")),
            ..query()
        },
    ))
    .expect_err("should reject");

    assert!(format!("{error:?}").contains("from"));
}

#[test]
fn a_bogus_cursor_is_rejected() {
    let world = World::new();

    let error = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            cursor: Some("no-soy-un-cursor".to_owned()),
            ..query()
        },
    ))
    .expect_err("should reject");

    assert!(format!("{error:?}").contains("cursor"));
}

#[test]
fn an_oversized_limit_is_capped_instead_of_rejected() {
    let world = World::new();
    world.movements.seed(vec![stored(
        world.user,
        "2026-09-01",
        MovementKind::Out,
        "1000",
    )]);

    let page = block_on(world.list().execute(
        world.user,
        &ListMovementsQuery {
            limit: Some(MAX_PAGE_LIMIT + 10_000),
            ..query()
        },
    ))
    .expect("should list");

    assert_eq!(page.items.len(), 1);
}
