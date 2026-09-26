//! Puerto de creacion de tokens opacos.

use gastonomo_domain::TokenDigest;

/// Token opaco recien creado: el valor que se entrega y la huella que se guarda.
#[derive(Debug, Clone)]
pub struct OpaqueToken {
    /// Valor que viaja al cliente o al correo. No se guarda nunca.
    pub value: String,
    /// Huella SHA-256 que si se guarda.
    pub digest: TokenDigest,
}

/// Crea tokens opacos aleatorios y calcula su huella.
///
/// Lo usan los refresh tokens y los enlaces de recuperacion. La aleatoriedad es
/// del sistema operativo, asi que vive en un adaptador y no en el nucleo.
pub trait OpaqueTokenFactory: Send + Sync {
    /// Crea un token nuevo.
    fn create(&self) -> OpaqueToken;

    /// Calcula la huella de un token que llega en una peticion.
    fn digest(&self, value: &str) -> TokenDigest;
}
