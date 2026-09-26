//! Calendario del refresco: cuando toca la siguiente pasada.
//!
//! Esta separado del bucle porque es aritmetica de fechas y se puede probar sin
//! esperar doce horas ni arrancar un runtime.

use chrono::{DateTime, NaiveDate, TimeZone as _, Utc};

/// Instante UTC de una hora en punto de un dia.
fn at(date: NaiveDate, hour: u32) -> Option<DateTime<Utc>> {
    date.and_hms_opt(hour, 0, 0)
        .map(|naive| Utc.from_utc_datetime(&naive))
}

/// Siguiente instante en que toca refrescar, dadas las horas UTC.
///
/// Devuelve `None` solo si la lista de horas viene vacia, lo que la
/// configuracion ya impide.
#[must_use]
pub fn next_tick_after(now: DateTime<Utc>, hours: &[u32]) -> Option<DateTime<Utc>> {
    let today = now.date_naive();
    hours
        .iter()
        .filter_map(|hour| at(today, *hour))
        .find(|candidate| *candidate > now)
        .or_else(|| {
            let tomorrow = today.succ_opt()?;
            hours.first().and_then(|hour| at(tomorrow, *hour))
        })
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use chrono::{DateTime, Utc};

    use super::next_tick_after;

    const HOURS: [u32; 2] = [6, 18];

    fn instant(raw: &str) -> DateTime<Utc> {
        DateTime::parse_from_rfc3339(raw)
            .expect("literal is valid")
            .with_timezone(&Utc)
    }

    #[test]
    fn before_the_first_hour_the_next_tick_is_that_same_morning() {
        assert_eq!(
            next_tick_after(instant("2026-09-25T03:12:00Z"), &HOURS),
            Some(instant("2026-09-25T06:00:00Z"))
        );
    }

    #[test]
    fn between_the_two_hours_the_next_tick_is_the_evening() {
        assert_eq!(
            next_tick_after(instant("2026-09-25T06:00:01Z"), &HOURS),
            Some(instant("2026-09-25T18:00:00Z"))
        );
    }

    #[test]
    fn after_the_last_hour_the_next_tick_is_tomorrow_morning() {
        assert_eq!(
            next_tick_after(instant("2026-09-25T23:59:00Z"), &HOURS),
            Some(instant("2026-09-26T06:00:00Z"))
        );
    }

    #[test]
    fn exactly_on_the_hour_the_tick_does_not_repeat_itself() {
        assert_eq!(
            next_tick_after(instant("2026-09-25T18:00:00Z"), &HOURS),
            Some(instant("2026-09-26T06:00:00Z"))
        );
    }

    #[test]
    fn the_end_of_the_month_rolls_over() {
        assert_eq!(
            next_tick_after(instant("2026-09-30T19:00:00Z"), &HOURS),
            Some(instant("2026-10-01T06:00:00Z"))
        );
    }

    #[test]
    fn a_single_hour_works_too() {
        assert_eq!(
            next_tick_after(instant("2026-09-25T09:00:00Z"), &[6]),
            Some(instant("2026-09-26T06:00:00Z"))
        );
    }

    #[test]
    fn without_hours_there_is_no_next_tick() {
        assert_eq!(next_tick_after(instant("2026-09-25T09:00:00Z"), &[]), None);
    }
}
