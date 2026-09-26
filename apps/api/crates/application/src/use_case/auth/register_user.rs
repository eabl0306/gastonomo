//! Caso de uso: registro de una cuenta nueva.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::{Email, NewUser, Password, PersonName, User};

use super::session::{IssuedSession, SessionPorts, open_session};
use crate::error::{AppError, ValidationErrors};
use crate::port::UserProvisioning;

/// Datos del formulario de registro.
#[derive(Debug, Clone)]
pub struct RegisterUserCommand {
    /// Nombre que se muestra en el saludo.
    pub name: String,
    /// Correo de la cuenta.
    pub email: String,
    /// Contrasena en claro, de 8 caracteres o mas.
    pub password: String,
    /// Aceptacion de los terminos, que el diseno exige marcar.
    pub accepted_terms: bool,
    /// `User-Agent` de quien se registra.
    pub user_agent: Option<String>,
}

/// Registra una cuenta y deja la sesion abierta.
///
/// Ademas de crear la cuenta siembra las 12 categorias predeterminadas y la fila
/// de ajustes, porque sin ellas la app no tiene nada que pintar en Categorias ni
/// moneda con la que formatear.
#[derive(Clone)]
pub struct RegisterUser {
    ports: SessionPorts,
    provisioning: Arc<dyn UserProvisioning>,
}

impl fmt::Debug for RegisterUser {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RegisterUser").finish_non_exhaustive()
    }
}

/// Campos validados del registro.
struct ValidRegistration {
    name: PersonName,
    email: Email,
    password: Password,
}

impl RegisterUser {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: SessionPorts, provisioning: Arc<dyn UserProvisioning>) -> Self {
        Self {
            ports,
            provisioning,
        }
    }

    /// Registra la cuenta.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si el formulario no cumple las reglas,
    /// [`AppError::Conflict`] con codigo `email_taken` si el correo ya tiene
    /// cuenta, y [`AppError::Dependency`] si falla una dependencia.
    pub async fn execute(&self, command: RegisterUserCommand) -> Result<IssuedSession, AppError> {
        let valid = validate(&command)?;
        let now = self.ports.clock.now();
        let password_hash = self.ports.password_hasher.hash(&valid.password).await?;
        let user = User::register(NewUser {
            name: valid.name,
            email: valid.email,
            password_hash,
            created_at: now,
        });

        self.ports.users.insert(&user).await?;
        self.provisioning.provision_new_user(user.id(), now).await?;
        open_session(&self.ports, &user, command.user_agent).await
    }
}

fn validate(command: &RegisterUserCommand) -> Result<ValidRegistration, AppError> {
    let mut errors = ValidationErrors::new();
    let name = PersonName::parse(&command.name);
    let email = Email::parse(&command.email);
    let password = Password::parse(&command.password);

    // El mensaje que se guarda es el que la pantalla pinta junto al campo, asi
    // que va en castellano y no es el Display del error de dominio, que esta en
    // ingles porque habla con quien lee los logs.
    if name.is_err() {
        errors.push("name", "El nombre es obligatorio.");
    }
    if email.is_err() {
        errors.push("email", "El correo no tiene un formato valido.");
    }
    if password.is_err() {
        errors.push("password", "La contrasena necesita 8 caracteres o mas.");
    }
    if !command.accepted_terms {
        errors.push("acceptedTerms", "Tienes que aceptar los terminos de uso.");
    }
    errors.into_result()?;

    let (Ok(name), Ok(email), Ok(password)) = (name, email, password) else {
        unreachable!("every error was already collected above")
    };
    Ok(ValidRegistration {
        name,
        email,
        password,
    })
}
