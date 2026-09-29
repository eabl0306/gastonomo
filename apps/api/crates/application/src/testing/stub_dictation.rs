//! Dobles del dictado.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::dictation::{DictationContext, Interpretation};
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::dictation::{DictationAccess, DictationInterpreter};

/// Lista de aprobados en memoria.
#[derive(Debug, Default)]
pub struct FakeDictationAccess {
    allowed: Mutex<Vec<UserId>>,
}

impl FakeDictationAccess {
    /// Nadie aprobado.
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// Aprueba a una cuenta.
    pub fn allow(&self, user_id: UserId) {
        if let Ok(mut allowed) = self.allowed.lock() {
            allowed.push(user_id);
        }
    }
}

#[async_trait]
impl DictationAccess for FakeDictationAccess {
    async fn is_allowed(&self, user_id: UserId) -> Result<bool, AppError> {
        Ok(self
            .allowed
            .lock()
            .is_ok_and(|allowed| allowed.contains(&user_id)))
    }
}

/// Interprete que devuelve lo que le digan, y cuenta las veces que lo llaman.
pub struct StubDictationInterpreter {
    answer: Result<Interpretation, String>,
    calls: Mutex<u32>,
}

impl std::fmt::Debug for StubDictationInterpreter {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("StubDictationInterpreter")
            .finish_non_exhaustive()
    }
}

impl StubDictationInterpreter {
    /// Devuelve siempre esta interpretacion.
    #[must_use]
    pub const fn answering(interpretation: Interpretation) -> Self {
        Self {
            answer: Ok(interpretation),
            calls: Mutex::new(0),
        }
    }

    /// Falla siempre, como un modelo inalcanzable.
    #[must_use]
    pub const fn failing(detail: String) -> Self {
        Self {
            answer: Err(detail),
            calls: Mutex::new(0),
        }
    }

    /// Cuantas veces se pidio una interpretacion.
    #[must_use]
    pub fn calls(&self) -> u32 {
        self.calls
            .lock()
            .map_or_else(|poisoned| *poisoned.into_inner(), |calls| *calls)
    }
}

#[async_trait]
impl DictationInterpreter for StubDictationInterpreter {
    async fn interpret(
        &self,
        _text: &str,
        _context: &DictationContext<'_>,
    ) -> Result<Interpretation, AppError> {
        if let Ok(mut calls) = self.calls.lock() {
            *calls += 1;
        }
        self.answer.clone().map_err(AppError::dependency)
    }
}
