-- Ajustes sincronizados del usuario.
--
-- Solo viajan la moneda de visualizacion y el idioma. El bloqueo por PIN o
-- huella y el ojo que oculta los montos son estado del dispositivo y no tienen
-- columna aqui a proposito.

CREATE TABLE IF NOT EXISTS user_settings (
    user_id          uuid        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    display_currency char(3)     NOT NULL DEFAULT 'COP',
    language         text        NOT NULL DEFAULT 'es',
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_settings_currency_shape CHECK (display_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT user_settings_language_allowed CHECK (language IN ('es', 'en'))
);

COMMENT ON TABLE user_settings IS
    'Una fila por usuario. Cambiar display_currency no toca ningun registro guardado.';
