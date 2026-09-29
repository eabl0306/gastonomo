//! El correo de recuperacion, en los dos idiomas de la aplicacion.
//!
//! Los dos textos viven juntos a proposito. Estaban dentro del caso de uso y solo
//! en castellano, asi que alguien con el telefono en ingles pedia el enlace desde
//! una pantalla en ingles y recibia un correo en espanol. Teniendolos al lado, la
//! siguiente frase que se toque se toca en los dos o se ve que falta.
//!
//! No hay motor de plantillas ni parte HTML: el correo es texto plano, que es lo
//! que manda [`crate::port::Mailer`].

use gastonomo_domain::settings::Language;
use gastonomo_domain::{PersonName, RESET_TOKEN_MINUTES};

use crate::port::MailMessage;
use gastonomo_domain::Email;

/// Asunto y cuerpo ya compuestos.
pub(super) fn reset_mail(
    to: Email,
    name: &PersonName,
    link: &str,
    language: Language,
) -> MailMessage {
    let name = name.as_str();
    let minutes = RESET_TOKEN_MINUTES;
    let (subject, body) = match language {
        Language::Spanish => (
            "Recupera tu contrasena de Gastonomo".to_owned(),
            format!(
                "Hola, {name}.\n\n\
                 Pediste crear una contrasena nueva para tu cuenta de Gastonomo.\n\
                 Abre este enlace: {link}\n\n\
                 Caduca en {minutes} minutos y solo sirve una vez.\n\
                 Si lo pediste varias veces, solo vale el ultimo enlace.\n\
                 Si no lo pediste, no hace falta que hagas nada.\n"
            ),
        ),
        Language::English => (
            "Reset your Gastonomo password".to_owned(),
            format!(
                "Hi {name},\n\n\
                 You asked to create a new password for your Gastonomo account.\n\
                 Open this link: {link}\n\n\
                 It expires in {minutes} minutes and only works once.\n\
                 If you asked more than once, only the latest link works.\n\
                 If you did not ask for it, you do not need to do anything.\n"
            ),
        ),
    };
    MailMessage { to, subject, body }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::*;

    fn mail_in(language: Language) -> MailMessage {
        let email = Email::parse("ana@example.com").expect("el correo de prueba es valido");
        let name = PersonName::parse("Ana").expect("el nombre de prueba es valido");
        reset_mail(email, &name, "https://gastonomo.test/r?token=abc", language)
    }

    #[test]
    fn el_correo_sale_en_el_idioma_de_la_cuenta() {
        assert!(mail_in(Language::Spanish).subject.contains("Recupera"));
        assert!(mail_in(Language::English).subject.contains("Reset"));
    }

    /// Lo que no puede faltar en ninguno de los dos, pase lo que pase con el texto:
    /// el enlace, el nombre y cuanto dura. Sin el enlace el correo no sirve de nada.
    #[test]
    fn los_dos_llevan_el_enlace_el_nombre_y_el_plazo() {
        for language in [Language::Spanish, Language::English] {
            let mail = mail_in(language);
            assert!(mail.body.contains("https://gastonomo.test/r?token=abc"));
            assert!(mail.body.contains("Ana"));
            assert!(mail.body.contains(&RESET_TOKEN_MINUTES.to_string()));
        }
    }
}
