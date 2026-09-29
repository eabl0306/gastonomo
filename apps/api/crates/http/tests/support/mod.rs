//! Montaje del router con dobles de test, sin base de datos ni red.

#![allow(
    dead_code,
    reason = "each integration test binary compiles this module and uses only the part it needs"
)]
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_application::testing::{
    FakeDisplayCurrencyCatalog, FakeHealthProbe, FakeOpaqueTokenFactory, FakePasswordHasher,
    FakeTokenIssuer, FixedClock, InMemoryCategoryRepository, InMemoryChangeFeed,
    InMemoryExchangeRateRepository, InMemoryIdempotencyStore, InMemoryMovementRepository,
    InMemoryPasswordResetTokenRepository, InMemoryRefreshTokenRepository, InMemoryUserRepository,
    InMemoryUserSettingsRepository, RecordingMailer, RecordingUserProvisioning,
    StubMovementCategoryReader,
};
use gastonomo_application::use_case::category::{
    ArchiveCategory, CreateCategory, ListCategories, RestoreCategory, UpdateCategory,
};
use gastonomo_application::use_case::changes::GetChanges;
use gastonomo_application::use_case::currency::{GetExchangeRates, ListCurrencies};
use gastonomo_application::use_case::loan::testing::InMemoryLoanRepository;
use gastonomo_application::use_case::loan::{
    CreateLoan, GetLoan, ListLoans, LoanPorts, RegisterLoanPayment,
};
use gastonomo_application::use_case::movement::{CreateMovement, ListMovements};
use gastonomo_application::use_case::settings::{GetUserSettings, UpdateUserSettings};
use gastonomo_application::{
    ChangePassword, CheckLiveness, CheckReadiness, GetProfile, HealthProbe, LoginUser,
    LogoutSession, PasswordPorts, RefreshSession, RegisterUser, RequestPasswordReset,
    ResetPassword, SessionPorts,
};
use gastonomo_domain::movement::{CategoryId, MovementCategory};
use gastonomo_domain::{CategoryKind, ServiceVersion, UserId};
use gastonomo_http::build_service;
use gastonomo_http::router::{
    AuthDependencies, CategoryDependencies, ChangesDependencies, CurrencyDependencies,
    HealthDependencies, LoanDependencies, MovementDependencies, RouterDependencies,
    SettingsDependencies,
};

/// Version que anuncian las respuestas de salud.
pub const VERSION: ServiceVersion = ServiceVersion::new(0, 1, 0);

/// Todo lo que un test necesita mirar despues de una peticion.
pub mod dictation;

pub struct Harness {
    pub service: salvo::Service,
    pub categories: Arc<InMemoryCategoryRepository>,
    pub settings: Arc<InMemoryUserSettingsRepository>,
    pub movements: Arc<InMemoryMovementRepository>,
    pub loans: Arc<InMemoryLoanRepository>,
    pub movement_categories: Arc<StubMovementCategoryReader>,
    pub exchange_rates: Arc<InMemoryExchangeRateRepository>,
    pub changes: Arc<InMemoryChangeFeed>,
    pub idempotency: Arc<InMemoryIdempotencyStore>,
    pub mailer: Arc<RecordingMailer>,
    pub users: Arc<InMemoryUserRepository>,
    pub refresh_tokens: Arc<InMemoryRefreshTokenRepository>,
    pub clock: Arc<FixedClock>,
    pub dictation: dictation::DictationDoubles,
}

impl Harness {
    /// Monta el servicio con un probe de salud sano.
    #[must_use]
    pub fn new() -> Self {
        Self::with_probes(vec![Arc::new(FakeHealthProbe::healthy("database"))])
    }

    /// Monta el servicio con los probes de salud dados.
    #[must_use]
    pub fn with_probes(probes: Vec<Arc<dyn HealthProbe>>) -> Self {
        Self::build(probes, NO_RATE_LIMIT)
    }

    /// Monta el servicio con un limite de peticiones que si se puede alcanzar.
    ///
    /// El resto de la bateria usa [`NO_RATE_LIMIT`], tan alto que ningun test lo
    /// roza; sin esto no hay forma de provocar un 429.
    #[must_use]
    pub fn with_rate_limit(max_requests: u32) -> Self {
        Self::build(
            vec![Arc::new(FakeHealthProbe::healthy("database"))],
            max_requests,
        )
    }

