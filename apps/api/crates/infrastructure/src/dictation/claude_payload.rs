//! Lo que viaja hacia el modelo y lo que vuelve.

use chrono::NaiveDate;
use gastonomo_domain::dictation::{Interpretation, InterpretedKind};
use gastonomo_domain::value_object::Money;
use serde::Deserialize;

/// La respuesta del modelo.
///
/// **Todo es opcional**, igual que en el payload de las tasas y por la misma razon
/// llevada al extremo: al otro lado hay un modelo de lenguaje. Un campo que no
/// llega, o que llega con otra forma, tiene que poder descartarse solo y dejar un
/// borrador incompleto que la persona completa, no tumbar la peticion entera.
#[derive(Debug, Deserialize)]
pub struct ClaudeResponse {
    #[serde(default)]
    content: Vec<ContentBlock>,
}

#[derive(Debug, Deserialize)]
struct ContentBlock {
    #[serde(rename = "type", default)]
    kind: String,
    #[serde(default)]
    name: String,
    #[serde(default)]
    input: Option<ToolInput>,
}

/// Los campos que rellena la herramienta.
#[derive(Debug, Default, Deserialize)]
#[serde(rename_all = "camelCase")]
struct ToolInput {
    #[serde(default)]
    kind: Option<String>,
    #[serde(default)]
    movement_kind: Option<String>,
    #[serde(default)]
    direction: Option<String>,
    /// Texto y nunca numero: el dinero es decimal exacto y no puede pasar por
    /// coma flotante ni un instante.
    #[serde(default)]
    amount: Option<String>,
    #[serde(default)]
    currency: Option<String>,
    #[serde(default)]
    category: Option<String>,
    #[serde(default)]
    title: Option<String>,
    #[serde(default)]
    person: Option<String>,
    #[serde(default)]
    date: Option<String>,
    #[serde(default)]
    note: Option<String>,
}

impl ClaudeResponse {
    /// Lee la respuesta y la pasa a un tipo de dominio.
    ///
    /// Si el modelo no uso la herramienta, o la uso con cualquier cosa, sale una
    /// interpretacion vacia: un gasto de hoy sin importe. El usuario ve la tarjeta
    /// de revision pidiendole el monto, que es mucho mejor que un error.
    #[must_use]
    pub fn into_interpretation(self, tool_name: &str) -> Interpretation {
        let input = self
            .content
            .into_iter()
            .find(|block| block.kind == "tool_use" && block.name == tool_name)
            .and_then(|block| block.input)
            .unwrap_or_default();
        Interpretation {
            kind: match input.kind.as_deref() {
                Some("loan") => InterpretedKind::Loan,
                _ => InterpretedKind::Movement,
            },
            incoming: input.movement_kind.as_deref().map(|value| value == "in"),
            lent: input.direction.as_deref().map(|value| value != "borrowed"),
            amount: input.amount.as_deref().and_then(parse_amount),
            currency: clean(input.currency),
            category: clean(input.category),
            title: clean(input.title),
            person: clean(input.person),
            occurred_on: input.date.as_deref().and_then(parse_date),
            note: clean(input.note),
        }
    }
}

/// El importe tal cual lo escribio el modelo.
///
/// Un importe que no se puede leer vale lo mismo que no haberlo dicho: se queda en
/// `None` y el borrador pide que lo completen. Adivinarlo seria peor.
fn parse_amount(raw: &str) -> Option<Money> {
    let trimmed = raw.trim();
    if trimmed.is_empty() {
        return None;
    }
    Money::parse(trimmed)
        .ok()
        .filter(|amount| amount.is_positive())
}

fn parse_date(raw: &str) -> Option<NaiveDate> {
    NaiveDate::parse_from_str(raw.trim(), "%Y-%m-%d").ok()
}

fn clean(raw: Option<String>) -> Option<String> {
    raw.map(|value| value.trim().to_owned())
        .filter(|value| !value.is_empty())
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::*;

    fn read(body: &str) -> Interpretation {
        serde_json::from_str::<ClaudeResponse>(body)
            .expect("el cuerpo del test es json valido")
            .into_interpretation("registrar")
    }

    #[test]
    fn a_complete_answer_becomes_an_interpretation() {
        let interpretation = read(
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{
                "kind":"movement","movementKind":"out","amount":"45000",
                "currency":"COP","category":"Alimentación","title":"Almuerzo",
                "date":"2026-09-28"}}]}"#,
        );

        assert_eq!(interpretation.kind, InterpretedKind::Movement);
        assert_eq!(interpretation.incoming, Some(false));
        assert_eq!(interpretation.amount, Money::parse("45000").ok());
        assert_eq!(interpretation.category.as_deref(), Some("Alimentación"));
    }

    /// El dinero es decimal exacto: un importe con decimales tiene que llegar tal
    /// cual, no como lo mas parecido que quepa en una coma flotante.
    #[test]
    fn the_amount_keeps_its_decimals() {
        let interpretation = read(
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{
                "kind":"movement","amount":"186400.5001"}}]}"#,
        );

        assert_eq!(interpretation.amount, Money::parse("186400.5001").ok());
    }

    /// Lo que no se puede leer se queda vacio y lo completa la persona. Adivinarlo
    /// seria guardar un importe que nadie dijo.
    #[test]
    fn an_unreadable_amount_is_left_empty() {
        for body in [
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{"kind":"movement","amount":"un monton"}}]}"#,
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{"kind":"movement","amount":""}}]}"#,
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{"kind":"movement","amount":"-5"}}]}"#,
        ] {
            assert_eq!(read(body).amount, None, "con {body}");
        }
    }

    /// Si el modelo contesta texto en vez de usar la herramienta, sale un gasto de
    /// hoy sin importe: la tarjeta de revision pide el monto y nadie ve un error.
    #[test]
    fn an_answer_without_the_tool_still_gives_something_to_review() {
        let interpretation = read(r#"{"content":[{"type":"text","text":"no se que quieres"}]}"#);

        assert_eq!(interpretation.kind, InterpretedKind::Movement);
        assert_eq!(interpretation.amount, None);
        assert_eq!(interpretation.category, None);
    }

    #[test]
    fn a_loan_keeps_its_direction_and_person() {
        let interpretation = read(
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{
                "kind":"loan","direction":"borrowed","person":" Rosa ","amount":"200000"}}]}"#,
        );

        assert_eq!(interpretation.kind, InterpretedKind::Loan);
        assert_eq!(interpretation.lent, Some(false));
        assert_eq!(interpretation.person.as_deref(), Some("Rosa"));
    }

    #[test]
    fn a_date_that_is_not_a_date_is_ignored() {
        let interpretation = read(
            r#"{"content":[{"type":"tool_use","name":"registrar","input":{
                "kind":"movement","date":"ayer"}}]}"#,
        );

        assert_eq!(interpretation.occurred_on, None);
    }
}
