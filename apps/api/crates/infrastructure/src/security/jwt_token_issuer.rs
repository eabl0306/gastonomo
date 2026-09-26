//! Adaptador driven: access tokens JWT firmados con HS256.

use chrono::{DateTime, Duration, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::{AccessToken, TokenIssuer};
use gastonomo_domain::UserId;
use jsonwebtoken::{Algorithm, DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::{Deserialize, Serialize};

use super::hs256;

/// Contenido del access token.
///
/// Se queda en lo minimo: quien es y hasta cuando vale. Nada de nombre ni correo,
/// porque un token no es un sitio para guardar datos que cambian.
#[derive(Debug, Clone, Serialize, Deserialize)]
struct Claims {
    /// Sujeto: el identificador de la cuenta.
    sub: String,
    /// Caducidad, en segundos desde epoch.
    exp: i64,
    /// Emision, en segundos desde epoch.
    iat: i64,
}

/// Emite y valida access tokens JWT.
#[derive(Clone)]
pub struct JwtTokenIssuer {
    encoding: EncodingKey,
    decoding: DecodingKey,
    ttl_minutes: i64,
}

impl std::fmt::Debug for JwtTokenIssuer {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("JwtTokenIssuer")
            .field("ttl_minutes", &self.ttl_minutes)
            .finish_non_exhaustive()
    }
}

impl JwtTokenIssuer {
    /// Crea el emisor con el secreto y la validez configurados.
    ///
    /// Instala el proveedor criptografico HS256 la primera vez que se llama.
    #[must_use]
    pub fn new(secret: &str, ttl_minutes: i64) -> Self {
        hs256::install();
        Self {
            encoding: EncodingKey::from_secret(secret.as_bytes()),
            decoding: DecodingKey::from_secret(secret.as_bytes()),
            ttl_minutes,
        }
    }
}

impl TokenIssuer for JwtTokenIssuer {
    fn issue(&self, user_id: UserId, now: DateTime<Utc>) -> Result<AccessToken, AppError> {
        let expires_at = now + Duration::minutes(self.ttl_minutes);
        let claims = Claims {
            sub: user_id.to_string(),
            exp: expires_at.timestamp(),
            iat: now.timestamp(),
        };
        let value = encode(&Header::new(Algorithm::HS256), &claims, &self.encoding)
            .map_err(|error| AppError::dependency(format!("could not sign the token: {error}")))?;
        Ok(AccessToken {
            value,
            expires_in_seconds: self.ttl_minutes * 60,
        })
    }

    fn verify(&self, token: &str, now: DateTime<Utc>) -> Result<UserId, AppError> {
        let mut validation = Validation::new(Algorithm::HS256);
        validation.validate_exp = false;
        validation.required_spec_claims.clear();
        let data = decode::<Claims>(token, &self.decoding, &validation)
            .map_err(|error| AppError::unauthorized(format!("invalid access token: {error}")))?;
        if now.timestamp() >= data.claims.exp {
            return Err(AppError::unauthorized("the access token expired"));
        }
        UserId::parse(&data.claims.sub)
            .map_err(|_| AppError::unauthorized("the access token carries no valid subject"))
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use chrono::{DateTime, Duration, Utc};
    use gastonomo_application::error::AppError;
    use gastonomo_application::port::TokenIssuer as _;
    use gastonomo_domain::UserId;

    use super::JwtTokenIssuer;

    fn now() -> DateTime<Utc> {
        DateTime::parse_from_rfc3339("2026-09-22T10:00:00Z")
            .expect("literal is valid")
            .with_timezone(&Utc)
    }

    fn issuer() -> JwtTokenIssuer {
        JwtTokenIssuer::new("a-secret-long-enough-for-hs256", 15)
    }

    #[test]
    fn a_token_verifies_back_to_the_user_that_asked_for_it() {
        let issuer = issuer();
        let user_id = UserId::generate();
        let token = issuer.issue(user_id, now()).expect("should sign");

        assert_eq!(token.expires_in_seconds, 900);
        assert_eq!(
            issuer
                .verify(&token.value, now() + Duration::minutes(14))
                .expect("should verify"),
            user_id
        );
    }

    #[test]
    fn an_expired_token_is_rejected() {
        let issuer = issuer();
        let token = issuer
            .issue(UserId::generate(), now())
            .expect("should sign");
        let error = issuer
            .verify(&token.value, now() + Duration::minutes(16))
            .expect_err("should be expired");
        assert!(matches!(error, AppError::Unauthorized { .. }));
    }

    #[test]
    fn a_token_signed_with_another_secret_is_rejected() {
        let token = JwtTokenIssuer::new("another-secret-entirely", 15)
            .issue(UserId::generate(), now())
            .expect("should sign");
        let error = issuer()
            .verify(&token.value, now())
            .expect_err("the signature should not match");
        assert!(matches!(error, AppError::Unauthorized { .. }));
    }

    #[test]
    fn garbage_is_rejected_without_panicking() {
        let issuer = issuer();
        for candidate in ["", "not-a-jwt", "a.b.c"] {
            assert!(issuer.verify(candidate, now()).is_err());
        }
    }
}
