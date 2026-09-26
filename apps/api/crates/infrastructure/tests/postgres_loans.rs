//! Tests de integracion del repositorio de prestamos contra PostgreSQL.
//!
//! Aqui se prueba lo que solo se puede probar con una base de datos de verdad: que
//! el prestamo y su movimiento entran en la misma transaccion y que, si la segunda
//! escritura falla, no queda la primera.
//!
//! Mismas reglas que `postgres_auth.rs`: sin `GASTONOMO_TEST_DATABASE_URL` cada test
//! se salta a si mismo, y con `GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta de base
//! de datos es un fallo.
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

use chrono::{NaiveDate, Utc};
use gastonomo_application::port::loan::LoanRepository;
use gastonomo_application::port::{UserProvisioning as _, UserRepository as _};
use gastonomo_domain::loan::{
    GeneratedMovement, Loan, LoanDirection, LoanPayment, NewLoan, NewLoanPayment,
};
use gastonomo_domain::{
    CurrencyCode, Email, MonetaryAmount, Money, NewUser, PasswordHash, PersonName, User, UserId,
};
use gastonomo_infrastructure::persistence::PostgresLoanRepository;
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresUserProvisioning, PostgresUserRepository, connect_pool,
    run_migrations,
};
use sqlx::Row as _;

/// Variable que habilita esta bateria de tests.
const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

fn database_url() -> Option<String> {
    let url = std::env::var(URL_VAR).ok().filter(|url| !url.is_empty());
    let mandatory = std::env::var(REQUIRE_VAR)
        .is_ok_and(|value| !matches!(value.trim(), "" | "0" | "false" | "FALSE"));
    assert!(
        url.is_some() || !mandatory,
        "{URL_VAR} is empty but {REQUIRE_VAR} is set: \
         these tests must not pass without a real postgres"
    );
    url
}

async fn pool() -> Option<PgPool> {
    let config = DatabaseConfig {
        url: database_url()?,
        max_connections: 2,
        acquire_timeout_ms: 30_000,
    };
    let pool = connect_pool(&config).expect("pool should build");
    run_migrations(&pool)
        .await
        .expect("migrations should apply");
    Some(pool)
}

/// Crea una cuenta. Con `seeded`, tambien sus 12 categorias y sus ajustes.
async fn account(pool: &PgPool, seeded: bool) -> UserId {
    let unique = UserId::generate();
    let user = User::register(NewUser {
        name: PersonName::parse("Camilo Rojas").expect("valid"),
        email: Email::parse(&format!("loans-{unique}@correo.com")).expect("valid"),
        password_hash: PasswordHash::parse("$argon2id$v=19$fixture").expect("valid"),
        created_at: Utc::now(),
    });
    PostgresUserRepository::new(pool.clone())
        .insert(&user)
        .await
        .expect("the account should be stored");
    if seeded {
        PostgresUserProvisioning::new(pool.clone())
            .provision_new_user(user.id(), Utc::now())
            .await
            .expect("the seed should run");
    }
    user.id()
}

fn amount(value: &str, currency: &str) -> MonetaryAmount {
    MonetaryAmount::single_currency(
        Money::parse(value).expect("valid amount"),
        CurrencyCode::parse(currency).expect("valid currency"),
    )
    .expect("valid pair")
}

fn converted(value: &str, currency: &str, base: &str, base_currency: &str) -> MonetaryAmount {
    MonetaryAmount::new(
        Money::parse(value).expect("valid amount"),
        CurrencyCode::parse(currency).expect("valid currency"),
        Money::parse(base).expect("valid base"),
        CurrencyCode::parse(base_currency).expect("valid currency"),
    )
    .expect("valid pair")
}

fn day(raw: &str) -> NaiveDate {
    NaiveDate::parse_from_str(raw, "%Y-%m-%d").expect("valid date")
}

fn a_loan(user_id: UserId, direction: LoanDirection, money: MonetaryAmount) -> Loan {
    Loan::open(NewLoan {
        user_id,
        person: PersonName::parse("Rosa").expect("valid"),
        note: "Teléfono".to_owned(),
        direction,
        opened_on: day("2026-06-28"),
        amount: money,
        created_at: Utc::now(),
    })
}

async fn count_loans(pool: &PgPool, user_id: UserId) -> i64 {
    scalar(
        pool,
        "SELECT count(*) FROM loans WHERE user_id = $1",
        user_id,
    )
    .await
}

