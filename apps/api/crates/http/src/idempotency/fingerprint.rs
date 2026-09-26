//! Huella del cuerpo de una peticion idempotente.
//!
//! Sirve para distinguir un reintento (misma clave, mismo cuerpo) de una
//! reutilizacion (misma clave, cuerpo distinto). Se guarda el resumen y no el
//! cuerpo: no hace falta poder reconstruirlo, solo compararlo, y asi no se duplican
//! importes y notas en una segunda tabla.

use gastonomo_application::error::AppError;
use salvo::prelude::Request;
use sha2::{Digest as _, Sha256};

/// Calcula la huella del cuerpo de la peticion.
///
/// Lee el cuerpo con `payload`, que Salvo cachea: el handler envuelto lo vuelve a
/// pedir despues para interpretarlo y recibe el mismo buffer.
///
/// # Errors
///
/// Devuelve [`AppError::Validation`] sobre `body` si el cuerpo no se puede leer.
pub async fn fingerprint_of(req: &mut Request) -> Result<String, AppError> {
    let payload = req
        .payload()
        .await
        .map_err(|error| AppError::validation("body", format!("Cuerpo invalido: {error}")))?;
    let mut hasher = Sha256::new();
    hasher.update(payload);
    Ok(hex(&hasher.finalize()))
}

fn hex(bytes: &[u8]) -> String {
    let mut text = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        // `write!` sobre un String devuelve un Result que clippy no deja descartar
        // sin mirarlo, y aqui no puede fallar: se compone a mano.
        text.push(nibble(byte >> 4));
        text.push(nibble(byte & 0x0f));
    }
    text
}

const fn nibble(value: u8) -> char {
    match value {
        0..=9 => (b'0' + value) as char,
        _ => (b'a' + value - 10) as char,
    }
}

#[cfg(test)]
mod tests {
    use super::{hex, nibble};

    #[test]
    fn the_hex_of_a_byte_is_two_lowercase_digits() {
        assert_eq!(hex(&[0x00, 0x0f, 0xa5, 0xff]), "000fa5ff");
        assert_eq!(nibble(0), '0');
        assert_eq!(nibble(15), 'f');
    }
}
