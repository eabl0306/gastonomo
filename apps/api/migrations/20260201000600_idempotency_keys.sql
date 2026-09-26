-- Claves de idempotencia de los tres POST que crean dinero.
--
-- El cliente manda una cabecera Idempotency-Key por intento del usuario y la repite
-- en cada reintento de ese intento. La fila se reserva antes de ejecutar la
-- operacion y se completa con la respuesta: el segundo intento la repite en vez de
-- escribir un segundo gasto. La cabecera es opcional y sin ella no se toca esta
-- tabla.
--
-- La clave incluye el endpoint: la misma Idempotency-Key en dos operaciones
-- distintas son dos reservas distintas, asi que un cliente que reutilice el valor
-- entre pantallas no se bloquea a si mismo.
--
-- fingerprint es el SHA-256 del cuerpo. Con la misma clave y otro cuerpo no es un
-- reintento sino una reutilizacion, y la API responde 409 en vez de perder uno de
-- los dos registros en silencio.

CREATE TABLE IF NOT EXISTS idempotency_keys (
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint        text        NOT NULL,
    key             text        NOT NULL,
    fingerprint     text        NOT NULL,
    response_status smallint,
    response_body   bytea,
    created_at      timestamptz NOT NULL DEFAULT now(),
    completed_at    timestamptz,
    PRIMARY KEY (user_id, endpoint, key),
    CONSTRAINT idempotency_keys_key_length CHECK (length(key) BETWEEN 1 AND 200),
    CONSTRAINT idempotency_keys_endpoint_length CHECK (length(btrim(endpoint)) BETWEEN 1 AND 120),
    -- O la reserva esta en vuelo (las dos columnas nulas) o esta completa (las dos
    -- puestas). Una fila a medias haria que un reintento repitiera un cuerpo vacio.
    CONSTRAINT idempotency_keys_response_is_whole
        CHECK ((response_status IS NULL) = (response_body IS NULL))
);

COMMENT ON TABLE idempotency_keys IS
    'Respuestas ya emitidas por clave de idempotencia. Se purgan a las 24 horas.';

-- La purga borra por antiguedad dentro de una cuenta, que es como la hace el
-- adaptador al reservar una clave nueva.
CREATE INDEX IF NOT EXISTS idempotency_keys_created_at_idx
    ON idempotency_keys (user_id, created_at);
