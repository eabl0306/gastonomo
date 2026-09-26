//! Caso de uso: registrar un movimiento.

use std::fmt;
use std::sync::Arc;

use chrono::NaiveDate;
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementError, MovementKind, MovementTitle,
    NewMovement, parse_record_date,
};
use gastonomo_domain::{CurrencyCode, MonetaryAmount, Money, UserId};

use crate::error::{AppError, ValidationErrors};
use crate::port::Clock;
use crate::port::movement::{MovementCategoryReader, MovementRepository};
use crate::use_case::validation_message::{
    monetary_amount_field, monetary_amount_message, movement_title_message,
};

/// Lo que manda el formulario de Nuevo movimiento.
///
/// Llega en crudo, tal cual lo escribio el cliente, y es este caso de uso el que lo
/// valida: asi un cuerpo con tres errores devuelve los tres de golpe y el adaptador
/// HTTP no tiene que saber nada de dinero ni de monedas.
///
/// `base` y `baseCurrency` se pueden omitir juntos. Es el caso del 95% de los
/// registros, el del usuario que teclea en su moneda principal: entonces la
/// equivalencia es el propio importe. Si se manda uno hay que mandar los dos, y la
/// API los guarda tal cual, incluida la tasa que el usuario haya editado a mano.
#[derive(Debug, Clone, Default)]
pub struct CreateMovementCommand {
    /// Fecha del registro en `YYYY-MM-DD`.
    pub occurred_on: String,
    /// `in` o `out`.
    pub kind: String,
    /// Categoria elegida en el grid.
    pub category_id: String,
    /// Nota opcional. Si va vacia, el titulo es el nombre de la categoria.
    pub note: Option<String>,
    /// Importe tecleado, como string.
    pub amount: String,
    /// Moneda del importe tecleado.
    pub currency: String,
    /// Equivalencia en la moneda principal, como string.
    pub base: Option<String>,
    /// Moneda de la equivalencia.
    pub base_currency: Option<String>,
}

/// Campos del comando ya convertidos a tipos del dominio.
struct Parsed {
    occurred_on: NaiveDate,
    kind: MovementKind,
    category_id: CategoryId,
    amount: Money,
    currency: CurrencyCode,
    base: Option<Money>,
    base_currency: Option<CurrencyCode>,
}

/// Registra un ingreso o un gasto.
#[derive(Clone)]
pub struct CreateMovement {
    movements: Arc<dyn MovementRepository>,
    categories: Arc<dyn MovementCategoryReader>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for CreateMovement {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("CreateMovement").finish_non_exhaustive()
    }
}

impl CreateMovement {
    /// Crea el caso de uso con sus puertos.
    #[must_use]
    pub const fn new(
        movements: Arc<dyn MovementRepository>,
        categories: Arc<dyn MovementCategoryReader>,
        clock: Arc<dyn Clock>,
    ) -> Self {
        Self {
            movements,
            categories,
            clock,
        }
    }

    /// Guarda el movimiento y lo devuelve ya con su identificador.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si algun campo no vale, si la categoria no
    /// es de la cuenta o si su tipo no encaja con el del movimiento, y
    /// [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        user_id: UserId,
        command: CreateMovementCommand,
    ) -> Result<Movement, AppError> {
        let parsed = parse(&command)?;
        let category = self.category(user_id, parsed.category_id).await?;
        let movement = self.assemble(user_id, &command, &parsed, category)?;
        self.movements.insert(&movement).await?;
        Ok(movement)
    }

    async fn category(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<MovementCategory, AppError> {
        self.categories
            .find(user_id, category_id)
            .await?
            .ok_or_else(|| AppError::validation("categoryId", "La categoria no existe."))
    }

    fn assemble(
        &self,
        user_id: UserId,
        command: &CreateMovementCommand,
        parsed: &Parsed,
        category: MovementCategory,
    ) -> Result<Movement, AppError> {
        let amount = monetary_amount(parsed)?;
        let title = MovementTitle::from_note_or_category(command.note.as_deref(), category.name())
            .map_err(|error| AppError::validation("note", movement_title_message(&error)))?;
        let now = self.clock.now();
        Movement::record(
            NewMovement {
                user_id,
                occurred_on: parsed.occurred_on,
                kind: parsed.kind,
                category,
                title,
                amount,
                loan_id: None,
                loan_payment_id: None,
                created_at: now,
            },
            self.clock.today(),
        )
        .map_err(|error| movement_error(&error))
    }
}

