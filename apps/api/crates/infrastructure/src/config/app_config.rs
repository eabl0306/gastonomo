//! Carga de configuracion con prefijo `GASTONOMO__`.

use std::net::{IpAddr, Ipv4Addr, SocketAddr};

use figment::Figment;
use figment::providers::Env;
use serde::{Deserialize, Serialize};
use thiserror::Error;

/// Prefijo de todas las variables de entorno del servicio.
pub const ENV_PREFIX: &str = "GASTONOMO__";

/// Separador de niveles dentro del nombre de la variable.
const ENV_SEPARATOR: &str = "__";

/// Fallo al construir la configuracion.
#[derive(Debug, Error)]
pub enum ConfigError {
    /// El entorno no se pudo interpretar como configuracion valida.
    ///
    /// El error de figment ocupa varios cientos de bytes, asi que viaja en un
    /// `Box` para que `Result<_, ConfigError>` siga siendo barato de mover.
    #[error("invalid configuration: {0}")]
    Invalid(#[source] Box<figment::Error>),
    /// La direccion de escucha resultante no es valida.
    #[error("invalid listen address {host}:{port}")]
    InvalidAddress {
        /// Host configurado.
        host: String,
        /// Puerto configurado.
        port: u16,
    },
}

/// Configuracion del servidor HTTP.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct ServerConfig {
    /// Direccion de escucha.
    pub host: String,
    /// Puerto de escucha.
    pub port: u16,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            host: "0.0.0.0".to_owned(),
            port: 8080,
        }
    }
}

impl ServerConfig {
    /// Resuelve la direccion de escucha.
    ///
    /// # Errors
    ///
    /// Devuelve [`ConfigError::InvalidAddress`] si el host no es una IP valida.
    pub fn socket_addr(&self) -> Result<SocketAddr, ConfigError> {
        self.host.parse::<IpAddr>().map_or_else(
            |_| {
                Err(ConfigError::InvalidAddress {
                    host: self.host.clone(),
                    port: self.port,
                })
            },
            |ip| Ok(SocketAddr::new(ip, self.port)),
        )
    }
}

/// Configuracion de acceso a PostgreSQL.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct DatabaseConfig {
    /// Cadena de conexion completa.
    pub url: String,
    /// Numero maximo de conexiones del pool.
    pub max_connections: u32,
    /// Tiempo maximo, en milisegundos, para obtener una conexion.
    pub acquire_timeout_ms: u64,
}

impl Default for DatabaseConfig {
    fn default() -> Self {
        Self {
            url: "postgres://gastonomo:gastonomo@localhost:5432/gastonomo".to_owned(),
            max_connections: 5,
            acquire_timeout_ms: 3_000,
        }
    }
}

/// Configuracion de telemetria.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct TelemetryConfig {
    /// Filtro de nivel de log, con la sintaxis de `RUST_LOG`.
    pub log_level: String,
}

impl Default for TelemetryConfig {
    fn default() -> Self {
        Self {
            log_level: "info".to_owned(),
        }
    }
}

/// Configuracion completa del servicio.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields, default)]
pub struct AppConfig {
    /// Servidor HTTP.
    pub server: ServerConfig,
    /// Base de datos.
    pub database: DatabaseConfig,
    /// Telemetria.
    pub telemetry: TelemetryConfig,
}

impl AppConfig {
    /// Carga la configuracion desde las variables de entorno del proceso.
    ///
    /// # Errors
    ///
    /// Devuelve [`ConfigError`] si alguna variable no encaja con el esquema.
    pub fn from_env() -> Result<Self, ConfigError> {
        Self::from_provider(Env::prefixed(ENV_PREFIX).split(ENV_SEPARATOR))
    }

    /// Carga la configuracion desde un proveedor arbitrario de figment.
    ///
    /// Los valores ausentes caen en los valores por defecto del servicio.
    ///
    /// # Errors
    ///
    /// Devuelve [`ConfigError`] si el proveedor no encaja con el esquema.
    pub fn from_provider(provider: impl figment::Provider) -> Result<Self, ConfigError> {
        Figment::new()
            .merge(figment::providers::Serialized::defaults(Self::default()))
            .merge(provider)
            .extract()
            .map_err(|error| ConfigError::Invalid(Box::new(error)))
    }

    /// Direccion de escucha resuelta.
    ///
    /// # Errors
    ///
    /// Devuelve [`ConfigError::InvalidAddress`] si el host no es una IP valida.
    pub fn socket_addr(&self) -> Result<SocketAddr, ConfigError> {
        self.server.socket_addr()
    }

    /// Direccion de escucha por defecto, util para diagnosticos.
    #[must_use]
    pub const fn default_socket_addr() -> SocketAddr {
        SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 8080)
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]
    #![allow(
        clippy::result_large_err,
        reason = "figment::Jail fixes the closure signature and its error type"
    )]

    use figment::Jail;
    use figment::providers::Env;

    use super::{AppConfig, ConfigError, ENV_PREFIX, ServerConfig};

    fn load(jail: &Jail) -> Result<AppConfig, ConfigError> {
        let _ = jail;
        AppConfig::from_provider(Env::prefixed(ENV_PREFIX).split("__"))
    }

    #[test]
    fn falls_back_to_defaults_when_the_environment_is_empty() {
        Jail::expect_with(|jail| {
            let config = load(jail).expect("defaults should load");
            assert_eq!(config.server.port, 8080);
            assert_eq!(config.server.host, "0.0.0.0");
            assert_eq!(config.telemetry.log_level, "info");
            assert_eq!(config.database.max_connections, 5);
            Ok(())
        });
    }

    #[test]
    fn reads_nested_keys_from_double_underscore_variables() {
        Jail::expect_with(|jail| {
            jail.set_env("GASTONOMO__SERVER__HOST", "127.0.0.1");
            jail.set_env("GASTONOMO__SERVER__PORT", "9090");
            jail.set_env("GASTONOMO__DATABASE__URL", "postgres://u:p@db:5432/x");
            jail.set_env("GASTONOMO__TELEMETRY__LOG_LEVEL", "debug");
            let config = load(jail).expect("environment should load");
            assert_eq!(config.server.port, 9090);
            assert_eq!(config.database.url, "postgres://u:p@db:5432/x");
            assert_eq!(config.telemetry.log_level, "debug");
            assert_eq!(
                config.socket_addr().expect("address should resolve"),
                "127.0.0.1:9090".parse().expect("literal is valid")
            );
            Ok(())
        });
    }

    #[test]
    fn rejects_a_port_that_is_not_a_number() {
        Jail::expect_with(|jail| {
            jail.set_env("GASTONOMO__SERVER__PORT", "not-a-port");
            assert!(load(jail).is_err());
            Ok(())
        });
    }

    #[test]
    fn rejects_a_host_that_is_not_an_ip() {
        let server = ServerConfig {
            host: "not an ip".to_owned(),
            port: 8080,
        };
        assert!(matches!(
            server.socket_addr(),
            Err(ConfigError::InvalidAddress { .. })
        ));
    }

    #[test]
    fn default_address_matches_the_documented_contract() {
        assert_eq!(AppConfig::default_socket_addr().port(), 8080);
    }
}
