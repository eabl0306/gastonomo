//! Caso de uso: pedir el enlace de recuperacion.

use std::fmt;

use chrono::Duration;
use gastonomo_domain::{
    Email, NewPasswordResetToken, PasswordResetToken, RESET_REISSUE_COOLDOWN_SECONDS,
    RESET_TOKEN_MINUTES, User, UserId,
};

use gastonomo_domain::settings::Language;

use super::password_ports::PasswordPorts;
use super::reset_mail::reset_mail;
use crate::error::AppError;

/// Correo para el que se pide el enlace.
#[derive(Debug, Clone)]
pub struct RequestPasswordResetCommand {
    /// Correo escrito en la pantalla de recuperar.
    pub email: String,
}

/// Manda el enlace de recuperacion, si la cuenta existe.
#[derive(Clone)]
pub struct RequestPasswordReset {
    ports: PasswordPorts,
}

impl fmt::Debug for RequestPasswordReset {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RequestPasswordReset")
            .finish_non_exhaustive()
    }
}

impl RequestPasswordReset {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: PasswordPorts) -> Self {
        Self { ports }
    }

    /// Pide el enlace.
    ///
    /// Termina bien aunque el correo no tenga cuenta, y aunque no sea un correo
    /// valido. El endpoint responde 202 siempre: si respondiera distinto seria un
    /// oraculo para saber quien esta registrado. La pantalla de "enviado" muestra
    /// el correo que escribio el usuario, no uno que confirme la API.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos o el correo.
    pub async fn execute(&self, command: RequestPasswordResetCommand) -> Result<(), AppError> {
        let Ok(email) = Email::parse(&command.email) else {
            return Ok(());
        };
        let Some(user) = self.ports.users.find_by_email(&email).await? else {
            return Ok(());
        };
        if self.issued_a_moment_ago(user.id()).await? {
            return Ok(());
        }
        let link = self.store_token(&user).await?;
        self.send_mail(&user, &link).await
    }

    /// La cuenta ya tiene un enlace vivo recien mandado.
    ///
    /// Entonces esta peticion no hace nada: ni emite, ni borra, ni manda correo.
    /// Sin esto, emitir borra el anterior, y cualquiera que supiera un correo podia
    /// pedir el enlace en bucle y dejar a su dueno sin poder recuperar la cuenta.
    /// El limite de peticiones no lo impide, porque cuenta por IP y el atacante no
    /// comparte la de la victima.
    ///
    /// Sigue respondiendo 202, igual que cuando la cuenta no existe, asi que no
    /// sirve para averiguar si alguien acaba de pedir el enlace.
    async fn issued_a_moment_ago(&self, user: UserId) -> Result<bool, AppError> {
        let now = self.ports.clock.now();
        let latest = self
            .ports
            .reset_tokens
            .latest_live_for_user(user, now)
            .await?;
        Ok(latest.is_some_and(|token| {
            now - token.created_at() < Duration::seconds(RESET_REISSUE_COOLDOWN_SECONDS)
        }))
    }

    /// Emite el enlace, y de paso mata los anteriores de esa cuenta.
    ///
    /// El orden importa: primero se borran los viejos y despues se inserta, para
    /// que un fallo a mitad no deje al usuario sin ningun enlace vivo y con el
    /// correo ya mandado.
    async fn store_token(&self, user: &User) -> Result<String, AppError> {
        self.ports
            .reset_tokens
            .discard_all_for_user(user.id())
            .await?;
        let now = self.ports.clock.now();
        let opaque = self.ports.opaque_tokens.create();
        let token = PasswordResetToken::issue(NewPasswordResetToken {
            user_id: user.id(),
            digest: opaque.digest,
            created_at: now,
            expires_at: now + Duration::minutes(RESET_TOKEN_MINUTES),
        });
        self.ports.reset_tokens.insert(&token).await?;
        Ok(self.ports.reset_link(&opaque.value))
    }

    async fn send_mail(&self, user: &User, link: &str) -> Result<(), AppError> {
        let message = reset_mail(
            user.email().clone(),
            user.name(),
            link,
            self.language_of(user).await,
        );
        self.ports.mailer.send(&message).await
    }

    /// El idioma que la cuenta tiene elegido.
    ///
    /// Un fallo leyendo los ajustes **no impide mandar el correo**: se cae al
    /// castellano, que es el idioma por defecto de una cuenta nueva. Perder el
    /// enlace de recuperacion porque la consulta de ajustes tropezo seria mucho
    /// peor que mandarlo en el idioma equivocado.
    async fn language_of(&self, user: &User) -> Language {
        self.ports
            .settings
            .find(user.id())
            .await
            .ok()
            .flatten()
            .map(|settings| settings.language())
            .unwrap_or_default()
    }
}
