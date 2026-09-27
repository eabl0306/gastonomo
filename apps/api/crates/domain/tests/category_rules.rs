//! Reglas del agregado de categoria y de sus value objects.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use chrono::{Duration, Utc};
use gastonomo_domain::category::{
    CUSTOM_CATEGORY_COLORS, CUSTOM_CATEGORY_ICONS, Category, CategoryChange, CategoryColor,
    CategoryError, CategoryIcon, CategoryId, CategoryKind, CategoryName, CategoryNameError,
    MAX_CATEGORY_NAME_LENGTH, NewCategory,
};
use gastonomo_domain::value_object::UserId;

fn a_category(name: &str) -> Category {
    Category::create_custom(NewCategory {
        user_id: UserId::generate(),
        name: CategoryName::parse(name).expect("valid name"),
        kind: CategoryKind::Out,
        icon: CategoryIcon::parse("pets").expect("offered icon"),
        color: CategoryColor::parse("#2f7fb5").expect("offered color"),
        created_at: Utc::now(),
    })
    .expect("the fixture respects the design rules")
}

#[test]
fn a_name_is_trimmed_and_capped_at_the_length_of_the_sheet() {
    assert_eq!(
        CategoryName::parse("  Mascotas  ").unwrap().as_str(),
        "Mascotas"
    );
    assert_eq!(MAX_CATEGORY_NAME_LENGTH, 22);
    let limit = "a".repeat(MAX_CATEGORY_NAME_LENGTH);
    assert!(CategoryName::parse(&limit).is_ok());
    assert_eq!(
        CategoryName::parse(&"a".repeat(MAX_CATEGORY_NAME_LENGTH + 1)),
        Err(CategoryNameError::TooLong)
    );
    assert_eq!(CategoryName::parse("   "), Err(CategoryNameError::Empty));
}

#[test]
fn two_names_that_only_differ_in_case_are_the_same_name() {
    let name = CategoryName::parse("Mascotas").unwrap();

    assert!(name.same_as("  mascotas "));
    assert!(!name.same_as("Mascotaz"));
    assert_eq!(name.comparison_key(), "mascotas");
}

#[test]
fn the_palette_of_the_sheet_is_the_one_of_the_handoff() {
    assert_eq!(CUSTOM_CATEGORY_ICONS.len(), 16);
    assert_eq!(CUSTOM_CATEGORY_COLORS.len(), 8);
    assert_eq!(CUSTOM_CATEGORY_ICONS[0], "shopping_cart");
    assert_eq!(CUSTOM_CATEGORY_COLORS[0], "#cf5f35");
    assert!(CategoryIcon::parse("PETS").unwrap().is_offered_to_users());
    assert!(
        !CategoryIcon::parse("rocket_launch")
            .unwrap()
            .is_offered_to_users()
    );
    assert!(
        CategoryColor::parse("#2F7FB5")
            .unwrap()
            .is_offered_to_users()
    );
    assert!(
        !CategoryColor::parse("#123456")
            .unwrap()
            .is_offered_to_users()
    );
}

#[test]
fn a_color_has_to_be_six_hex_digits_with_a_hash() {
    assert_eq!(CategoryColor::parse("#2f7fb5").unwrap().as_str(), "#2f7fb5");
    assert!(CategoryColor::parse("2f7fb5").is_err());
    assert!(CategoryColor::parse("#2f7fb").is_err());
    assert!(CategoryColor::parse("#gggggg").is_err());
}

#[test]
fn an_icon_only_admits_the_shape_of_a_material_symbol() {
    assert!(CategoryIcon::parse("local_gas_station").is_ok());
    assert!(CategoryIcon::parse("  ").is_err());
    assert!(CategoryIcon::parse("rocket launch").is_err());
    assert!(CategoryIcon::parse(&"a".repeat(41)).is_err());
}

#[test]
fn the_user_cannot_create_a_category_that_serves_both_grids() {
    let attempt = Category::create_custom(NewCategory {
        user_id: UserId::generate(),
        name: CategoryName::parse("Mascotas").unwrap(),
        kind: CategoryKind::Both,
        icon: CategoryIcon::parse("pets").unwrap(),
        color: CategoryColor::parse("#2f7fb5").unwrap(),
        created_at: Utc::now(),
    });

    assert_eq!(attempt, Err(CategoryError::KindNotSelectable));
}

#[test]
fn a_category_of_the_user_only_admits_the_icons_and_colors_of_the_sheet() {
    let with_bogus_icon = Category::create_custom(NewCategory {
        user_id: UserId::generate(),
        name: CategoryName::parse("Mascotas").unwrap(),
        kind: CategoryKind::Out,
        icon: CategoryIcon::parse("rocket_launch").unwrap(),
        color: CategoryColor::parse("#2f7fb5").unwrap(),
        created_at: Utc::now(),
    });
    assert_eq!(with_bogus_icon, Err(CategoryError::IconNotOffered));

    let with_bogus_color = Category::create_custom(NewCategory {
        user_id: UserId::generate(),
        name: CategoryName::parse("Mascotas").unwrap(),
        kind: CategoryKind::Out,
        icon: CategoryIcon::parse("pets").unwrap(),
        color: CategoryColor::parse("#123456").unwrap(),
        created_at: Utc::now(),
    });
    assert_eq!(with_bogus_color, Err(CategoryError::ColorNotOffered));
}

