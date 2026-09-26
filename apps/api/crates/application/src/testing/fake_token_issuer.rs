//! Emisor de access tokens de test.

use chrono::{DateTime, Duration, Utc};
use gastonomo_domain::UserId;

use crate::error::AppError;
use crate::port::{AccessToken, TokenIssuer};

/// Validez del token falso, la misma que la del adaptador real.
const TTL_SECONDS: i64 = 15 * 60;

/// Doble del puerto [`TokenIssuer`] con tokens legibles.
///
/// El token es `access.{usuario}.{caducidad}`: sin firma, pero con la misma
/// semantica de caducidad, que es lo que interesa comprobar.
#[derive(Debug, Default)]
pub struct FakeTokenIssuer;

impl FakeTokenIssuer {
    /// Crea el doble.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

impl TokenIssuer for FakeTokenIssuer {
    fn issue(&self, user_id: UserId, now: DateTime<Utc>) -> Result<AccessToken, AppError> {
        let expires_at = now + Duration::seconds(TTL_SECONDS);
        Ok(AccessToken {
            value: format!("access.{user_id}.{}", expires_at.timestamp()),
            expires_in_seconds: TTL_SECONDS,
        })
    }

    fn verify(&self, token: &str, now: DateTime<Utc>) -> Result<UserId, AppError> {
        let mut parts = token.split('.');
        let (Some("access"), Some(raw_user), Some(raw_expiry), None) =
            (parts.next(), parts.next(), parts.next(), parts.next())
        else {
            return Err(AppError::unauthorized("malformed access token"));
        };
        let expires_at: i64 = raw_expiry
            .parse()
            .map_err(|_| AppError::unauthorized("malformed access token"))?;
        if now.timestamp() >= expires_at {
            return Err(AppError::unauthorized("the access token expired"));
        }
        UserId::parse(raw_user).map_err(|_| AppError::unauthorized("malformed access token"))
    }
}
