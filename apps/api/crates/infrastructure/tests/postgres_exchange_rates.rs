//! Tests de integracion del adaptador de tasas contra PostgreSQL.
//!
//! Mismas reglas que `postgres_integration.rs`: sin `GASTONOMO_TEST_DATABASE_URL`
//! cada test se salta a si mismo, y con `GASTONOMO_TEST_REQUIRE_DATABASE=1` la
//! falta de base de datos es un fallo.
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

mod rates_support;

use chrono::{Duration, NaiveDate};
use gastonomo_application::port::currency::{ExchangeRateRepository, SaveOutcome};
use gastonomo_domain::currency::{ExchangeRate, ExchangeRateTable, RateStatus, rates_base};
use gastonomo_infrastructure::PostgresExchangeRateRepository;
use rates_support::{bulletin, clean, code, date, instant, pool, serially};

/// Cada test abre y cierra con las dos tablas vacias.
///
/// Si se quedasen con datos de test, el siguiente arranque de la API los veria
/// recientes y no refrescaria hasta 24 horas despues: la maquina de desarrollo se
/// quedaria con una sola moneda en el sheet.
async fn latest(repository: &PostgresExchangeRateRepository) -> ExchangeRateTable {
    repository
        .latest(&rates_base())
        .await
        .expect("the query should succeed")
        .expect("there should be a stored table")
}

#[tokio::test]
async fn an_empty_database_has_no_table_and_no_currencies() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());

    assert!(
        repository
            .latest(&rates_base())
            .await
            .expect("the query should succeed")
            .is_none()
    );
    assert!(
        repository
            .offered_currencies()
            .await
            .expect("the query should succeed")
            .is_empty()
    );
    clean(&pool).await;
}

#[tokio::test]
async fn a_pass_is_stored_whole_and_read_back_exactly() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());
    let pass = bulletin(
        date(25),
        &[("COP", "3316.44"), ("EUR", "0.87732"), ("JPY", "158.37")],
        &[
            ("COP", "Colombian Peso"),
            ("EUR", "Euro"),
            ("JPY", "Japanese Yen"),
            ("USD", "United States Dollar"),
        ],
    );

    let outcome = repository
        .save(&pass, instant("2026-09-25T06:00:12Z"))
        .await
        .expect("the write should succeed");

    assert_eq!(outcome, SaveOutcome::Written);
    let table = latest(&repository).await;
    assert_eq!(table.date(), date(25));
    assert_eq!(table.fetched_at(), instant("2026-09-25T06:00:12Z"));
    assert_eq!(table.rate(&code("USD")), Some(ExchangeRate::ONE));
    assert_eq!(
        table.rate(&code("COP")).map(|rate| rate.to_string()),
        Some("3316.44".to_owned()),
        "numeric(20,10) no puede cambiar los digitos que publico la fuente"
    );
    assert_eq!(
        table.rate(&code("EUR")).map(|rate| rate.to_string()),
        Some("0.87732".to_owned())
    );
    let codes: Vec<String> = repository
        .offered_currencies()
        .await
        .expect("the query should succeed")
        .iter()
        .map(|currency| currency.code().to_string())
        .collect();
    assert_eq!(codes, ["COP", "EUR", "JPY", "USD"]);
    clean(&pool).await;
}

#[tokio::test]
async fn the_same_date_twice_updates_the_row_instead_of_adding_one() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());
    let first = bulletin(
        date(25),
        &[("COP", "3316.44")],
        &[("COP", "Colombian Peso")],
    );
    let corrected = bulletin(
        date(25),
        &[("COP", "3320.10")],
        &[("COP", "Colombian Peso")],
    );

    repository
        .save(&first, instant("2026-09-25T06:00:00Z"))
        .await
        .expect("the first write should succeed");
    repository
        .save(&corrected, instant("2026-09-25T18:00:00Z"))
        .await
        .expect("the second write should succeed");

    let rows: i64 = sqlx::query_scalar("SELECT count(*) FROM exchange_rates")
        .fetch_one(&pool)
        .await
        .expect("the count should succeed");
    assert_eq!(rows, 2, "COP y la propia base, una fila cada una");
    let table = latest(&repository).await;
    assert_eq!(
        table.rate(&code("COP")).map(|rate| rate.to_string()),
        Some("3320.1".to_owned())
    );
    assert_eq!(
        table.fetched_at(),
        instant("2026-09-25T18:00:00Z"),
        "el instante del refresco si avanza"
    );
    clean(&pool).await;
}

