-- Archivar categorias en vez de borrarlas.
--
-- Borrar una categoria ejecutaba `UPDATE movements SET category_id = <Otros>`, y
-- eso era el ultimo sitio por el que se podia alterar como se lee el historico:
-- los importes no cambiaban, pero un gasto de hace dos anos pasaba a leerse con
-- otra categoria. El historico es de solo anadir a proposito, asi que ese camino
-- no puede existir.
--
-- Ahora la categoria se marca inactiva y se queda donde esta. Los movimientos
-- siguen apuntando a ella para siempre.
--
-- El nombre sigue siendo unico incluyendo las archivadas, y es deliberado: el
-- nombre tiene que seguir resolviendo a la categoria que lo llevaba. Por eso hace
-- falta poder desarchivar, en vez de crear otra igual.

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS archived_at timestamptz NULL;

COMMENT ON COLUMN categories.archived_at IS
    'Cuando se archivo. NULL mientras esta activa. Las predeterminadas nunca se archivan.';

-- Una predeterminada no se puede archivar, igual que no se puede editar ni borrar.
ALTER TABLE categories
    DROP CONSTRAINT IF EXISTS categories_defaults_never_archived;
ALTER TABLE categories
    ADD CONSTRAINT categories_defaults_never_archived
    CHECK (archived_at IS NULL OR is_default = FALSE);

-- El listado por defecto solo trae las activas.
CREATE INDEX IF NOT EXISTS categories_user_active_idx
    ON categories (user_id) WHERE archived_at IS NULL;
