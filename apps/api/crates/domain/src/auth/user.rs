//! Agregado de cuenta de usuario.

use chrono::{DateTime, Utc};

use super::credentials::PasswordHash;
use crate::value_object::{Email, PersonName, UserId};

/// Datos con los que nace una cuenta.
///
/// Va en un struct y no en cinco argumentos porque el limite de parametros del
/// repositorio es cinco y este agregado ya tiene seis campos.
#[derive(Debug, Clone)]
pub struct NewUser {
    /// Nombre que se muestra en el saludo de Inicio.
    pub name: PersonName,
    /// Correo, unico entre todas las cuentas.
    pub email: Email,
    /// Hash de la contrasena.
    pub password_hash: PasswordHash,
    /// Instante de creacion.
    pub created_at: DateTime<Utc>,
}

/// Cuenta de usuario.
#[derive(Debug, Clone)]
pub struct User {
    id: UserId,
    name: PersonName,
    email: Email,
    password_hash: PasswordHash,
    created_at: DateTime<Utc>,
    updated_at: DateTime<Utc>,
}

impl User {
    /// Crea una cuenta nueva con un identificador recien generado.
    #[must_use]
    pub fn register(new_user: NewUser) -> Self {
        Self {
            id: UserId::generate(),
            name: new_user.name,
            email: new_user.email,
            password_hash: new_user.password_hash,
            created_at: new_user.created_at,
            updated_at: new_user.created_at,
        }
    }

    /// Reconstruye una cuenta leida de la base de datos.
    #[must_use]
    pub fn rehydrate(id: UserId, new_user: NewUser, updated_at: DateTime<Utc>) -> Self {
        Self {
            id,
            name: new_user.name,
            email: new_user.email,
            password_hash: new_user.password_hash,
            created_at: new_user.created_at,
            updated_at,
        }
    }

    /// Cambia el hash de la contrasena.
    pub fn change_password(&mut self, password_hash: PasswordHash, now: DateTime<Utc>) {
        self.password_hash = password_hash;
        self.updated_at = now;
    }

    /// Identificador de la cuenta.
    #[must_use]
    pub const fn id(&self) -> UserId {
        self.id
    }

    /// Nombre que se muestra en la app.
    #[must_use]
    pub const fn name(&self) -> &PersonName {
        &self.name
    }

    /// Correo de la cuenta.
    #[must_use]
    pub const fn email(&self) -> &Email {
        &self.email
    }

    /// Hash de la contrasena.
    #[must_use]
    pub const fn password_hash(&self) -> &PasswordHash {
        &self.password_hash
    }

    /// Instante de creacion.
    #[must_use]
    pub const fn created_at(&self) -> DateTime<Utc> {
        self.created_at
    }

    /// Instante de la ultima modificacion.
    #[must_use]
    pub const fn updated_at(&self) -> DateTime<Utc> {
        self.updated_at
    }
}
