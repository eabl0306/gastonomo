//! Consultas del repositorio de prestamos.
//!
//! Estan aqui como constantes porque SQLx 0.9 solo acepta SQL literal: no se puede
//! construir la consulta con `format!`, asi que la lista de columnas se repite en
//! cada sentencia y conviene tenerlas juntas para leerlas de una vez.

/// Prestamos de una cuenta, con el filtro de direccion opcional.
///
/// El `$2::text IS NULL` es lo que permite tener una sola consulta para las tres
/// pills del diseno (Todos, Me deben, Debo).
pub const LOANS_OF_USER: &str = "SELECT id, user_id, person, note, direction, opened_on, \
     amount, currency, base, base_currency, created_at, updated_at \
     FROM loans \
     WHERE user_id = $1 AND ($2::text IS NULL OR direction = $2) \
     ORDER BY opened_on DESC, id DESC";

/// Un prestamo concreto de una cuenta.
///
/// Filtra tambien por `user_id`: un prestamo de otra cuenta no se distingue de uno
/// que no existe.
pub const LOAN_OF_USER: &str = "SELECT id, user_id, person, note, direction, opened_on, \
     amount, currency, base, base_currency, created_at, updated_at \
     FROM loans \
     WHERE user_id = $1 AND id = $2";

/// Abonos de un conjunto de prestamos, del mas reciente al mas antiguo.
pub const PAYMENTS_OF_LOANS: &str = "SELECT id, loan_id, user_id, paid_on, label, \
     amount, currency, base, base_currency, created_at \
     FROM loan_payments \
     WHERE user_id = $1 AND loan_id = ANY($2) \
     ORDER BY paid_on DESC, created_at DESC, id DESC";

/// Alta de un prestamo.
pub const INSERT_LOAN: &str = "INSERT INTO loans \
     (id, user_id, person, note, direction, opened_on, \
      amount, currency, base, base_currency, created_at, updated_at) \
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $11)";

/// Alta de un abono.
pub const INSERT_PAYMENT: &str = "INSERT INTO loan_payments \
     (id, loan_id, user_id, paid_on, label, amount, currency, base, base_currency, created_at) \
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)";

/// Alta del movimiento que acompana al prestamo o al abono.
pub const INSERT_MOVEMENT: &str = "INSERT INTO movements \
     (id, user_id, occurred_on, kind, category_id, title, \
      amount, currency, base, base_currency, loan_id, loan_payment_id, created_at, updated_at) \
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $13)";

/// Categoria predeterminada por nombre, sin distinguir mayusculas.
///
/// El movimiento generado se cuelga de una categoria del usuario, y el contexto de
/// prestamos solo conoce su nombre: las categorias son de otro contexto. Resolver
/// el identificador aqui, dentro de la transaccion, evita tener que exponer el
/// modelo de categorias en el puerto.
pub const CATEGORY_BY_NAME: &str = "SELECT id FROM categories \
     WHERE user_id = $1 AND lower(name) = lower($2) LIMIT 1";
