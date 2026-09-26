-- Usuarios y autenticacion.
--
-- El correo usa citext para que la unicidad no distinga mayusculas sin tener que
-- recordar un lower() en cada consulta. Los tokens se guardan hasheados: si
-- alguien lee la tabla no puede usarlos.

CREATE EXTENSION IF NOT EXISTS "citext";

CREATE TABLE IF NOT EXISTS users (
    id            uuid        PRIMARY KEY,
    name          text        NOT NULL,
    email         citext      NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_name_not_blank CHECK (length(btrim(name)) BETWEEN 1 AND 80),
    CONSTRAINT users_email_shape CHECK (position('@' IN email) > 1)
);

COMMENT ON TABLE users IS
    'Cuentas de la app. El hash es Argon2id en formato PHC.';

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  text        NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    replaced_by uuid        REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    user_agent  text,
    created_at  timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE refresh_tokens IS
    'Familias de sesion. Al refrescar se revoca el viejo y replaced_by apunta al nuevo; '
    'reutilizar uno ya rotado revoca la familia entera.';

CREATE INDEX IF NOT EXISTS refresh_tokens_user_id_idx
    ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS refresh_tokens_expires_at_idx
    ON refresh_tokens (expires_at);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash text        NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE password_reset_tokens IS
    'Enlaces de recuperacion: caducan en 30 minutos y son de un solo uso.';

CREATE INDEX IF NOT EXISTS password_reset_tokens_user_id_idx
    ON password_reset_tokens (user_id);
