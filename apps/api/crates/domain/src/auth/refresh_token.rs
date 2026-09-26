//! Refresh token con rotacion y familia.

use chrono::{DateTime, Utc};
use uuid::Uuid;

use super::token_digest::TokenDigest;
use crate::value_object::UserId;

/// Identificador de un refresh token.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct RefreshTokenId(Uuid);

impl RefreshTokenId {
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

/// Datos con los que se emite un refresh token.
#[derive(Debug, Clone)]
pub struct NewRefreshToken {
    /// Dueno de la sesion.
    pub user_id: UserId,
    /// Huella del token entregado al cliente.
    pub digest: TokenDigest,
    /// Instante de emision.
    pub created_at: DateTime<Utc>,
    /// Instante de caducidad.
    pub expires_at: DateTime<Utc>,
    /// `User-Agent` de quien lo pidio, para que el usuario reconozca la sesion.
    pub user_agent: Option<String>,
}

/// Refresh token guardado, siempre en forma de huella.
#[derive(Debug, Clone)]
pub struct RefreshToken {
    id: RefreshTokenId,
    user_id: UserId,
    digest: TokenDigest,
    created_at: DateTime<Utc>,
    expires_at: DateTime<Utc>,
    revoked_at: Option<DateTime<Utc>>,
    replaced_by: Option<RefreshTokenId>,
    user_agent: Option<String>,
}

impl RefreshToken {
    /// Emite un token nuevo.
    #[must_use]
    pub fn issue(new_token: NewRefreshToken) -> Self {
        Self {
            id: RefreshTokenId::generate(),
            user_id: new_token.user_id,
            digest: new_token.digest,
            created_at: new_token.created_at,
            expires_at: new_token.expires_at,
            revoked_at: None,
            replaced_by: None,
            user_agent: new_token.user_agent,
        }
    }

    /// Reconstruye un token leido de la base de datos.
    #[must_use]
    pub fn rehydrate(id: RefreshTokenId, stored: StoredRefreshToken) -> Self {
        Self {
            id,
            user_id: stored.token.user_id,
            digest: stored.token.digest,
            created_at: stored.token.created_at,
            expires_at: stored.token.expires_at,
            revoked_at: stored.revoked_at,
            replaced_by: stored.replaced_by,
            user_agent: stored.token.user_agent,
        }
    }

    /// Revoca el token. Si ya estaba revocado no cambia la marca original.
    pub const fn revoke(&mut self, now: DateTime<Utc>) {
        if self.revoked_at.is_none() {
            self.revoked_at = Some(now);
        }
    }

    /// Marca el token como rotado y lo apunta a su sustituto.
    pub const fn mark_rotated(&mut self, replacement: RefreshTokenId, now: DateTime<Utc>) {
        self.replaced_by = Some(replacement);
        self.revoke(now);
    }

    /// Indica si el token ya se revoco, por logout o por rotacion.
    #[must_use]
    pub const fn is_revoked(&self) -> bool {
        self.revoked_at.is_some()
    }

    /// Indica si el token ha caducado en el instante dado.
    #[must_use]
    pub fn is_expired(&self, now: DateTime<Utc>) -> bool {
        now >= self.expires_at
    }

    /// Indica si el token sirve para refrescar la sesion.
    #[must_use]
    pub fn is_usable(&self, now: DateTime<Utc>) -> bool {
        !self.is_revoked() && !self.is_expired(now)
    }

    /// Indica que el token ya se uso para rotar y por tanto su presentacion
    /// vuelve a ser sospechosa de robo.
    #[must_use]
    pub const fn was_rotated(&self) -> bool {
        self.replaced_by.is_some()
    }

    /// Identificador del token.
    #[must_use]
    pub const fn id(&self) -> RefreshTokenId {
        self.id
    }

    /// Dueno de la sesion.
    #[must_use]
    pub const fn user_id(&self) -> UserId {
        self.user_id
    }

    /// Huella guardada.
    #[must_use]
    pub const fn digest(&self) -> &TokenDigest {
        &self.digest
    }

    /// Instante de caducidad.
    #[must_use]
    pub const fn expires_at(&self) -> DateTime<Utc> {
        self.expires_at
    }

    /// Instante de emision.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.created_at
    }

    /// Instante de revocacion, si la hubo.
    #[must_use]
    pub const fn revoked_at(&self) -> Option<DateTime<Utc>> {
        self.revoked_at
    }

    /// Token que sustituyo a este, si se roto.
    #[must_use]
    pub const fn replaced_by(&self) -> Option<RefreshTokenId> {
        self.replaced_by
    }

    /// `User-Agent` de la sesion.
    #[must_use]
    pub fn user_agent(&self) -> Option<&str> {
        self.user_agent.as_deref()
    }
}

/// Campos que solo existen para un token ya guardado.
#[derive(Debug, Clone)]
pub struct StoredRefreshToken {
    /// Datos de emision.
    pub token: NewRefreshToken,
    /// Instante de revocacion.
    pub revoked_at: Option<DateTime<Utc>>,
    /// Token que lo sustituyo.
    pub replaced_by: Option<RefreshTokenId>,
}
