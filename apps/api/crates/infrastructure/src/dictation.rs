//! Adaptadores del dictado.

mod claude_interpreter;
mod claude_payload;
mod prompt;

pub use claude_interpreter::{ClaudeDictationInterpreter, InterpreterError};
