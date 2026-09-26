//! Huella de un token opaco.

use std::fmt;

use thiserror::Error;

/// La huella recibida no tiene forma de resumen hexadecimal.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("a token digest must be a hex string of 64 characters")]
pub struct TokenDigestError;

/// Resumen SHA-256 en hexadecimal de un token opaco.
///
/// En la base de datos nunca se guarda el token, solo su huella: si alguien lee
/// la tabla no se lleva ninguna sesion. Buscar por token consiste en volver a
/// calcular la huella y comparar.
#[derive(Clone, PartialEq, Eq, Hash)]
pub struct TokenDigest(String);

impl TokenDigest {
    /// Envuelve una huella ya calculada.
    ///
    /// # Errors
    ///
    /// Devuelve [`TokenDigestError`] si no son 64 caracteres hexadecimales.
    pub fn parse(raw: &str) -> Result<Self, TokenDigestError> {
        let trimmed = raw.trim();
        if trimmed.len() != 64 || !trimmed.chars().all(|c| c.is_ascii_hexdigit()) {
            return Err(TokenDigestError);
        }
        Ok(Self(trimmed.to_ascii_lowercase()))
    }

    /// Huella en hexadecimal minuscula.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Debug for TokenDigest {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("TokenDigest(redacted)")
    }
}
