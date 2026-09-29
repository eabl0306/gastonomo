//! El caso de uso del dictado.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::dictation::{DictatedEntry, Interpretation, InterpretedKind};
use gastonomo_domain::value_object::{Money, UserId};

use super::{ParseDictation, ParseDictationCommand, ParseDictationPorts};
use crate::error::AppError;
use crate::testing::{
    FakeDictationAccess, FixedClock, InMemoryCategoryRepository, InMemoryExchangeRateRepository,
    InMemoryUserSettingsRepository, StubDictationInterpreter, block_on,
};

fn an_interpretation() -> Interpretation {
    Interpretation {
        kind: InterpretedKind::Movement,
        incoming: Some(false),
        lent: None,
        amount: Some(Money::parse("45000").expect("importe valido")),
        currency: Some("COP".to_owned()),
        category: Some("Otros".to_owned()),
        title: Some("Almuerzo".to_owned()),
        person: None,
        occurred_on: None,
        note: None,
    }
}

struct Harness {
    user_id: UserId,
    access: Arc<FakeDictationAccess>,
    interpreter: Arc<StubDictationInterpreter>,
    ports: ParseDictationPorts,
}

impl Harness {
    fn with(interpreter: StubDictationInterpreter) -> Self {
        let user_id = UserId::generate();
        let access = Arc::new(FakeDictationAccess::new());
        let interpreter = Arc::new(interpreter);
        let ports = ParseDictationPorts {
            access: access.clone(),
            interpreter: interpreter.clone(),
            settings: Arc::new(InMemoryUserSettingsRepository::new()),
            categories: Arc::new(InMemoryCategoryRepository::new()),
            rates: Arc::new(InMemoryExchangeRateRepository::new()),
            clock: Arc::new(FixedClock::at_fixture()),
        };
        Self {
            user_id,
            access,
            interpreter,
            ports,
        }
    }

    fn working() -> Self {
        Self::with(StubDictationInterpreter::answering(an_interpretation()))
    }

    fn parse(&self, text: &str) -> Result<gastonomo_domain::dictation::DictationDraft, AppError> {
        block_on(ParseDictation::new(self.ports.clone()).execute(
            self.user_id,
            ParseDictationCommand {
                text: text.to_owned(),
                today: None,
            },
        ))
    }
}

/// La capacidad no se ofrece a quien no esta aprobado, y **no se dice que existe**:
/// un 403 confirmaria que esta ahi, que es justo lo que el resto de la API evita.
#[test]
fn without_approval_the_capability_does_not_exist() {
    let harness = Harness::working();

    let error = harness
        .parse("gaste 45 mil en almuerzo")
        .expect_err("deberia rechazar");

    assert!(matches!(error, AppError::NotFound { .. }), "fue {error:?}");
}

/// Y no se llama al modelo: el gasto de una capacidad que se paga no puede
/// dispararlo alguien que no la tiene concedida.
#[test]
fn without_approval_the_model_is_never_called() {
    let harness = Harness::working();

    let _ = harness.parse("gaste 45 mil en almuerzo");

    assert_eq!(harness.interpreter.calls(), 0);
}

#[test]
fn an_approved_account_gets_its_draft() {
    let harness = Harness::working();
    harness.access.allow(harness.user_id);

    let draft = harness
        .parse("gaste 45 mil en almuerzo")
        .expect("deberia interpretar");

    assert!(matches!(draft.entry, DictatedEntry::Movement(_)));
    assert!(draft.missing.is_empty());
    assert_eq!(harness.interpreter.calls(), 1);
}

/// El texto se comprueba **antes** de llamar al modelo. Un dictado vacio o
/// kilometrico no tiene por que costar dinero.
#[test]
fn an_empty_or_huge_text_never_reaches_the_model() {
    let harness = Harness::working();
    harness.access.allow(harness.user_id);

    let empty = harness.parse("   ").expect_err("deberia rechazar el vacio");
    assert!(matches!(empty, AppError::Validation(_)), "fue {empty:?}");

    let huge = harness
        .parse(&"a".repeat(501))
        .expect_err("deberia rechazar el largo");
    assert!(matches!(huge, AppError::Validation(_)), "fue {huge:?}");

    assert_eq!(harness.interpreter.calls(), 0);
}

/// Si el modelo no responde, el error sale como dependencia, o sea un 500 sin
/// detalles: lo que fallo es cosa nuestra y no del cuerpo que mando el cliente.
#[test]
fn a_broken_interpreter_is_a_dependency_failure() {
    let harness = Harness::with(StubDictationInterpreter::failing(
        "el modelo no responde".to_owned(),
    ));
    harness.access.allow(harness.user_id);

    let error = harness.parse("gaste 45 mil").expect_err("deberia fallar");

    assert!(
        matches!(error, AppError::Dependency { .. }),
        "fue {error:?}"
    );
}
