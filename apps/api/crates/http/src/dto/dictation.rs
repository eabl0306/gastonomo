//! DTO del dictado.

mod parse_request;
mod response;

pub use parse_request::ParseDictationRequestDto;
pub use response::{DictatedLoanDto, DictatedMovementDto, DictationDraftDto, DraftCategoryDto};
