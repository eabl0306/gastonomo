//! Correo electronico validado.

use std::fmt;

use thiserror::Error;

/// Longitud maxima admitida, la del estandar.
const MAX_LENGTH: usize = 254;

/// Motivo por el que un correo no es valido.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum EmailError {
    /// El correo esta vacio.
    #[error("the email is empty")]
    Empty,
    /// El correo es mas largo de lo que admite el estandar.
    #[error("the email is longer than {MAX_LENGTH} characters")]
    TooLong,
    /// El correo no tiene la forma `local@dominio.tld`.
    #[error("{got:?} is not a well formed email address")]
    Malformed {
        /// Valor recibido.
        got: String,
    },
}

/// Correo electronico normalizado en minusculas.
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Email(String);

impl Email {
    /// Valida y normaliza un correo.
    ///
    /// La validacion es deliberadamente sencilla: una parte local no vacia, una
    /// sola arroba y un dominio con punto. Comprobar de verdad si un correo
    /// existe solo se puede hacer enviandole un mensaje, que es justo lo que hace
    /// el flujo de recuperacion.
    ///
    /// # Errors
    ///
    /// Devuelve [`EmailError`] si esta vacio, es demasiado largo o no tiene la
    /// forma esperada.
    pub fn parse(raw: &str) -> Result<Self, EmailError> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return Err(EmailError::Empty);
        }
        if trimmed.len() > MAX_LENGTH {
            return Err(EmailError::TooLong);
        }
        if !is_well_formed(trimmed) {
            return Err(EmailError::Malformed {
                got: trimmed.to_owned(),
            });
        }
        Ok(Self(trimmed.to_lowercase()))
    }

    /// Correo en minusculas.
    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

fn is_well_formed(candidate: &str) -> bool {
    if candidate.chars().any(char::is_whitespace) {
        return false;
    }
    let mut parts = candidate.split('@');
    let (Some(local), Some(domain), None) = (parts.next(), parts.next(), parts.next()) else {
        return false;
    };
    !local.is_empty()
        && domain.len() >= 3
        && domain.contains('.')
        && !domain.starts_with('.')
        && !domain.ends_with('.')
}

impl fmt::Display for Email {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}
