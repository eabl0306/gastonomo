//! Catalogo de monedas que la app ofrece al usuario.

use std::fmt;

use thiserror::Error;

use crate::value_object::CurrencyCode;

/// Codigos que Frankfurter publica pero que no son monedas de gasto.
///
/// Los cuatro metales (oro, plata, platino y paladio) y los derechos especiales
/// de giro del FMI. El prototipo los descarta con
/// `!/^X(AU|AG|PT|PD|DR)$/.test(iso_code)` y la API hace ese filtro una sola vez
/// para que ningun cliente lo repita.
pub const EXCLUDED_CURRENCIES: [&str; 5] = ["XAG", "XAU", "XDR", "XPD", "XPT"];

/// Indica si el codigo es uno de los que nunca se ofrecen.
#[must_use]
pub fn is_excluded(code: &CurrencyCode) -> bool {
    EXCLUDED_CURRENCIES.contains(&code.as_str())
}

/// Motivo por el que una entrada del catalogo no es valida.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CurrencyError {
    /// La moneda llego sin nombre.
    #[error("currency {code} has no name")]
    MissingName {
        /// Codigo recibido.
        code: String,
    },
    /// El codigo esta en la lista de exclusiones.
    #[error("currency {code} is never offered")]
    Excluded {
        /// Codigo recibido.
        code: String,
    },
}

/// Moneda del catalogo, tal y como se ofrece en el sheet de seleccion.
#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub struct Currency {
    code: CurrencyCode,
    name: String,
    symbol: String,
}

impl Currency {
    /// Crea una entrada del catalogo.
    ///
    /// Un simbolo vacio cae al propio codigo, igual que hace el prototipo con
    /// `SYMS[c.code] = c.symbol || c.code`: el sheet siempre tiene algo que
    /// pintar delante de la cifra.
    ///
    /// # Errors
    ///
    /// Devuelve [`CurrencyError::MissingName`] si el nombre esta vacio y
    /// [`CurrencyError::Excluded`] si el codigo es un metal o los XDR.
    pub fn new(code: CurrencyCode, name: &str, symbol: &str) -> Result<Self, CurrencyError> {
        if is_excluded(&code) {
            return Err(CurrencyError::Excluded {
                code: code.as_str().to_owned(),
            });
        }
        let name = name.trim();
        if name.is_empty() {
            return Err(CurrencyError::MissingName {
                code: code.as_str().to_owned(),
            });
        }
        let symbol = symbol.trim();
        let symbol = if symbol.is_empty() {
            code.as_str().to_owned()
        } else {
            symbol.to_owned()
        };
        Ok(Self {
            code,
            name: name.to_owned(),
            symbol,
        })
    }

    /// Codigo ISO 4217.
    #[must_use]
    pub const fn code(&self) -> &CurrencyCode {
        &self.code
    }

    /// Nombre en ingles, tal y como lo publica la fuente.
    #[must_use]
    pub fn name(&self) -> &str {
        &self.name
    }

    /// Simbolo para pintar delante del importe.
    #[must_use]
    pub fn symbol(&self) -> &str {
        &self.symbol
    }
}

impl fmt::Display for Currency {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} ({})", self.code, self.name)
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::{Currency, CurrencyError, is_excluded};
    use crate::value_object::CurrencyCode;

    fn code(raw: &str) -> CurrencyCode {
        CurrencyCode::parse(raw).expect("literal is a valid code")
    }

    #[test]
    fn the_four_metals_and_the_sdr_are_never_offered() {
        for raw in ["xau", "XAG", "XPT", "XPD", "XDR"] {
            assert!(is_excluded(&code(raw)), "{raw} should be excluded");
        }
    }

    #[test]
    fn an_ordinary_currency_is_offered() {
        assert!(!is_excluded(&code("COP")));
        assert!(!is_excluded(&code("USD")));
    }

    #[test]
    fn a_blank_symbol_falls_back_to_the_code() {
        let currency = Currency::new(code("COP"), "Colombian Peso", "   ").expect("should build");
        assert_eq!(currency.symbol(), "COP");
        assert_eq!(currency.name(), "Colombian Peso");
        assert_eq!(currency.code().as_str(), "COP");
    }

    #[test]
    fn a_symbol_keeps_its_own_glyph() {
        let currency = Currency::new(code("EUR"), " Euro ", " € ").expect("should build");
        assert_eq!(currency.symbol(), "€");
        assert_eq!(currency.name(), "Euro");
        assert_eq!(currency.to_string(), "EUR (Euro)");
    }

    #[test]
    fn a_nameless_currency_is_rejected() {
        assert!(matches!(
            Currency::new(code("COP"), "  ", "$"),
            Err(CurrencyError::MissingName { .. })
        ));
    }

    #[test]
    fn gold_cannot_enter_the_catalog_even_with_a_name() {
        assert!(matches!(
            Currency::new(code("XAU"), "Gold (Troy Ounce)", "oz t"),
            Err(CurrencyError::Excluded { .. })
        ));
    }
}
