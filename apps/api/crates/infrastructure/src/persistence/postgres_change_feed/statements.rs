//! Consultas del feed de cambios.
//!
//! Las cinco primeras tienen la misma forma: las filas del usuario con numero de
//! cambio mayor que el pedido, en orden de numero y con un tope. Se pide una fila
//! mas de las que caben en el lote, que es como se sabe si queda algo detras sin
//! contar la tabla entera.
//!
//! `$2::bigint IS NULL` es el arranque en frio: sin `since` entran todas.

/// Movimientos cambiados, con su categoria incrustada como en la lista normal.
pub const MOVEMENTS: &str = "SELECT m.id, m.user_id, m.occurred_on, m.kind, m.title, \
     m.amount, m.currency, m.base, m.base_currency, \
     m.loan_id, m.loan_payment_id, m.created_at, m.updated_at, m.change_seq, \
     c.id AS category_id, c.name AS category_name, c.kind AS category_kind, \
     c.icon AS category_icon, c.color AS category_color \
     FROM movements m JOIN categories c ON c.id = m.category_id \
     WHERE m.user_id = $1 AND ($2::bigint IS NULL OR m.change_seq > $2) \
     ORDER BY m.change_seq \
     LIMIT $3";

/// Prestamos cambiados. Sus abonos se leen despues, con [`PAYMENTS_OF_LOANS`].
pub const LOANS: &str = "SELECT id, user_id, person, note, direction, opened_on, \
     amount, currency, base, base_currency, created_at, updated_at, change_seq \
     FROM loans \
     WHERE user_id = $1 AND ($2::bigint IS NULL OR change_seq > $2) \
     ORDER BY change_seq \
     LIMIT $3";

/// Abonos cambiados, que pueden ser de un prestamo mas viejo que el lote.
pub const PAYMENTS: &str = "SELECT id, loan_id, user_id, paid_on, label, \
     amount, currency, base, base_currency, created_at, change_seq \
     FROM loan_payments \
     WHERE user_id = $1 AND ($2::bigint IS NULL OR change_seq > $2) \
     ORDER BY change_seq \
     LIMIT $3";

/// Categorias cambiadas, archivadas incluidas: archivar es un cambio, no un borrado.
pub const CATEGORIES: &str = "SELECT c.id, c.user_id, c.name, c.kind, c.icon, c.color, \
     c.is_default, c.created_at, c.updated_at, c.archived_at, c.change_seq, \
     (SELECT count(*) FROM movements m \
       WHERE m.category_id = c.id AND m.user_id = c.user_id) AS movement_count \
     FROM categories c \
     WHERE c.user_id = $1 AND ($2::bigint IS NULL OR c.change_seq > $2) \
     ORDER BY c.change_seq \
     LIMIT $3";

/// Los ajustes, que son una sola fila y entran solo si cambiaron.
pub const SETTINGS: &str = "SELECT user_id, display_currency, language, updated_at, change_seq \
     FROM user_settings \
     WHERE user_id = $1 AND ($2::bigint IS NULL OR change_seq > $2)";

/// Abonos de los prestamos que entran en el lote, para reconstruirlos completos.
pub const PAYMENTS_OF_LOANS: &str = "SELECT id, loan_id, user_id, paid_on, label, \
     amount, currency, base, base_currency, created_at \
     FROM loan_payments \
     WHERE user_id = $1 AND loan_id = ANY($2) \
     ORDER BY paid_on DESC, created_at DESC, id DESC";

/// Ultimo numero repartido a la cuenta.
///
/// Hace falta para el lote vacio: un cliente al dia tiene que recibir el numero en
/// el que esta, no un cero que le haria volver a bajarlo todo.
pub const LAST_SEQ: &str = "SELECT coalesce(last_seq, 0) AS last_seq \
     FROM user_change_counters WHERE user_id = $1";
