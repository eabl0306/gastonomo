//! Decimales por moneda.
//!
//! La lista sale tal cual del prototipo de diseno (la constante `ZERO` del
//! HTML): son las monedas que no usan fraccion. Todo lo demas se muestra con dos
//! decimales. Esto afecta a la presentacion y al umbral de redondeo a cero, no a
//! como se guarda el importe (siempre `NUMERIC(18,4)`).

/// Monedas sin decimales, copiadas del prototipo.
pub const ZERO_DECIMAL_CURRENCIES: [&str; 34] = [
    "COP", "CLP", "JPY", "KRW", "IDR", "VND", "PYG", "HUF", "ISK", "UGX", "IRR", "LAK", "KHR",
    "MMK", "LBP", "IQD", "UZS", "GNF", "MGA", "RWF", "BIF", "XAF", "XOF", "KPW", "SYP", "TZS",
    "MNT", "KMF", "DJF", "VUV", "XPF", "SLE", "SOS", "SSP",
];

/// Decimales que se muestran para un codigo ISO ya normalizado.
#[must_use]
pub fn decimals_for(code: &str) -> u32 {
    u32::from(!is_zero_decimal(code)) * 2
}

/// Indica si la moneda no usa decimales.
#[must_use]
pub fn is_zero_decimal(code: &str) -> bool {
    ZERO_DECIMAL_CURRENCIES.contains(&code)
}

#[cfg(test)]
mod tests {
    use super::{ZERO_DECIMAL_CURRENCIES, decimals_for, is_zero_decimal};

    #[test]
    fn the_prototype_list_is_copied_whole() {
        assert_eq!(ZERO_DECIMAL_CURRENCIES.len(), 34);
        assert!(is_zero_decimal("COP"));
        assert!(is_zero_decimal("SSP"));
    }

    #[test]
    fn currencies_with_fraction_show_two_decimals() {
        assert_eq!(decimals_for("USD"), 2);
        assert_eq!(decimals_for("EUR"), 2);
        assert_eq!(decimals_for("COP"), 0);
    }
}
