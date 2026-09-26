//! Mensajes en castellano de las invariantes de dominio que toca un formulario.
//!
//! El contrato fija que `errors[].message` viaja en castellano, listo para pintarlo
//! junto al campo sin traducir nada. El `Display` de los errores de dominio se
//! queda en ingles porque habla con quien lee los logs, asi que no se puede
//! reenviar tal cual al cliente.
//!
//! Vive aqui, y no dentro de un contexto, porque el par de importes lo validan
//! igual los movimientos, los prestamos y los abonos: si cada uno escribiera su
//! propia traduccion, el mismo error se veria distinto en cada formulario.

use gastonomo_domain::MonetaryAmountError;
use gastonomo_domain::movement::{MAX_TITLE_LENGTH, MovementTitleError};

/// Campo del JSON al que se atribuye cada invariante del par de importes.
pub const fn monetary_amount_field(error: &MonetaryAmountError) -> &'static str {
    match error {
        MonetaryAmountError::AmountNotPositive => "amount",
        MonetaryAmountError::BaseNotPositive | MonetaryAmountError::InconsistentSingleCurrency => {
            "base"
        }
    }
}

/// Mensaje que ve el usuario cuando el par de importes no cuadra.
pub const fn monetary_amount_message(error: &MonetaryAmountError) -> &'static str {
    match error {
        MonetaryAmountError::AmountNotPositive => "El importe tiene que ser mayor que cero.",
        MonetaryAmountError::BaseNotPositive => "La equivalencia tiene que ser mayor que cero.",
        MonetaryAmountError::InconsistentSingleCurrency => {
            "Con una sola moneda el importe y su equivalencia tienen que coincidir."
        }
    }
}

/// Mensaje que ve el usuario cuando la nota del movimiento no vale como titulo.
///
/// El limite se lee del dominio para que el numero del mensaje no se quede atras
/// si alguna vez cambia.
pub fn movement_title_message(error: &MovementTitleError) -> String {
    match error {
        MovementTitleError::Empty => "La nota no puede estar vacia.".to_owned(),
        MovementTitleError::TooLong => format!("La nota admite {MAX_TITLE_LENGTH} caracteres."),
    }
}
