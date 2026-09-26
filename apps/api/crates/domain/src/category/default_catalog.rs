//! Las 12 categorias predeterminadas del diseno.
//!
//! Se siembran por usuario al registrarse con `is_default = true`, y no se pueden
//! editar ni borrar. Nombre, icono y color salen tal cual del handoff; el icono es
//! un Material Symbol y el color, el hex que pinta el tile.

use super::kind::CategoryKind;

/// Una categoria predeterminada.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DefaultCategory {
    /// Nombre que ve el usuario.
    pub name: &'static str,
    /// Tipo de movimiento al que aplica.
    pub kind: CategoryKind,
    /// Material Symbol del tile.
    pub icon: &'static str,
    /// Color del tile en hexadecimal.
    pub color: &'static str,
}

/// Nombre de la categoria a la que van los movimientos de una categoria borrada.
pub const FALLBACK_CATEGORY_NAME: &str = "Otros";

/// Catalogo completo, en el orden en que lo lista el handoff.
pub const DEFAULT_CATEGORIES: [DefaultCategory; 12] = [
    DefaultCategory {
        name: "Alimentación",
        kind: CategoryKind::Out,
        icon: "restaurant",
        color: "#cf5f35",
    },
    DefaultCategory {
        name: "Educación",
        kind: CategoryKind::Out,
        icon: "school",
        color: "#3f6fc4",
    },
    DefaultCategory {
        name: "Entretenimiento",
        kind: CategoryKind::Out,
        icon: "movie",
        color: "#b84a86",
    },
    DefaultCategory {
        name: "Salud",
        kind: CategoryKind::Out,
        icon: "medical_services",
        color: "#c9433f",
    },
    DefaultCategory {
        name: "Servicios",
        kind: CategoryKind::Out,
        icon: "bolt",
        color: "#c48f12",
    },
    DefaultCategory {
        name: "Transporte",
        kind: CategoryKind::Out,
        icon: "directions_bus",
        color: "#9a6a3a",
    },
    DefaultCategory {
        name: "Vivienda",
        kind: CategoryKind::Out,
        icon: "home",
        color: "#7a52c7",
    },
    DefaultCategory {
        name: "Salario",
        kind: CategoryKind::In,
        icon: "account_balance_wallet",
        color: "#2f8a55",
    },
    DefaultCategory {
        name: "Freelance",
        kind: CategoryKind::In,
        icon: "laptop_mac",
        color: "#2f7fb5",
    },
    DefaultCategory {
        name: "Préstamo",
        kind: CategoryKind::Both,
        icon: "handshake",
        color: "#5b6573",
    },
    DefaultCategory {
        name: "Abono préstamo",
        kind: CategoryKind::Both,
        icon: "payments",
        color: "#5b6573",
    },
    DefaultCategory {
        name: FALLBACK_CATEGORY_NAME,
        kind: CategoryKind::Both,
        icon: "more_horiz",
        color: "#7c7669",
    },
];

/// Categoria predeterminada con la que se crea el movimiento de un prestamo.
pub const LOAN_CATEGORY_NAME: &str = "Préstamo";

/// Categoria predeterminada con la que se crea el movimiento de un abono.
pub const LOAN_PAYMENT_CATEGORY_NAME: &str = "Abono préstamo";

#[cfg(test)]
mod tests {
    use super::{DEFAULT_CATEGORIES, FALLBACK_CATEGORY_NAME};
    use crate::category::kind::CategoryKind;

    #[test]
    fn the_catalog_has_the_twelve_names_of_the_design() {
        assert_eq!(DEFAULT_CATEGORIES.len(), 12);
    }

    #[test]
    fn every_name_is_unique_ignoring_case() {
        let mut names: Vec<String> = DEFAULT_CATEGORIES
            .iter()
            .map(|category| category.name.to_lowercase())
            .collect();
        names.sort();
        let total = names.len();
        names.dedup();
        assert_eq!(names.len(), total);
    }

    #[test]
    fn the_shared_ones_serve_both_grids() {
        let shared: Vec<&str> = DEFAULT_CATEGORIES
            .iter()
            .filter(|category| category.kind == CategoryKind::Both)
            .map(|category| category.name)
            .collect();
        assert_eq!(shared, vec!["Préstamo", "Abono préstamo", "Otros"]);
        assert_eq!(FALLBACK_CATEGORY_NAME, "Otros");
    }
}
