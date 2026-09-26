//! Categorias de movimientos.
//!
//! Aqui vive lo que comparten todos los contextos: el tipo de categoria y el
//! catalogo de las predeterminadas, que el registro siembra y que el contexto de
//! categorias amplia con las del usuario.

mod aggregate;
mod default_catalog;
mod identifier;
mod kind;
mod name;
mod style;

pub use aggregate::{Category, CategoryChange, CategoryError, NewCategory};
pub use default_catalog::{
    DEFAULT_CATEGORIES, DefaultCategory, FALLBACK_CATEGORY_NAME, LOAN_CATEGORY_NAME,
    LOAN_PAYMENT_CATEGORY_NAME,
};
pub use identifier::{CategoryId, CategoryIdError};
pub use kind::{CategoryKind, CategoryKindError};
pub use name::{CategoryName, CategoryNameError, MAX_CATEGORY_NAME_LENGTH};
pub use style::{
    CUSTOM_CATEGORY_COLORS, CUSTOM_CATEGORY_ICONS, CategoryColor, CategoryColorError, CategoryIcon,
    CategoryIconError,
};