    /// El cuerpo se reparte en una funcion por contexto: son siete y todas juntas
    /// pasaban del limite de lineas por funcion.
    #[must_use]
    fn build(probes: Vec<Arc<dyn HealthProbe>>, rate_limit_max_requests: u32) -> Self {
        let parts = Parts::new();
        let doubles = Doubles::new(&parts);
        let dictation_parts = dictation::dictation_dependencies(
            doubles.categories.clone(),
            doubles.settings.clone(),
            doubles.exchange_rates.clone(),
            parts.clock.clone(),
        );
        let service = build_service(RouterDependencies {
            health: health_dependencies(probes),
            auth: auth_dependencies(&parts, rate_limit_max_requests),
            movements: movement_dependencies(&doubles, &parts),
            loans: loan_dependencies(&doubles, &parts),
            categories: category_dependencies(&doubles, &parts),
            settings: settings_dependencies(&doubles, &parts),
            currencies: currency_dependencies(&doubles, &parts),
            changes: changes_dependencies(&doubles),
            dictation: dictation_parts.0,
        })
        .expect("service should build");

        Self {
            service,
            categories: doubles.categories,
            settings: doubles.settings,
            movements: doubles.movements,
            movement_categories: doubles.movement_categories,
            loans: doubles.loans,
            exchange_rates: doubles.exchange_rates,
            changes: doubles.changes,
            idempotency: doubles.idempotency,
            mailer: parts.mailer,
            users: parts.users,
            refresh_tokens: parts.refresh_tokens,
            clock: parts.clock,
            dictation: dictation_parts.1,
        }
    }
}

/// Repositorios en memoria de los contextos de dominio.
struct Doubles {
    categories: Arc<InMemoryCategoryRepository>,
    settings: Arc<InMemoryUserSettingsRepository>,
    movements: Arc<InMemoryMovementRepository>,
    movement_categories: Arc<StubMovementCategoryReader>,
    loans: Arc<InMemoryLoanRepository>,
    exchange_rates: Arc<InMemoryExchangeRateRepository>,
    idempotency: Arc<InMemoryIdempotencyStore>,
    changes: Arc<InMemoryChangeFeed>,
    loan_ports: LoanPorts,
}

impl Doubles {
    fn new(parts: &Parts) -> Self {
        let loans = Arc::new(InMemoryLoanRepository::new());
        Self {
            categories: Arc::new(InMemoryCategoryRepository::new()),
            settings: Arc::new(InMemoryUserSettingsRepository::new()),
            movements: Arc::new(InMemoryMovementRepository::new()),
            movement_categories: Arc::new(StubMovementCategoryReader::new()),
            loan_ports: LoanPorts {
                loans: loans.clone(),
                clock: parts.clock.clone(),
            },
            loans,
            exchange_rates: Arc::new(InMemoryExchangeRateRepository::new()),
            idempotency: Arc::new(InMemoryIdempotencyStore::new()),
            changes: Arc::new(InMemoryChangeFeed::new()),
        }
    }
}

fn changes_dependencies(doubles: &Doubles) -> ChangesDependencies {
    ChangesDependencies {
        read: Arc::new(GetChanges::new(doubles.changes.clone())),
    }
}

fn health_dependencies(probes: Vec<Arc<dyn HealthProbe>>) -> HealthDependencies {
    HealthDependencies {
        version: VERSION,
        liveness: CheckLiveness::new(VERSION),
        readiness: Arc::new(CheckReadiness::new(VERSION, probes)),
    }
}

fn movement_dependencies(doubles: &Doubles, parts: &Parts) -> MovementDependencies {
    MovementDependencies {
        list: Arc::new(ListMovements::new(doubles.movements.clone())),
        create: Arc::new(CreateMovement::new(
            doubles.movements.clone(),
            doubles.movement_categories.clone(),
            parts.clock.clone(),
        )),
        idempotency: doubles.idempotency.clone(),
    }
}

fn loan_dependencies(doubles: &Doubles, _parts: &Parts) -> LoanDependencies {
    let ports = doubles.loan_ports.clone();
    LoanDependencies {
        list: Arc::new(ListLoans::new(ports.clone())),
        detail: Arc::new(GetLoan::new(ports.clone())),
        create: Arc::new(CreateLoan::new(ports.clone())),
        register_payment: Arc::new(RegisterLoanPayment::new(ports)),
        idempotency: doubles.idempotency.clone(),
    }
}

fn category_dependencies(doubles: &Doubles, parts: &Parts) -> CategoryDependencies {
    let categories = doubles.categories.clone();
    CategoryDependencies {
        list: Arc::new(ListCategories::new(categories.clone())),
        create: Arc::new(CreateCategory::new(categories.clone(), parts.clock.clone())),
        update: Arc::new(UpdateCategory::new(categories.clone(), parts.clock.clone())),
        archive: Arc::new(ArchiveCategory::new(
            categories.clone(),
            parts.clock.clone(),
        )),
        restore: Arc::new(RestoreCategory::new(categories, parts.clock.clone())),
    }
}