async fn count_movements(pool: &PgPool, user_id: UserId) -> i64 {
    scalar(
        pool,
        "SELECT count(*) FROM movements WHERE user_id = $1",
        user_id,
    )
    .await
}

async fn count_payments(pool: &PgPool, user_id: UserId) -> i64 {
    scalar(
        pool,
        "SELECT count(*) FROM loan_payments WHERE user_id = $1",
        user_id,
    )
    .await
}

async fn scalar(pool: &PgPool, sql: &'static str, user_id: UserId) -> i64 {
    sqlx::query(sql)
        .bind(user_id.as_uuid())
        .fetch_one(pool)
        .await
        .expect("the count should run")
        .try_get::<i64, _>(0)
        .expect("count is a bigint")
}

#[tokio::test]
async fn a_loan_and_its_movement_are_written_in_the_same_transaction() {
    let Some(pool) = pool().await else { return };
    let user_id = account(&pool, true).await;
    let repository = PostgresLoanRepository::new(pool.clone());
    let loan = a_loan(user_id, LoanDirection::Lent, amount("600000", "COP"));

    repository
        .create(&loan, &GeneratedMovement::for_loan(&loan))
        .await
        .expect("the loan and its movement should be stored");

    assert_eq!(count_loans(&pool, user_id).await, 1);
    assert_eq!(count_movements(&pool, user_id).await, 1);

    let row = sqlx::query(
        "SELECT m.kind, m.title, m.loan_id, m.loan_payment_id, c.name AS category \
         FROM movements m JOIN categories c ON c.id = m.category_id WHERE m.user_id = $1",
    )
    .bind(user_id.as_uuid())
    .fetch_one(&pool)
    .await
    .expect("the generated movement should exist");

    assert_eq!(row.try_get::<String, _>("kind").expect("kind"), "out");
    assert_eq!(
        row.try_get::<String, _>("title").expect("title"),
        "Préstamo · Rosa"
    );
    assert_eq!(
        row.try_get::<String, _>("category").expect("category"),
        "Préstamo"
    );
    assert_eq!(
        row.try_get::<uuid::Uuid, _>("loan_id").expect("loan_id"),
        loan.id().as_uuid()
    );
    assert!(
        row.try_get::<Option<uuid::Uuid>, _>("loan_payment_id")
            .expect("loan_payment_id")
            .is_none()
    );
}

#[tokio::test]
async fn if_the_movement_cannot_be_written_the_loan_is_rolled_back() {
    let Some(pool) = pool().await else { return };
    // Sin sembrar: la cuenta no tiene la categoria "Prestamo", asi que la segunda
    // escritura de la transaccion falla de verdad.
    let user_id = account(&pool, false).await;
    let repository = PostgresLoanRepository::new(pool.clone());
    let loan = a_loan(user_id, LoanDirection::Lent, amount("600000", "COP"));

    let error = repository
        .create(&loan, &GeneratedMovement::for_loan(&loan))
        .await
        .expect_err("without the default category the movement cannot be written");

    assert!(
        error.to_string().contains("default category"),
        "the error should name the missing category, got {error}"
    );
    assert_eq!(
        count_loans(&pool, user_id).await,
        0,
        "the loan must not survive its movement failing"
    );
    assert_eq!(count_movements(&pool, user_id).await, 0);
    assert!(
        repository
            .find(user_id, loan.id())
            .await
            .expect("the query should run")
            .is_none()
    );
}

#[tokio::test]
async fn a_payment_and_its_movement_are_written_in_the_same_transaction() {
    let Some(pool) = pool().await else { return };
    let user_id = account(&pool, true).await;
    let repository = PostgresLoanRepository::new(pool.clone());
    let loan = a_loan(user_id, LoanDirection::Borrowed, amount("600000", "COP"));
    repository
        .create(&loan, &GeneratedMovement::for_loan(&loan))
        .await
        .expect("the loan should be stored");

    let payment = LoanPayment::record(NewLoanPayment {
        loan_id: loan.id(),
        user_id,
        paid_on: day("2026-07-29"),
        label: Some("Intereses".to_owned()),
        amount: amount("200000", "COP"),
        created_at: Utc::now(),
    });
    repository
        .add_payment(&payment, &GeneratedMovement::for_payment(&loan, &payment))
        .await
        .expect("the payment and its movement should be stored");

    assert_eq!(count_payments(&pool, user_id).await, 1);
    assert_eq!(count_movements(&pool, user_id).await, 2);

    let row = sqlx::query(
        "SELECT kind, title, loan_payment_id FROM movements \
         WHERE user_id = $1 AND loan_payment_id IS NOT NULL",
    )
    .bind(user_id.as_uuid())
    .fetch_one(&pool)
    .await
    .expect("the payment movement should exist");
    assert_eq!(
        row.try_get::<String, _>("kind").expect("kind"),
        "out",
        "paying back what I borrowed is an expense"
    );
    assert_eq!(
        row.try_get::<String, _>("title").expect("title"),
        "Abono · Rosa"
    );
}

