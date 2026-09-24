-- Esquema base de Gastonomo.
--
-- Todavia no hay dominio de negocio, asi que esta migracion solo deja el
-- terreno preparado: la extension para UUID y una tabla de metadatos que
-- registra que version de esquema hay aplicada. Nada de tablas inventadas.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS schema_metadata (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_version  text        NOT NULL,
    applied_at      timestamptz NOT NULL DEFAULT now(),
    singleton       boolean     NOT NULL DEFAULT TRUE,
    CONSTRAINT schema_metadata_singleton CHECK (singleton),
    CONSTRAINT schema_metadata_only_one_row UNIQUE (singleton)
);

COMMENT ON TABLE schema_metadata IS
    'Una sola fila con la version de esquema aplicada. Sin datos de negocio.';

INSERT INTO schema_metadata (schema_version)
VALUES ('0.1.0')
ON CONFLICT (singleton) DO UPDATE
    SET schema_version = EXCLUDED.schema_version,
        applied_at     = now();
