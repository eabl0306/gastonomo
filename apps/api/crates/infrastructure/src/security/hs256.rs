//! Proveedor criptografico HS256 para `jsonwebtoken`.
//!
//! `jsonwebtoken` 11 desacopla la criptografia detras de un `CryptoProvider`. El
//! proveedor que trae de serie (`rust_crypto`) arrastra el crate `rsa`, que tiene
//! un aviso de seguridad sin parche (RUSTSEC-2023-0071, ataque Marvin) y haria
//! fallar `cargo deny`. Como aqui solo se firma con HS256, montamos el proveedor
//! con `hmac` y `sha2` y nos ahorramos RSA, las curvas elipticas y el aviso.

use hmac::{Hmac, KeyInit as _, Mac as _};
use jsonwebtoken::crypto::{CryptoProvider, JwtSigner, JwtVerifier, KeyUtils};
use jsonwebtoken::errors::{Error, ErrorKind, Result};
use jsonwebtoken::signature::{Signer, Verifier};
use jsonwebtoken::{Algorithm, AlgorithmFamily, DecodingKey, EncodingKey};
use sha2::Sha256;

/// HMAC-SHA256 sobre el secreto configurado.
type HmacSha256 = Hmac<Sha256>;

/// Firma con HS256.
struct Hs256Signer {
    key: Vec<u8>,
}

/// Comprueba firmas HS256.
struct Hs256Verifier {
    key: Vec<u8>,
}

fn mac(key: &[u8], message: &[u8]) -> std::result::Result<Vec<u8>, signature_error::Wrapped> {
    let mut mac = HmacSha256::new_from_slice(key).map_err(|_| signature_error::Wrapped)?;
    mac.update(message);
    Ok(mac.finalize().into_bytes().to_vec())
}

/// Envoltorio para no depender del crate `signature` de forma directa.
mod signature_error {
    /// Marca de fallo interno del MAC.
    #[derive(Debug)]
    pub struct Wrapped;
}

impl From<signature_error::Wrapped> for jsonwebtoken::signature::Error {
    fn from(_: signature_error::Wrapped) -> Self {
        Self::new()
    }
}

impl Signer<Vec<u8>> for Hs256Signer {
    fn try_sign(
        &self,
        message: &[u8],
    ) -> std::result::Result<Vec<u8>, jsonwebtoken::signature::Error> {
        Ok(mac(&self.key, message)?)
    }
}

impl JwtSigner for Hs256Signer {
    fn algorithm(&self) -> Algorithm {
        Algorithm::HS256
    }
}

impl Verifier<Vec<u8>> for Hs256Verifier {
    fn verify(
        &self,
        message: &[u8],
        signature: &Vec<u8>,
    ) -> std::result::Result<(), jsonwebtoken::signature::Error> {
        let expected = mac(&self.key, message)?;
        // Comparacion en tiempo constante: `Mac::verify_slice` la hace por
        // nosotros, pero aqui ya tenemos el valor calculado.
        if constant_time_eq(&expected, signature) {
            Ok(())
        } else {
            Err(jsonwebtoken::signature::Error::new())
        }
    }
}

impl JwtVerifier for Hs256Verifier {
    fn algorithm(&self) -> Algorithm {
        Algorithm::HS256
    }
}

fn constant_time_eq(left: &[u8], right: &[u8]) -> bool {
    if left.len() != right.len() {
        return false;
    }
    left.iter()
        .zip(right)
        .fold(0_u8, |accumulator, (a, b)| accumulator | (a ^ b))
        == 0
}

#[allow(
    clippy::trivially_copy_pass_by_ref,
    reason = "the signature is fixed by CryptoProvider::signer_factory"
)]
fn signer(algorithm: &Algorithm, key: &EncodingKey) -> Result<Box<dyn JwtSigner>> {
    if *algorithm != Algorithm::HS256 || key.family() != AlgorithmFamily::Hmac {
        return Err(Error::from(ErrorKind::InvalidAlgorithm));
    }
    Ok(Box::new(Hs256Signer {
        key: key.as_bytes().to_vec(),
    }))
}

#[allow(
    clippy::trivially_copy_pass_by_ref,
    reason = "the signature is fixed by CryptoProvider::verifier_factory"
)]
fn verifier(algorithm: &Algorithm, key: &DecodingKey) -> Result<Box<dyn JwtVerifier>> {
    if *algorithm != Algorithm::HS256 || key.family() != AlgorithmFamily::Hmac {
        return Err(Error::from(ErrorKind::InvalidAlgorithm));
    }
    Ok(Box::new(Hs256Verifier {
        key: key.try_get_as_bytes()?.to_vec(),
    }))
}

/// Proveedor que solo sabe de HS256.
static PROVIDER: CryptoProvider = CryptoProvider {
    signer_factory: signer,
    verifier_factory: verifier,
    // No se publican JWKs: este servicio firma con un secreto compartido.
    key_utils: KeyUtils::new_unimplemented(),
};

/// Instala el proveedor HS256 como el del proceso.
///
/// Es idempotente: si ya estaba instalado, no hace nada. Hay que llamarla antes de
/// emitir o validar el primer token.
pub fn install() {
    // El error solo dice que ya habia uno instalado, que es exactamente lo que
    // esta funcion promete tolerar.
    let _ = PROVIDER.install_default();
}