fn settings_dependencies(doubles: &Doubles, parts: &Parts) -> SettingsDependencies {
    let settings = doubles.settings.clone();
    SettingsDependencies {
        read: Arc::new(GetUserSettings::new(settings.clone(), parts.clock.clone())),
        write: Arc::new(UpdateUserSettings::new(
            settings,
            Arc::new(FakeDisplayCurrencyCatalog::new()),
            parts.clock.clone(),
        )),
    }
}

fn currency_dependencies(doubles: &Doubles, parts: &Parts) -> CurrencyDependencies {
    let exchange_rates = doubles.exchange_rates.clone();
    CurrencyDependencies {
        list_currencies: Arc::new(ListCurrencies::new(exchange_rates.clone())),
        exchange_rates: Arc::new(GetExchangeRates::new(exchange_rates, parts.clock.clone())),
    }
}

impl Harness {
    /// Da de alta una categoria para la cuenta dada y devuelve su identificador.
    ///
    /// El identificador se genera aqui porque el contexto de categorias todavia no
    /// tiene endpoint de alta en este montaje de test.
    #[must_use]
    pub fn seed_category(&self, user_id: &str, name: &str, kind: CategoryKind) -> String {
        let owner = UserId::parse(user_id).expect("the response carries a valid uuid");
        let id = CategoryId::generate();
        let category = MovementCategory::new(id, name, kind, "restaurant", "#cf5f35")
            .expect("the fixture category is valid");
        self.movement_categories.add(owner, category);
        id.to_string()
    }
}

/// Dobles en memoria que comparten el router y el test.
struct Parts {
    users: Arc<InMemoryUserRepository>,
    refresh_tokens: Arc<InMemoryRefreshTokenRepository>,
    mailer: Arc<RecordingMailer>,
    clock: Arc<FixedClock>,
    session: SessionPorts,
    password: PasswordPorts,
}

impl Parts {
    fn new() -> Self {
        let users = Arc::new(InMemoryUserRepository::new());
        let refresh_tokens = Arc::new(InMemoryRefreshTokenRepository::new());
        let mailer = Arc::new(RecordingMailer::new());
        let clock = Arc::new(FixedClock::at_fixture());
        let opaque_tokens = Arc::new(FakeOpaqueTokenFactory::new());
        let password_hasher = Arc::new(FakePasswordHasher::new());

        let session = SessionPorts {
            users: users.clone(),
            refresh_tokens: refresh_tokens.clone(),
            password_hasher: password_hasher.clone(),
            access_tokens: Arc::new(FakeTokenIssuer::new()),
            opaque_tokens: opaque_tokens.clone(),
            clock: clock.clone(),
            refresh_ttl_days: 30,
        };
        let password = PasswordPorts {
            users: users.clone(),
            reset_tokens: Arc::new(InMemoryPasswordResetTokenRepository::new()),
            refresh_tokens: refresh_tokens.clone(),
            password_hasher,
            opaque_tokens,
            clock: clock.clone(),
            mailer: mailer.clone(),
            settings: Arc::new(InMemoryUserSettingsRepository::new()),
            reset_link_template: "https://gastonomo.test/reset?token={token}".to_owned(),
        };
        Self {
            users,
            refresh_tokens,
            mailer,
            clock,
            session,
            password,
        }
    }
}

/// Limite tan alto que ningun test lo alcanza sin querer.
pub const NO_RATE_LIMIT: u32 = 100;

fn auth_dependencies(parts: &Parts, rate_limit_max_requests: u32) -> AuthDependencies {
    AuthDependencies {
        register: Arc::new(RegisterUser::new(
            parts.session.clone(),
            Arc::new(RecordingUserProvisioning::new()),
        )),
        login: Arc::new(LoginUser::new(parts.session.clone())),
        refresh: Arc::new(RefreshSession::new(parts.session.clone())),
        logout: Arc::new(LogoutSession::new(
            parts.refresh_tokens.clone(),
            parts.session.opaque_tokens.clone(),
            parts.clock.clone(),
        )),
        forgot_password: Arc::new(RequestPasswordReset::new(parts.password.clone())),
        reset_password: Arc::new(ResetPassword::new(parts.password.clone())),
        change_password: Arc::new(ChangePassword::new(parts.password.clone())),
        profile: Arc::new(GetProfile::new(parts.users.clone())),
        access_tokens: parts.session.access_tokens.clone(),
        clock: parts.clock.clone(),
        rate_limit_max_requests,
        rate_limit_window_seconds: 60,
    }
}

/// Cuerpo de registro valido.
#[must_use]
pub fn registration() -> serde_json::Value {
    serde_json::json!({
        "name": "Camilo Rojas",
        "email": "camilo@correo.com",
        "password": "contrasena-larga",
        "acceptedTerms": true,
    })
}