/// Convierte el comando en tipos del dominio, juntando todos los fallos.
///
/// Recoge un error por campo en vez de parar en el primero: el formulario tiene
/// monto, moneda, categoria y fecha, y devolver los tres fallos de golpe evita tres
/// viajes.
fn parse(command: &CreateMovementCommand) -> Result<Parsed, AppError> {
    let mut errors = ValidationErrors::new();
    let occurred_on = parse_record_date(&command.occurred_on)
        .inspect_err(|_| errors.push("occurredOn", "La fecha tiene que ser YYYY-MM-DD."))
        .ok();
    let kind = MovementKind::parse(&command.kind)
        .inspect_err(|_| errors.push("kind", "El tipo tiene que ser `in` o `out`."))
        .ok();
    let category_id = CategoryId::parse(&command.category_id)
        .inspect_err(|_| errors.push("categoryId", "La categoria no es un identificador."))
        .ok();
    let amount = parse_money(
        &mut errors,
        "amount",
        Some(command.amount.as_str()),
        "El importe tiene que ser mayor que cero.",
    );
    let currency = parse_currency(&mut errors, "currency", Some(command.currency.as_str()));
    let base = parse_money(
        &mut errors,
        "base",
        command.base.as_deref(),
        "La equivalencia tiene que ser mayor que cero.",
    );
    let base_currency = parse_currency(
        &mut errors,
        "baseCurrency",
        command.base_currency.as_deref(),
    );
    if command.base.is_some() != command.base_currency.is_some() {
        errors.push("base", "La equivalencia necesita importe y moneda.");
    }
    errors.into_result()?;
    required(
        Required {
            occurred_on,
            kind,
            category_id,
            amount,
            currency,
        },
        base,
        base_currency,
    )
}

/// Los cinco campos obligatorios, ya leidos.
struct Required {
    occurred_on: Option<NaiveDate>,
    kind: Option<MovementKind>,
    category_id: Option<CategoryId>,
    amount: Option<Money>,
    currency: Option<CurrencyCode>,
}

/// Comprueba que los obligatorios estan, que es cierto en cuanto no hubo errores.
fn required(
    fields: Required,
    base: Option<Money>,
    base_currency: Option<CurrencyCode>,
) -> Result<Parsed, AppError> {
    let (Some(occurred_on), Some(kind), Some(category_id), Some(amount), Some(currency)) = (
        fields.occurred_on,
        fields.kind,
        fields.category_id,
        fields.amount,
        fields.currency,
    ) else {
        return Err(AppError::validation("body", "Faltan campos obligatorios."));
    };
    Ok(Parsed {
        occurred_on,
        kind,
        category_id,
        amount,
        currency,
        base,
        base_currency,
    })
}

/// Lee un importe positivo.
///
/// `not_positive` llega de fuera porque el mismo helper valida el importe tecleado
/// y su equivalencia, y el formulario los pinta en dos cajas distintas: decirle
/// "el importe" en la caja de la conversion despista.
fn parse_money(
    errors: &mut ValidationErrors,
    field: &str,
    raw: Option<&str>,
    not_positive: &str,
) -> Option<Money> {
    let parsed = Money::parse(raw?)
        .inspect_err(|_| errors.push(field, "El importe no es un numero."))
        .ok()?;
    if parsed.is_positive() {
        Some(parsed)
    } else {
        errors.push(field, not_positive);
        None
    }
}

fn parse_currency(
    errors: &mut ValidationErrors,
    field: &str,
    raw: Option<&str>,
) -> Option<CurrencyCode> {
    CurrencyCode::parse(raw?)
        .inspect_err(|_| errors.push(field, "La moneda tiene que ser un codigo ISO 4217."))
        .ok()
}

/// Monta el par de importes, que es donde vive la invariante del diseno.
fn monetary_amount(parsed: &Parsed) -> Result<MonetaryAmount, AppError> {
    match (parsed.base, parsed.base_currency.clone()) {
        (Some(base), Some(base_currency)) => {
            MonetaryAmount::new(parsed.amount, parsed.currency.clone(), base, base_currency)
        }
        _ => MonetaryAmount::single_currency(parsed.amount, parsed.currency.clone()),
    }
    .map_err(|error| {
        AppError::validation(
            monetary_amount_field(&error),
            monetary_amount_message(&error),
        )
    })
}

fn movement_error(error: &MovementError) -> AppError {
    match error {
        MovementError::CategoryKindMismatch { .. } => AppError::validation(
            "categoryId",
            "La categoria no sirve para este tipo de movimiento.",
        ),
        MovementError::RecordDate(_) => {
            AppError::validation("occurredOn", "La fecha no es razonable.")
        }
    }
}
