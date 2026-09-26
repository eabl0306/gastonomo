//! Adaptador driven: refresh tokens y enlaces de recuperacion.

use base64::Engine as _;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use gastonomo_application::port::{OpaqueToken, OpaqueTokenFactory};
use gastonomo_domain::TokenDigest;
use rand::{Rng as _, rng};
use sha2::{Digest as _, Sha256};

/// Bytes de entropia de cada token. 256 bits es lo que se recomienda para un
/// secreto que vive semanas en un dispositivo.
const TOKEN_BYTES: usize = 32;

/// Crea tokens opacos aleatorios y guarda solo su huella SHA-256.
#[derive(Debug, Clone, Default)]
pub struct RandomOpaqueTokenFactory;

impl RandomOpaqueTokenFactory {
    /// Crea la fabrica.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

impl OpaqueTokenFactory for RandomOpaqueTokenFactory {
    fn create(&self) -> OpaqueToken {
        let mut bytes = [0_u8; TOKEN_BYTES];
        rng().fill_bytes(&mut bytes);
        let value = URL_SAFE_NO_PAD.encode(bytes);
        let digest = self.digest(&value);
        OpaqueToken { value, digest }
    }

    fn digest(&self, value: &str) -> TokenDigest {
        let hashed = Sha256::digest(value.as_bytes());
        let hex = hashed
            .iter()
            .fold(String::with_capacity(64), |mut text, byte| {
                use std::fmt::Write as _;
                // El formato es de dos digitos por byte: siempre sale de 64 caracteres.
                let _ = write!(text, "{byte:02x}");
                text
            });
        match TokenDigest::parse(&hex) {
            Ok(digest) => digest,
            Err(error) => unreachable!("a sha256 in hex is always 64 chars: {error}"),
        }
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_application::port::OpaqueTokenFactory as _;

    use super::RandomOpaqueTokenFactory;

    #[test]
    fn each_token_is_different_and_carries_its_own_digest() {
        let factory = RandomOpaqueTokenFactory::new();
        let first = factory.create();
        let second = factory.create();

        assert_ne!(first.value, second.value);
        assert_ne!(first.digest.as_str(), second.digest.as_str());
        assert_eq!(factory.digest(&first.value), first.digest);
    }

    #[test]
    fn the_digest_is_a_sha256_in_lower_case_hex() {
        let digest = RandomOpaqueTokenFactory::new().digest("hola");
        assert_eq!(digest.as_str().len(), 64);
        assert!(
            digest
                .as_str()
                .chars()
                .all(|c| c.is_ascii_digit() || ('a'..='f').contains(&c))
        );
        assert_eq!(
            digest.as_str(),
            "b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79",
            "sha256(\"hola\")"
        );
    }

    #[test]
    fn the_value_is_url_safe_so_it_can_travel_in_a_link() {
        let token = RandomOpaqueTokenFactory::new().create();
        assert!(
            token
                .value
                .chars()
                .all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_')
        );
    }
}