#[test]
fn a_change_touches_only_what_it_brings_and_moves_the_clock() {
    let mut category = a_category("Mascotas");
    let created_at = category.created_at();
    let later = created_at + Duration::hours(2);

    category
        .apply(
            CategoryChange {
                name: Some(CategoryName::parse("Veterinario").unwrap()),
                kind: Some(CategoryKind::In),
                ..CategoryChange::default()
            },
            later,
        )
        .expect("the change is valid");

    assert_eq!(category.name().as_str(), "Veterinario");
    assert_eq!(category.kind(), CategoryKind::In);
    assert_eq!(category.icon().as_str(), "pets");
    assert_eq!(category.color().as_str(), "#2f7fb5");
    assert_eq!(category.created_at(), created_at);
    assert_eq!(category.updated_at(), later);
}

#[test]
fn an_empty_change_is_recognised_as_empty() {
    assert!(CategoryChange::default().is_empty());
    assert!(
        !CategoryChange {
            color: Some(CategoryColor::parse("#b84a86").unwrap()),
            ..CategoryChange::default()
        }
        .is_empty()
    );
}

#[test]
fn a_default_category_refuses_every_change() {
    let mut category = Category::rehydrate(
        CategoryId::generate(),
        NewCategory {
            user_id: UserId::generate(),
            name: CategoryName::parse("Salario").unwrap(),
            kind: CategoryKind::In,
            icon: CategoryIcon::parse("account_balance_wallet").unwrap(),
            color: CategoryColor::parse("#2f8a55").unwrap(),
            created_at: Utc::now(),
        },
        true,
        Utc::now(),
        None,
    );

    assert!(category.is_default());
    assert_eq!(
        category.check_writable(),
        Err(CategoryError::DefaultIsReadOnly)
    );
    assert_eq!(
        category.apply(CategoryChange::default(), Utc::now()),
        Err(CategoryError::DefaultIsReadOnly)
    );
    assert_eq!(category.name().as_str(), "Salario");
}

#[test]
fn a_default_category_can_keep_an_icon_and_a_color_outside_the_sheet() {
    let category = Category::rehydrate(
        CategoryId::generate(),
        NewCategory {
            user_id: UserId::generate(),
            name: CategoryName::parse("Transporte").unwrap(),
            kind: CategoryKind::Out,
            icon: CategoryIcon::parse("directions_bus").unwrap(),
            color: CategoryColor::parse("#9a6a3a").unwrap(),
            created_at: Utc::now(),
        },
        true,
        Utc::now(),
        None,
    );

    assert!(!category.icon().is_offered_to_users());
    assert!(!category.color().is_offered_to_users());
}

#[test]
fn an_identifier_reads_back_from_its_text() {
    let id = CategoryId::generate();

    assert_eq!(CategoryId::parse(&format!("  {id} ")).unwrap(), id);
    assert!(CategoryId::parse("no-es-un-uuid").is_err());
}

#[test]
fn the_category_identifier_is_the_same_type_in_every_context() {
    // Hubo dos CategoryId, uno en `category` y otro en `movement`, y nada impedia
    // que se separaran. Esta asignacion no compila si vuelven a ser dos tipos.
    let from_category: gastonomo_domain::category::CategoryId = CategoryId::generate();
    let from_movement: gastonomo_domain::movement::CategoryId = from_category;

    assert_eq!(from_category, from_movement);
}

#[test]
fn archiving_a_category_does_not_change_anything_else_about_it() {
    let mut category = custom("Mascotas");
    let before = category.clone();
    let now = Utc::now();

    category.archive(now).unwrap();

    // Lo unico que cambia es que deja de ofrecerse. Es la diferencia con borrar,
    // que reescribia la categoria de movimientos pasados.
    assert!(category.is_archived());
    assert_eq!(category.id(), before.id());
    assert_eq!(category.name(), before.name());
    assert_eq!(category.kind(), before.kind());
}

#[test]
fn archiving_twice_keeps_the_first_instant() {
    let mut category = custom("Mascotas");
    category.archive(Utc::now()).unwrap();
    let first = category.archived_at();

    category
        .archive(Utc::now() + chrono::Duration::hours(1))
        .unwrap();

    assert_eq!(category.archived_at(), first);
}

#[test]
fn an_archived_category_can_come_back() {
    let mut category = custom("Mascotas");
    category.archive(Utc::now()).unwrap();

    category.unarchive(Utc::now()).unwrap();

    assert!(!category.is_archived());
}

#[test]
fn a_default_category_cannot_be_archived() {
    let mut category = Category::rehydrate(
        CategoryId::generate(),
        NewCategory {
            user_id: UserId::generate(),
            name: CategoryName::parse("Salario").unwrap(),
            kind: CategoryKind::In,
            icon: CategoryIcon::parse("account_balance_wallet").unwrap(),
            color: CategoryColor::parse("#2f8a55").unwrap(),
            created_at: Utc::now(),
        },
        true,
        Utc::now(),
        None,
    );

    assert_eq!(
        category.archive(Utc::now()),
        Err(CategoryError::DefaultIsReadOnly)
    );
    assert!(!category.is_archived());
}

/// Una categoria del usuario con los valores del sheet.
fn custom(name: &str) -> Category {
    Category::create_custom(NewCategory {
        user_id: UserId::generate(),
        name: CategoryName::parse(name).unwrap(),
        kind: CategoryKind::Out,
        icon: CategoryIcon::parse("pets").unwrap(),
        color: CategoryColor::parse("#2f7fb5").unwrap(),
        created_at: Utc::now(),
    })
    .unwrap()
}
