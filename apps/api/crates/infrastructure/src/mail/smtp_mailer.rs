//! Adaptador driven: correo por SMTP.
//!
//! El envio no se espera dentro de la peticion: se construye el mensaje, se
//! entrega a una tarea de fondo y se responde. El motivo es de seguridad, no de
//! rendimiento. `POST /v1/auth/password/forgot` responde 202 tanto si la cuenta
//! existe como si no, justamente para no decir quien esta registrado, pero si el
//! envio se esperase, la rama con cuenta tardaria decenas de milisegundos y la otra
//! apenas uno: el tiempo de respuesta delataria lo que el codigo de estado calla.
//! Midiendolo contra Mailpit eran 50 ms frente a 2 ms, sin solaparse, o sea una
//! respuesta fiable a "existe esta cuenta".
//!
//! El precio es que un fallo de envio ya no llega al caso de uso. Para este correo
//! es lo correcto: al usuario se le responde igual de todas formas, asi que lo que
//! toca es dejarlo en el log para quien opere el servicio.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::{MailMessage, Mailer};
use lettre::message::Mailbox;
use lettre::transport::smtp::authentication::Credentials;
use lettre::{AsyncSmtpTransport, AsyncTransport, Message, Tokio1Executor};

use super::MailError;
use crate::config::MailConfig;

/// Envia correo por SMTP.
#[derive(Debug, Clone)]
pub struct SmtpMailer {
    transport: AsyncSmtpTransport<Tokio1Executor>,
    from: Mailbox,
}

impl SmtpMailer {
    /// Construye el transporte a partir de la configuracion.
    ///
    /// Sin STARTTLS el transporte se monta en claro, que es lo que hace falta para
    /// hablar con Mailpit en desarrollo. En produccion se enciende `starttls`.
    ///
    /// # Errors
    ///
    /// Devuelve [`MailError`] si el remitente no es una direccion valida o si el
    /// transporte no se puede construir.
    pub fn from_config(config: &MailConfig) -> Result<Self, MailError> {
        let from: Mailbox = config
            .from
            .parse()
            .map_err(|_| MailError::InvalidSender(config.from.clone()))?;
        let mut builder = if config.starttls {
            AsyncSmtpTransport::<Tokio1Executor>::starttls_relay(&config.host)
                .map_err(|error| MailError::Transport(error.to_string()))?
        } else {
            AsyncSmtpTransport::<Tokio1Executor>::builder_dangerous(&config.host)
        }
        .port(config.port);

        if config.has_credentials() {
            builder = builder.credentials(Credentials::new(
                config.username.clone(),
                config.password.clone(),
            ));
        }
        Ok(Self {
            transport: builder.build(),
            from,
        })
    }
}

#[async_trait]
impl Mailer for SmtpMailer {
    async fn send(&self, message: &MailMessage) -> Result<(), AppError> {
        let to: Mailbox = message
            .to
            .as_str()
            .parse()
            .map_err(|_| AppError::dependency("the recipient is not a valid mailbox"))?;
        let email = Message::builder()
            .from(self.from.clone())
            .to(to)
            .subject(message.subject.clone())
            .body(message.body.clone())
            .map_err(|error| AppError::dependency(format!("could not build the mail: {error}")))?;
        // El clon es del transporte, que comparte el pool de conexiones por dentro,
        // asi que la tarea no abre nada nuevo.
        let transport = self.transport.clone();
        let recipient = message.to.as_str().to_owned();
        tokio::spawn(async move {
            if let Err(error) = transport.send(email).await {
                // El destinatario se registra, el cuerpo no: lleva el enlace de
                // recuperacion, que es tan bueno como la contrasena.
                tracing::error!(%error, recipient = %recipient, "could not send the mail");
            }
        });
        Ok(())
    }
}
