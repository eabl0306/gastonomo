//! Contrasena en claro y su hash, como tipos distintos.

use std::fmt;

use thiserror::Error;

/// Longitud minima que exige el diseno.
pub const MIN_PASSWORD_LENGTH: usize = 8;

/// Longitud maxima admitida, para no alimentar al hash con entradas enormes.
pub const MAX_PASSWORD_LENGTH: usize = 128;

/// Motivo por el que una contrasena no es valida.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum PasswordError {
    /// La contrasena tiene menos de [`MIN_PASSWORD_LENGTH`] caracteres.
    #[error("the password needs at least {MIN_PASSWORD_LENGTH} characters")]
    TooShort,
    /// La contrasena pasa de [`MAX_PASSWORD_LENGTH`] caracteres.
    #[error("the password is longer than {MAX_PASSWORD_LENGTH} characters")]
    TooLong,
}

/// Contrasena en claro, solo viva en memoria durante una peticion.
///
/// `Debug` no imprime el valor: una traza con la contrasena del usuario es una
/// fuga, y aqui se escriben trazas en cada peticion.
#[derive(Clone, PartialEq, Eq)]
pub struct Password(String);

impl Password {
    /// Valida la contrasena en claro.
    ///
    /// El medidor de fuerza del diseno es puramente visual y vive en el cliente:
    /// la API solo exige el minimo de caracteres.
    ///
    /// # Errors
    ///
    /// Devuelve [`PasswordError`] si es demasiado corta o demasiado larga.
    pub fn parse(raw: &str) -> Result<Self, PasswordError> {
        let length = raw.chars().count();
        if length < MIN_PASSWORD_LENGTH {
            return Err(PasswordError::TooShort);
        }
        if length > MAX_PASSWORD_LENGTH {
            return Err(PasswordError::TooLong);
        }
        Ok(Self(raw.to_owned()))
    }

    /// Bytes que se le pasan al algoritmo de hash.
    #[must_use]
    pub const fn expose(&self) -> &[u8] {
        self.0.as_bytes()
    }
}

impl fmt::Debug for Password {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("Password(redacted)")
    }
}

/// Hash de contrasena en formato PHC, tal y como lo guarda la base de datos.
#[derive(Clone, PartialEq, Eq)]
pub struct PasswordHash(String);

impl PasswordHash {
    /// Envuelve un hash ya calculado.
    ///
    /// # Errors
    ///
    /// Devuelve [`PasswordError::TooShort`] si el hash viene vacio, que solo
    /// puede pasar con una fila corrupta.
    pub fn parse(raw: &str) -> Result<Self, PasswordError> {
        if raw.trim().is_empty() {
            return Err(PasswordError::TooShort);
        }
        Ok(Self(raw.to_owned()))
    }

    /// Hash en formato PHC.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Debug for PasswordHash {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("PasswordHash(redacted)")
    }
}
