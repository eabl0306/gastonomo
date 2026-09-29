//! Token de recuperacion de contrasena: caduca en 30 minutos y es de un solo uso.

use chrono::{DateTime, Utc};
use uuid::Uuid;

use super::token_digest::TokenDigest;
use crate::value_object::UserId;

/// Validez del enlace, tal y como lo anuncia la pantalla de "Revisa tu correo".
pub const RESET_TOKEN_MINUTES: i64 = 30;

/// Espera entre dos emisiones seguidas para la misma cuenta.
///
/// Emitir un enlace borra los anteriores, asi que sin esta espera cualquiera que
/// supiera un correo podia pedir el enlace en bucle y borrar el que su dueno
/// acababa de recibir, dejandole sin poder recuperar la cuenta. El limite de
/// peticiones no lo impide: cuenta por IP, y el atacante no comparte la de la
/// victima.
///
/// Un minuto es el equilibrio: corta el bucle y deja reenviar de verdad cuando el
/// correo no llega, que es para lo que existe el boton de "reenviar".
pub const RESET_REISSUE_COOLDOWN_SECONDS: i64 = 60;

/// Identificador de un token de recuperacion.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct PasswordResetTokenId(Uuid);

impl PasswordResetTokenId {
    /// Genera un identificador nuevo.
    #[must_use]
    pub fn generate() -> Self {
        Self(Uuid::new_v4())
    }

    /// Envuelve un UUID leido de la base de datos.
    #[must_use]
    pub const fn from_uuid(value: Uuid) -> Self {
        Self(value)
    }

    /// UUID subyacente.
    #[must_use]
    pub const fn as_uuid(self) -> Uuid {
        self.0
    }
}

/// Datos con los que se emite un token de recuperacion.
#[derive(Debug, Clone)]
pub struct NewPasswordResetToken {
    /// Cuenta que pidio recuperar la contrasena.
    pub user_id: UserId,
    /// Huella del token que viaja en el enlace del correo.
    pub digest: TokenDigest,
    /// Instante de emision.
    pub created_at: DateTime<Utc>,
    /// Instante de caducidad.
    pub expires_at: DateTime<Utc>,
}

/// Token de recuperacion guardado en forma de huella.
#[derive(Debug, Clone)]
pub struct PasswordResetToken {
    id: PasswordResetTokenId,
    user_id: UserId,
    digest: TokenDigest,
    created_at: DateTime<Utc>,
    expires_at: DateTime<Utc>,
    used_at: Option<DateTime<Utc>>,
}

impl PasswordResetToken {
    /// Emite un token nuevo.
    #[must_use]
    pub fn issue(new_token: NewPasswordResetToken) -> Self {
        Self {
            id: PasswordResetTokenId::generate(),
            user_id: new_token.user_id,
            digest: new_token.digest,
            created_at: new_token.created_at,
            expires_at: new_token.expires_at,
            used_at: None,
        }
    }

    /// Reconstruye un token leido de la base de datos.
    #[must_use]
    pub fn rehydrate(
        id: PasswordResetTokenId,
        new_token: NewPasswordResetToken,
        used_at: Option<DateTime<Utc>>,
    ) -> Self {
        Self {
            id,
            user_id: new_token.user_id,
            digest: new_token.digest,
            created_at: new_token.created_at,
            expires_at: new_token.expires_at,
            used_at,
        }
    }

    /// Marca el token como usado. Solo surte efecto la primera vez.
    pub const fn mark_used(&mut self, now: DateTime<Utc>) {
        if self.used_at.is_none() {
            self.used_at = Some(now);
        }
    }

    /// Indica si el token sigue sirviendo para cambiar la contrasena.
    #[must_use]
    pub fn is_usable(&self, now: DateTime<Utc>) -> bool {
        self.used_at.is_none() && now < self.expires_at
    }

    /// Identificador del token.
    #[must_use]
    pub const fn id(&self) -> PasswordResetTokenId {
        self.id
    }

    /// Cuenta a la que pertenece.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.user_id
    }

    /// Huella guardada.
    #[must_use]
    pub const fn digest(&self) -> &TokenDigest {
        &self.digest
    }

    /// Instante de emision.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.created_at
    }

    /// Instante de caducidad.
    #[must_use]
    pub const fn expires_at(&self) -> DateTime<Utc> {
        self.expires_at
    }

    /// Instante en que se uso, si se uso.
    #[must_use]
    pub const fn used_at(&self) -> Option<DateTime<Utc>> {
        self.used_at
    }
}
