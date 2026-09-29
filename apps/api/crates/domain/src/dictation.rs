//! Lo que sale de dictar un registro en voz alta.
//!
//! Dictar produce un **borrador**, no un registro. El usuario lo revisa y confirma,
//! y solo entonces se crea el movimiento o el prestamo por los endpoints de
//! siempre, con su clave de idempotencia. Aqui no se escribe nada: el historico es
//! de solo anadir y no puede depender de que un modelo acierte.

mod draft;
mod interpretation;
#[cfg(test)]
mod tests;

pub use draft::{
    DictatedEntry, DictatedLoan, DictatedMovement, DictationDraft, DraftCategory, MissingField,
};
pub use interpretation::{AvailableCategory, DictationContext, Interpretation, InterpretedKind};