#[tokio::test]
async fn a_newer_date_wins_and_the_older_one_stays() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());

    repository
        .save(
            &bulletin(
                date(24),
                &[("COP", "3199.41")],
                &[("COP", "Colombian Peso")],
            ),
            instant("2026-09-24T06:00:00Z"),
        )
        .await
        .expect("the first write should succeed");
    repository
        .save(
            &bulletin(
                date(25),
                &[("COP", "3316.44")],
                &[("COP", "Colombian Peso")],
            ),
            instant("2026-09-25T06:00:00Z"),
        )
        .await
        .expect("the second write should succeed");

    let table = latest(&repository).await;
    assert_eq!(table.date(), date(25));
    let dates: Vec<NaiveDate> =
        sqlx::query_scalar("SELECT DISTINCT rate_date FROM exchange_rates ORDER BY rate_date")
            .fetch_all(&pool)
            .await
            .expect("the query should succeed");
    assert_eq!(dates, [date(24), date(25)], "nunca se borra lo anterior");
    clean(&pool).await;
}

#[tokio::test]
async fn a_currency_that_disappears_stops_being_offered_but_is_not_deleted() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());

    repository
        .save(
            &bulletin(
                date(24),
                &[("COP", "3199.41"), ("ZWL", "322.0")],
                &[
                    ("COP", "Colombian Peso"),
                    ("USD", "United States Dollar"),
                    ("ZWL", "Zimbabwean Dollar"),
                ],
            ),
            instant("2026-09-24T06:00:00Z"),
        )
        .await
        .expect("the first write should succeed");
    repository
        .save(
            &bulletin(
                date(25),
                &[("COP", "3316.44")],
                &[("COP", "Colombian Peso"), ("USD", "United States Dollar")],
            ),
            instant("2026-09-25T06:00:00Z"),
        )
        .await
        .expect("the second write should succeed");

    let offered: Vec<String> = repository
        .offered_currencies()
        .await
        .expect("the query should succeed")
        .iter()
        .map(|currency| currency.code().to_string())
        .collect();
    assert_eq!(offered, ["COP", "USD"], "el dolar sigue, el ZWL ya no");
    let stored: i64 = sqlx::query_scalar("SELECT count(*) FROM currencies WHERE code = 'ZWL'")
        .fetch_one(&pool)
        .await
        .expect("the count should succeed");
    assert_eq!(
        stored, 1,
        "la fila sigue ahi para los registros que la usan"
    );
    clean(&pool).await;
}

#[tokio::test]
async fn the_status_turns_stale_once_the_last_refresh_is_two_days_old() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());
    let fetched_at = instant("2026-09-25T06:00:00Z");
    repository
        .save(
            &bulletin(
                date(25),
                &[("COP", "3316.44")],
                &[("COP", "Colombian Peso")],
            ),
            fetched_at,
        )
        .await
        .expect("the write should succeed");

    let table = latest(&repository).await;

    assert_eq!(
        table.status(fetched_at + Duration::hours(47)),
        RateStatus::Ok
    );
    assert_eq!(
        table.status(fetched_at + Duration::hours(49)),
        RateStatus::Stale
    );
    assert!(!table.is_due_for_refresh(fetched_at + Duration::hours(23)));
    assert!(table.is_due_for_refresh(fetched_at + Duration::hours(25)));
    clean(&pool).await;
}

#[tokio::test]
async fn a_second_replica_finds_the_refresh_taken_and_does_not_block() {
    let _serial = serially().await;
    let Some(pool) = pool().await else {
        return;
    };
    clean(&pool).await;
    let repository = PostgresExchangeRateRepository::new(pool.clone());
    let pass = bulletin(
        date(25),
        &[("COP", "3316.44")],
        &[("COP", "Colombian Peso")],
    );

    // Otra sesion toma el mismo cerrojo y lo mantiene mientras se intenta escribir.
    let mut holder = pool
        .begin()
        .await
        .expect("the holding transaction should open");
    let taken: bool = sqlx::query_scalar("SELECT pg_try_advisory_xact_lock(8314072026)")
        .fetch_one(&mut *holder)
        .await
        .expect("the lock query should succeed");
    assert!(taken, "the holder should get the lock first");

    let outcome = repository
        .save(&pass, instant("2026-09-25T06:00:00Z"))
        .await
        .expect("skipping is not an error");

    assert_eq!(outcome, SaveOutcome::LockedElsewhere);
    assert!(
        repository
            .latest(&rates_base())
            .await
            .expect("the query should succeed")
            .is_none(),
        "la replica que no gana el cerrojo no escribe nada"
    );

    holder
        .rollback()
        .await
        .expect("the holding transaction should close");
    assert_eq!(
        repository
            .save(&pass, instant("2026-09-25T06:00:00Z"))
            .await
            .expect("the write should succeed now"),
        SaveOutcome::Written
    );
    clean(&pool).await;
}
