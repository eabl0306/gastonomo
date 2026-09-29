//! Dobles del dictado para el harness de los tests de endpoint.
//!
//! Va en su propio fichero porque `mod.rs` ya roza el limite de lineas del
//! repositorio, y el comentario de alli ya avisa de que el cuerpo se reparte por
//! contexto.

use std::sync::Arc;

use gastonomo_application::port::Clock;
use gastonomo_application::testing::{
    FakeDictationAccess, InMemoryCategoryRepository, InMemoryExchangeRateRepository,
    InMemoryUserSettingsRepository, StubDictationInterpreter,
};
use gastonomo_application::use_case::dictation::{ParseDictation, ParseDictationPorts};
use gastonomo_domain::dictation::{Interpretation, InterpretedKind};
use gastonomo_domain::value_object::Money;
use gastonomo_http::DictationDependencies;

/// Lo que el interprete devuelve en los tests: un gasto de 45.000 en Otros.
#[must_use]
pub fn an_interpretation() -> Interpretation {
    Interpretation {
        kind: InterpretedKind::Movement,
        incoming: Some(false),
        lent: None,
        amount: Money::parse("45000").ok(),
        currency: Some("COP".to_owned()),
        category: Some("Otros".to_owned()),
        title: Some("Almuerzo".to_owned()),
        person: None,
        occurred_on: None,
        note: None,
    }
}

/// Los dobles del dictado, para que el test pueda aprobar cuentas.
pub struct DictationDoubles {
    /// Lista de aprobados.
    pub access: Arc<FakeDictationAccess>,
    /// Interprete, que cuenta las veces que lo llaman.
    pub interpreter: Arc<StubDictationInterpreter>,
}

/// Monta el caso de uso del dictado con dobles.
#[must_use]
pub fn dictation_dependencies(
    categories: Arc<InMemoryCategoryRepository>,
    settings: Arc<InMemoryUserSettingsRepository>,
    rates: Arc<InMemoryExchangeRateRepository>,
    clock: Arc<dyn Clock>,
) -> (DictationDependencies, DictationDoubles) {
    let access = Arc::new(FakeDictationAccess::new());
    let interpreter = Arc::new(StubDictationInterpreter::answering(an_interpretation()));
    let dependencies = DictationDependencies {
        parse: Arc::new(ParseDictation::new(ParseDictationPorts {
            access: access.clone(),
            interpreter: interpreter.clone(),
            settings,
            categories,
            rates,
            clock,
        })),
    };
    (
        dependencies,
        DictationDoubles {
            access,
            interpreter,
        },
    )
}
