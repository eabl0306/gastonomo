//! Casos de uso del dictado.

mod parse_dictation;
#[cfg(test)]
mod tests;

pub use parse_dictation::{ParseDictation, ParseDictationCommand, ParseDictationPorts};
