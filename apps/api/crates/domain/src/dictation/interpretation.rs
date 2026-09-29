//! Lo que el interprete entendio, antes de comprobarlo contra la cuenta.

use chrono::NaiveDate;

use crate::category::{CategoryId, CategoryKind};
use crate::value_object::{CurrencyCode, Money};

/// Si lo dictado es un movimiento o un prestamo.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InterpretedKind {
    /// Un ingreso o un gasto.
    Movement,
    /// Un prestamo, en cualquiera de los dos sentidos.
    Loan,
}

/// El resultado en bruto del interprete.
///
/// Todo es opcional a proposito. Un modelo puede devolver cualquier cosa, y un
/// campo que no se entiende tiene que poder descartarse solo en vez de tumbar la
/// peticion entera: el usuario ve el borrador a medias y lo completa a mano, que es
/// mucho mejor que un error sin nada que revisar.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Interpretation {
    /// Movimiento o prestamo.
    pub kind: InterpretedKind,
    /// Ingreso o gasto. En un prestamo se ignora: lo decide el sentido.
    pub incoming: Option<bool>,
    /// Presto el usuario (`true`) o le prestaron (`false`).
    pub lent: Option<bool>,
    /// Importe. `None` es "no lo entendi", y acaba en [`super::MissingField`].
    pub amount: Option<Money>,
    /// Codigo de moneda tal cual lo dijo el modelo, sin comprobar.
    pub currency: Option<String>,
    /// Nombre de categoria tal cual lo dijo el modelo, sin comprobar.
    pub category: Option<String>,
    /// Descripcion corta del movimiento.
    pub title: Option<String>,
    /// Con quien es el prestamo.
    pub person: Option<String>,
    /// Fecha del registro. Una futura o ilegible se sustituye por hoy.
    pub occurred_on: Option<NaiveDate>,
    /// Nota libre.
    pub note: Option<String>,
}

/// Una categoria de la cuenta, con lo justo para poder elegirla.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AvailableCategory {
    /// Identificador que el cliente mandara despues al crear el registro.
    pub id: CategoryId,
    /// Nombre visible, que es contra lo que se compara lo dictado.
    pub name: String,
    /// Para que tipo de movimiento sirve.
    pub kind: CategoryKind,
}

/// Lo que la cuenta aporta para poder comprobar lo dictado.
///
/// Sale del servidor y no del cliente a proposito: son datos que la API ya tiene, y
/// dejar que los mande quien llama abre la puerta a que los dos no coincidan.
#[derive(Debug, Clone)]
pub struct DictationContext<'a> {
    /// Moneda principal de la cuenta, a la que se cae lo que no se puede convertir.
    pub display_currency: CurrencyCode,
    /// Monedas con tasa conocida hoy. Una fuera de aqui no se puede equivaler.
    pub convertible: &'a [CurrencyCode],
    /// Categorias de la cuenta, incluidas las suyas.
    pub categories: &'a [AvailableCategory],
    /// Hoy segun el dispositivo que dicta, que es quien sabe su zona horaria.
    pub today: NaiveDate,
}