#[tokio::test]
async fn the_loan_comes_back_with_its_payments_most_recent_first() {
    let Some(pool) = pool().await else { return };
    let user_id = account(&pool, true).await;
    let repository = PostgresLoanRepository::new(pool.clone());
    let loan = a_loan(user_id, LoanDirection::Lent, amount("600000", "COP"));
    repository
        .create(&loan, &GeneratedMovement::for_loan(&loan))
        .await
        .expect("the loan should be stored");

    for date in ["2026-07-29", "2026-08-06"] {
        let payment = LoanPayment::record(NewLoanPayment {
            loan_id: loan.id(),
            user_id,
            paid_on: day(date),
            label: None,
            amount: amount("50000", "COP"),
            created_at: Utc::now(),
        });
        repository
            .add_payment(&payment, &GeneratedMovement::for_payment(&loan, &payment))
            .await
            .expect("the payment should be stored");
    }

    let stored = repository
        .find(user_id, loan.id())
        .await
        .expect("the query should run")
        .expect("the loan should be there");
    let dates: Vec<String> = stored
        .payments()
        .iter()
        .map(|payment| payment.paid_on().to_string())
        .collect();
    assert_eq!(dates, vec!["2026-08-06", "2026-07-29"]);
    assert_eq!(stored.note(), "Teléfono");
}

#[tokio::test]
async fn the_four_amount_fields_survive_the_round_trip() {
    let Some(pool) = pool().await else { return };
    let user_id = account(&pool, true).await;
    let repository = PostgresLoanRepository::new(pool.clone());
    let loan = a_loan(
        user_id,
        LoanDirection::Lent,
        converted("140.5000", "USD", "562345.7500", "COP"),
    );
    repository
        .create(&loan, &GeneratedMovement::for_loan(&loan))
        .await
        .expect("the loan should be stored");

    let stored = repository
        .find(user_id, loan.id())
        .await
        .expect("the query should run")
        .expect("the loan should be there");

    assert_eq!(stored.amount().amount().to_storage_string(), "140.5000");
    assert_eq!(stored.amount().currency().as_str(), "USD");
    assert_eq!(stored.amount().base().to_storage_string(), "562345.7500");
    assert_eq!(stored.amount().base_currency().as_str(), "COP");
}

#[tokio::test]
async fn every_query_is_scoped_to_the_account_and_the_direction() {
    let Some(pool) = pool().await else { return };
    let user_id = account(&pool, true).await;
    let other_id = account(&pool, true).await;
    let repository = PostgresLoanRepository::new(pool.clone());

    for direction in [LoanDirection::Lent, LoanDirection::Borrowed] {
        let loan = a_loan(user_id, direction, amount("600000", "COP"));
        repository
            .create(&loan, &GeneratedMovement::for_loan(&loan))
            .await
            .expect("the loan should be stored");
    }
    let theirs = a_loan(other_id, LoanDirection::Lent, amount("100000", "COP"));
    repository
        .create(&theirs, &GeneratedMovement::for_loan(&theirs))
        .await
        .expect("the other loan should be stored");

    assert_eq!(
        repository.list(user_id, None).await.expect("list").len(),
        2,
        "the list only carries the loans of the account"
    );
    assert_eq!(
        repository
            .list(user_id, Some(LoanDirection::Borrowed))
            .await
            .expect("list")
            .len(),
        1
    );
    assert!(
        repository
            .find(user_id, theirs.id())
            .await
            .expect("the query should run")
            .is_none(),
        "a loan of another account behaves like one that does not exist"
    );
}
