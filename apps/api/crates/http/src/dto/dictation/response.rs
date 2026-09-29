//! Respuesta de `POST /v1/dictation/parse`.

use gastonomo_domain::dictation::{DictatedEntry, DictationDraft};
use gastonomo_domain::value_object::Money;
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

use crate::serialization::date_to_string;

/// La categoria elegida para el borrador.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DraftCategoryDto {
    /// Identificador, que es lo que el cliente manda al crear el movimiento.
    #[schema(example = "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34")]
    pub id: String,
    /// Nombre, para la tarjeta de revision.
    #[schema(example = "Alimentación")]
    pub name: String,
}

/// Lo propio de un movimiento dictado.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DictatedMovementDto {
    /// `in` para un ingreso, `out` para un gasto.
    #[schema(example = "out")]
    pub kind: String,
    /// Categoria de la cuenta, siempre una que existe.
    pub category: DraftCategoryDto,
    /// Descripcion corta.
    #[schema(example = "Almuerzo")]
    pub note: String,
}

/// Lo propio de un prestamo dictado.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DictatedLoanDto {
    /// `lent` si el usuario presto, `borrowed` si le prestaron.
    #[schema(example = "lent")]
    pub direction: String,
    /// Con quien. Vacio si no se entendio, y entonces sale en `missing`.
    #[schema(example = "Rosa")]
    pub person: String,
    /// Nota libre.
    pub note: String,
}

/// El borrador que el usuario revisa antes de guardar.
///
/// **No se ha creado nada.** Confirmar es una llamada aparte a `POST /v1/movements`
/// o `POST /v1/loans`, con su `Idempotency-Key`: el historico es de solo anadir y
/// no puede depender de que un modelo acierte a la primera.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct DictationDraftDto {
    /// `movement` o `loan`. Dice cual de los dos objetos de abajo viene relleno.
    #[schema(example = "movement")]
    pub kind: String,
    /// Relleno si `kind` es `movement`.
    pub movement: Option<DictatedMovementDto>,
    /// Relleno si `kind` es `loan`.
    pub loan: Option<DictatedLoanDto>,
    /// Importe, o `null` si no se entendio. Siempre string, nunca un numero.
    #[schema(example = "45000.0000")]
    pub amount: Option<String>,
    /// Moneda del registro, siempre una que se puede equivaler hoy.
    #[schema(example = "COP")]
    pub currency: String,
    /// Fecha del registro, nunca futura.
    #[schema(example = "2026-09-29")]
    pub occurred_on: String,
    /// Campos que el usuario tiene que completar para poder guardar.
    ///
    /// El cliente apaga el boton de guardar mientras no este vacio. Que lo diga la
    /// API y no cada cliente es lo que evita que el movil y la web no coincidan.
    #[schema(example = json!(["amount"]))]
    pub missing: Vec<String>,
}

impl From<&DictationDraft> for DictationDraftDto {
    fn from(draft: &DictationDraft) -> Self {
        let (kind, movement, loan) = match &draft.entry {
            DictatedEntry::Movement(entry) => (
                "movement",
                Some(DictatedMovementDto {
                    kind: entry.kind.as_str().to_owned(),
                    category: DraftCategoryDto {
                        id: entry.category.id.as_uuid().to_string(),
                        name: entry.category.name.clone(),
                    },
                    note: entry.note.clone(),
                }),
                None,
            ),
            DictatedEntry::Loan(entry) => (
                "loan",
                None,
                Some(DictatedLoanDto {
                    direction: entry.direction.as_str().to_owned(),
                    person: entry.person.clone(),
                    note: entry.note.clone(),
                }),
            ),
        };
        Self {
            kind: kind.to_owned(),
            movement,
            loan,
            amount: draft.amount.map(Money::to_storage_string),
            currency: draft.currency.as_str().to_owned(),
            occurred_on: date_to_string(draft.occurred_on),
            missing: draft
                .missing
                .iter()
                .map(|field| field.as_str().to_owned())
                .collect(),
        }
    }
}
