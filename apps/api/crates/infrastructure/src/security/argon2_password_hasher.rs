//! Adaptador driven: hash de contrasenas con Argon2id.

use argon2::password_hash::phc::PasswordHash as PhcHash;
use argon2::password_hash::{
    PasswordHasher as Argon2PasswordHasher, PasswordVerifier as Argon2PasswordVerifier,
};
use argon2::{Algorithm, Argon2, Params, Version};
use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::PasswordHasher;
use gastonomo_domain::{Password, PasswordHash};

/// Hash de contrasenas con Argon2id y los parametros recomendados por OWASP.
///
/// Son los que trae `Params::default()` en argon2 0.6: 19 MiB de memoria, dos
/// pasadas y un carril. Se escriben aqui de forma explicita para que quede claro
/// que son una decision y no un descuido, y para que cambiarlos se vea en el diff.
///
/// El calculo sale del hilo del runtime con `spawn_blocking`. Argon2id esta hecho
/// para tardar: con los parametros de OWASP son decenas de milisegundos de CPU pura,
/// y dejarlos en un hilo de trabajo de tokio para todas las peticiones que ese hilo
/// llevaba a medias. Un pico de logins simultaneos tumbaba la latencia del resto de
/// la API.
#[derive(Debug, Clone, Default)]
pub struct Argon2PasswordHashing;

impl Argon2PasswordHashing {
    /// Crea el adaptador.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }

    fn argon2() -> Argon2<'static> {
        Argon2::new(Algorithm::Argon2id, Version::V0x13, Params::DEFAULT.clone())
    }
}

impl Argon2PasswordHashing {
    fn digest(password: &Password) -> Result<PasswordHash, AppError> {
        let hashed = Self::argon2()
            .hash_password(password.expose())
            .map_err(|error| {
                AppError::dependency(format!("could not hash the password: {error}"))
            })?;
        PasswordHash::parse(&hashed.to_string())
            .map_err(|error| AppError::dependency(error.to_string()))
    }

    fn matches(password: &Password, hash: &PasswordHash) -> Result<bool, AppError> {
        let parsed = PhcHash::new(hash.as_str()).map_err(|error| {
            AppError::dependency(format!("the stored password hash is unreadable: {error}"))
        })?;
        Ok(Self::argon2()
            .verify_password(password.expose(), &parsed)
            .is_ok())
    }

    /// Ejecuta el calculo en el pool de hilos de bloqueo de tokio.
    ///
    /// Si la tarea no se puede unir es que el runtime se esta apagando o que el
    /// hilo entro en panico: las dos cosas son fallos de dependencia, no del
    /// cuerpo de la peticion.
    async fn off_the_runtime<T, F>(work: F) -> Result<T, AppError>
    where
        T: Send + 'static,
        F: FnOnce() -> Result<T, AppError> + Send + 'static,
    {
        tokio::task::spawn_blocking(work).await.map_err(|error| {
            AppError::dependency(format!("the password hashing task did not finish: {error}"))
        })?
    }
}

#[async_trait]
impl PasswordHasher for Argon2PasswordHashing {
    async fn hash(&self, password: &Password) -> Result<PasswordHash, AppError> {
        let password = password.clone();
        Self::off_the_runtime(move || Self::digest(&password)).await
    }

    async fn verify(&self, password: &Password, hash: &PasswordHash) -> Result<bool, AppError> {
        let password = password.clone();
        let hash = hash.clone();
        Self::off_the_runtime(move || Self::matches(&password, &hash)).await
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_application::port::PasswordHasher as _;
    use gastonomo_domain::{Password, PasswordHash};

    use super::Argon2PasswordHashing;

    fn password(raw: &str) -> Password {
        Password::parse(raw).expect("fixture password should be valid")
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn a_hash_verifies_against_its_own_password() {
        let hasher = Argon2PasswordHashing::new();
        let hash = hasher
            .hash(&password("contrasena-larga"))
            .await
            .expect("should hash");
        assert!(hash.as_str().starts_with("$argon2id$v=19$"));
        assert!(
            hasher
                .verify(&password("contrasena-larga"), &hash)
                .await
                .expect("should verify")
        );
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn another_password_does_not_verify() {
        let hasher = Argon2PasswordHashing::new();
        let hash = hasher
            .hash(&password("contrasena-larga"))
            .await
            .expect("should hash");
        assert!(
            !hasher
                .verify(&password("otra-contrasena"), &hash)
                .await
                .expect("should verify")
        );
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn the_same_password_hashes_differently_each_time() {
        let hasher = Argon2PasswordHashing::new();
        let first = hasher
            .hash(&password("contrasena-larga"))
            .await
            .expect("should hash");
        let second = hasher
            .hash(&password("contrasena-larga"))
            .await
            .expect("should hash");
        assert_ne!(
            first.as_str(),
            second.as_str(),
            "each hash carries its own random salt"
        );
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn a_corrupt_stored_hash_is_a_dependency_error() {
        let hasher = Argon2PasswordHashing::new();
        let broken = PasswordHash::parse("not-a-phc-string").expect("non empty");
        assert!(
            hasher
                .verify(&password("contrasena-larga"), &broken)
                .await
                .is_err()
        );
    }
}
