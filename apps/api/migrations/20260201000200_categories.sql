-- Categorias de movimientos.
--
-- Las 12 predeterminadas se siembran al registrarse con is_default = true y no
-- se pueden editar ni borrar. El nombre es unico entre todas las categorias del
-- usuario, sin distinguir tipo ni mayusculas.
--
-- kind admite un tercer valor, 'both', que el analisis no contemplaba. Hace falta
-- porque el prototipo ensena Prestamo, Abono prestamo y Otros en el grid de gasto
-- y en el de ingreso, y a la vez el nombre no puede repetirse: con solo 'in' y
-- 'out' esas tres categorias tendrian que existir dos veces. Solo lo pueden usar
-- las predeterminadas; el sheet de categoria del usuario ofrece Gasto o Ingreso.

CREATE TABLE IF NOT EXISTS categories (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       text        NOT NULL,
    kind       text        NOT NULL,
    icon       text        NOT NULL,
    color      text        NOT NULL,
    is_default boolean     NOT NULL DEFAULT FALSE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT categories_kind_allowed CHECK (kind IN ('in', 'out', 'both')),
    CONSTRAINT categories_both_only_for_defaults CHECK (kind <> 'both' OR is_default),
    CONSTRAINT categories_name_length CHECK (length(btrim(name)) BETWEEN 1 AND 22),
    CONSTRAINT categories_color_shape CHECK (color ~ '^#[0-9a-fA-F]{6}$')
);

COMMENT ON TABLE categories IS
    'Categorias por usuario. Las predeterminadas llevan is_default = true.';

CREATE UNIQUE INDEX IF NOT EXISTS categories_user_name_unique_idx
    ON categories (user_id, lower(name));
CREATE INDEX IF NOT EXISTS categories_user_kind_idx
    ON categories (user_id, kind);
