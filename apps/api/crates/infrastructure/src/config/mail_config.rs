//! Configuracion de envio de correo.

use serde::{Deserialize, Serialize};

/// Adaptador de correo elegido por configuracion.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum MailDriver {
    /// Escribe el mensaje en el log. Es el de desarrollo y el de los tests.
    #[default]
    Log,
    /// Envia por SMTP.
    Smtp,
}

/// Configuracion del correo saliente.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct MailConfig {
    /// Adaptador que se usa.
    pub driver: MailDriver,
    /// Host del servidor SMTP.
    pub host: String,
    /// Puerto del servidor SMTP.
    pub port: u16,
    /// Usuario SMTP. Vacio significa sin autenticacion, como en Mailpit.
    pub username: String,
    /// Contrasena SMTP.
    pub password: String,
    /// Remitente de los mensajes.
    pub from: String,
    /// Exige STARTTLS. En desarrollo contra Mailpit va apagado.
    pub starttls: bool,
}

impl Default for MailConfig {
    fn default() -> Self {
        Self {
            driver: MailDriver::Log,
            host: "localhost".to_owned(),
            port: 1025,
            username: String::new(),
            password: String::new(),
            from: "Gastonomo <no-reply@gastonomo.local>".to_owned(),
            starttls: false,
        }
    }
}

impl MailConfig {
    /// Indica si hay credenciales SMTP configuradas.
    #[must_use]
    pub const fn has_credentials(&self) -> bool {
        !self.username.is_empty()
    }
}
