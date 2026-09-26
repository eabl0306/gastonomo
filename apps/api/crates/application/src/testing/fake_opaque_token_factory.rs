//! Fabrica de tokens opacos deterministas.

use std::sync::atomic::{AtomicU64, Ordering};

use gastonomo_domain::TokenDigest;

use crate::port::{OpaqueToken, OpaqueTokenFactory};

/// Doble del puerto [`OpaqueTokenFactory`] con valores predecibles.
///
/// Los tokens van numerados, asi un test puede afirmar que el segundo refresco
/// devolvio un token distinto del primero. La huella es un hash FNV-1a repetido
/// hasta los 64 caracteres que exige [`TokenDigest`]: no es criptografico, pero es
/// funcion pura del valor, que es lo unico que el nucleo necesita.
#[derive(Debug, Default)]
pub struct FakeOpaqueTokenFactory {
    issued: AtomicU64,
}

impl FakeOpaqueTokenFactory {
    /// Crea la fabrica.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            issued: AtomicU64::new(0),
        }
    }

    /// Numero de tokens creados.
    #[must_use]
    pub fn issued(&self) -> u64 {
        self.issued.load(Ordering::Relaxed)
    }
}

fn fnv1a(value: &str) -> u64 {
    let mut hash: u64 = 0xcbf2_9ce4_8422_2325;
    for byte in value.as_bytes() {
        hash ^= u64::from(*byte);
        hash = hash.wrapping_mul(0x0000_0100_0000_01b3);
    }
    hash
}

impl OpaqueTokenFactory for FakeOpaqueTokenFactory {
    fn create(&self) -> OpaqueToken {
        let number = self.issued.fetch_add(1, Ordering::Relaxed) + 1;
        let value = format!("opaque-token-{number}");
        let digest = self.digest(&value);
        OpaqueToken { value, digest }
    }

    fn digest(&self, value: &str) -> TokenDigest {
        let hex = format!("{:016x}", fnv1a(value)).repeat(4);
        match TokenDigest::parse(&hex) {
            Ok(digest) => digest,
            Err(error) => unreachable!("the fake digest is always 64 hex chars: {error}"),
        }
    }
}
