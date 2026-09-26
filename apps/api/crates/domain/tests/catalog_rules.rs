//! Catalogo compartido: tipos de categoria, predeterminadas e idiomas.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use gastonomo_domain::category::{
    DEFAULT_CATEGORIES, LOAN_CATEGORY_NAME, LOAN_PAYMENT_CATEGORY_NAME,
};
use gastonomo_domain::{CategoryKind, FALLBACK_CATEGORY_NAME, Language};

#[test]
fn a_category_kind_round_trips_through_its_stored_value() {
    for kind in [CategoryKind::In, CategoryKind::Out, CategoryKind::Both] {
        let stored = kind.as_str();
        assert_eq!(CategoryKind::parse(stored), Ok(kind));
        assert_eq!(kind.to_string(), stored);
    }
    assert!(CategoryKind::parse("income").is_err());
}

#[test]
fn only_the_shared_kind_is_out_of_reach_for_users() {
    assert!(CategoryKind::In.is_selectable_by_users());
    assert!(CategoryKind::Out.is_selectable_by_users());
    assert!(!CategoryKind::Both.is_selectable_by_users());
}

#[test]
fn the_categories_the_loans_context_needs_are_in_the_catalog() {
    for name in [
        LOAN_CATEGORY_NAME,
        LOAN_PAYMENT_CATEGORY_NAME,
        FALLBACK_CATEGORY_NAME,
    ] {
        let found = DEFAULT_CATEGORIES
            .iter()
            .find(|category| category.name == name)
            .unwrap_or_else(|| unreachable!("{name} should be a default category"));
        assert_eq!(found.kind, CategoryKind::Both);
        assert!(
            found
                .icon
                .chars()
                .all(|c| c.is_ascii_lowercase() || c == '_')
        );
        assert!(found.color.starts_with('#') && found.color.len() == 7);
    }
}

#[test]
fn every_default_category_carries_an_icon_and_a_hex_color() {
    for category in DEFAULT_CATEGORIES {
        assert!(!category.icon.is_empty(), "{} has no icon", category.name);
        assert_eq!(category.color.len(), 7, "{} has a bad color", category.name);
    }
}

#[test]
fn a_language_round_trips_and_carries_its_locale() {
    assert_eq!(Language::parse("es"), Ok(Language::Spanish));
    assert_eq!(Language::parse(" en "), Ok(Language::English));
    assert!(Language::parse("fr").is_err());
    assert_eq!(Language::Spanish.as_str(), "es");
    assert_eq!(Language::Spanish.locale(), "es-CO");
    assert_eq!(Language::English.to_string(), "en");
}
