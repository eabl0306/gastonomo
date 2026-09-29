//! Lo que se le pregunta al modelo.

use gastonomo_domain::category::{CategoryKind, FALLBACK_CATEGORY_NAME, LOAN_CATEGORY_NAME};
use gastonomo_domain::dictation::DictationContext;

/// Nombre de la herramienta que el modelo tiene que rellenar.
pub const TOOL_NAME: &str = "registrar";

/// Las instrucciones fijas.
///
/// Se le pide el importe **como texto** a proposito. Un numero JSON pasaria por
/// coma flotante, y el dinero de esta aplicacion es decimal exacto de punta a
/// punta: 186400.50 tiene que llegar tal cual, no como lo mas parecido que quepa
/// en un `f64`.
const SYSTEM: &str = "Eres el interprete de una aplicacion de finanzas personales. \
El usuario dicta UN registro en una frase y tu lo conviertes en campos. \
No inventes lo que no se haya dicho: deja vacio lo que no aparezca, que la persona \
lo completara. El importe va como texto con punto decimal y sin separador de miles.";

/// El mensaje con el contexto de la cuenta y lo que se dicto.
pub fn user_message(text: &str, context: &DictationContext<'_>) -> String {
    let outgoing = names(context, CategoryKind::Out);
    let incoming = names(context, CategoryKind::In);
    format!(
        "Hoy es {today}. La moneda principal de la cuenta es {currency}.\n\
         Categorias de gasto: {outgoing}.\n\
         Categorias de ingreso: {incoming}.\n\
         Texto dictado: {text}",
        today = context.today,
        currency = context.display_currency.as_str(),
    )
}

/// Las instrucciones fijas del sistema.
#[must_use]
pub const fn system_prompt() -> &'static str {
    SYSTEM
}

/// Los nombres que se le ofrecen al modelo para un tipo de movimiento.
///
/// Las de prestamos se quedan fuera: son internas y un movimiento suelto no puede
/// colgarse de ellas. Ofrecerselas solo invita a que las elija.
fn names(context: &DictationContext<'_>, wanted: CategoryKind) -> String {
    let mut chosen: Vec<&str> = context
        .categories
        .iter()
        .filter(|category| serves(category.kind, wanted))
        .map(|category| category.name.as_str())
        .filter(|name| *name != LOAN_CATEGORY_NAME && !name.starts_with("Abono"))
        .collect();
    if chosen.is_empty() {
        chosen.push(FALLBACK_CATEGORY_NAME);
    }
    chosen.join(", ")
}

const fn serves(category: CategoryKind, wanted: CategoryKind) -> bool {
    match category {
        CategoryKind::Both => true,
        CategoryKind::In => matches!(wanted, CategoryKind::In),
        CategoryKind::Out => matches!(wanted, CategoryKind::Out),
    }
}

/// El esquema de la herramienta, que es lo que obliga a que la respuesta venga
/// con forma y no como texto suelto que luego haya que adivinar.
pub fn tool_schema() -> serde_json::Value {
    serde_json::json!({
        "type": "object",
        "properties": {
            "kind": {
                "type": "string",
                "enum": ["movement", "loan"],
                "description": "loan solo si se presta o se pide prestado dinero a alguien"
            },
            "movementKind": {
                "type": "string",
                "enum": ["in", "out"],
                "description": "in si el dinero entra, out si sale. Solo para movement"
            },
            "direction": {
                "type": "string",
                "enum": ["lent", "borrowed"],
                "description": "lent si el usuario presto, borrowed si le prestaron. Solo para loan"
            },
            "amount": {
                "type": "string",
                "description": "Importe con punto decimal y sin separador de miles, por ejemplo 45000 o 12.50. Vacio si no se dijo"
            },
            "currency": { "type": "string", "description": "Codigo ISO 4217, por ejemplo COP o USD" },
            "category": { "type": "string", "description": "Una de las categorias ofrecidas. Solo para movement" },
            "title": { "type": "string", "description": "Descripcion corta, por ejemplo Almuerzo" },
            "person": { "type": "string", "description": "Con quien es el prestamo. Solo para loan" },
            "date": { "type": "string", "description": "Fecha en formato YYYY-MM-DD. Vacio si no se dijo" },
            "note": { "type": "string", "description": "Nota libre si se dijo algo mas" }
        },
        "required": ["kind"]
    })
}
