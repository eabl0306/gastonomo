-- Numero de cambio por usuario, para que un cliente pueda pedir "lo que ha
-- pasado desde la ultima vez" sin bajarse el historico entero.
--
-- El cursor NO es una marca de tiempo. Dos filas creadas en el mismo
-- milisegundo, o un reloj que se ajusta hacia atras, harian que el cliente se
-- saltara registros sin enterarse, y saltarse un movimiento en una aplicacion de
-- finanzas es un balance que no cuadra y ninguna pista de por que.
--
-- Es un contador por usuario, y se reparte con un UPDATE sobre la fila del
-- usuario. Eso da la propiedad que hace falta: el UPDATE toma un cerrojo de fila,
-- asi que dos escrituras del mismo usuario se serializan y **cuando el numero N
-- es visible, ninguno menor que N esta todavia por confirmar**. Un cliente que va
-- por N no puede perderse nada. Con una secuencia global de PostgreSQL no seria
-- cierto: dos transacciones pueden coger 5 y 6 y confirmar en orden inverso.
--
-- Lo reparte un disparador y no el codigo de Rust a proposito. Hay cinco caminos
-- de escritura hoy y habra mas; un disparador no se puede olvidar en el sexto, y
-- esto no es una regla de negocio sino una propiedad de como se guardan las filas.

CREATE TABLE IF NOT EXISTS user_change_counters (
    user_id  uuid   PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    last_seq bigint NOT NULL DEFAULT 0
);

COMMENT ON TABLE user_change_counters IS
    'Ultimo numero de cambio repartido a cada usuario. Sin huecos salvo por transacciones revertidas.';

-- El siguiente numero del usuario. Crea la fila si es la primera escritura, asi
-- que no hace falta sembrarla al registrarse.
CREATE OR REPLACE FUNCTION next_change_seq(p_user_id uuid) RETURNS bigint AS $$
DECLARE
    v_seq bigint;
BEGIN
    INSERT INTO user_change_counters (user_id, last_seq)
    VALUES (p_user_id, 1)
    ON CONFLICT (user_id) DO UPDATE
        SET last_seq = user_change_counters.last_seq + 1
    RETURNING last_seq INTO v_seq;
    RETURN v_seq;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE movements     ADD COLUMN IF NOT EXISTS change_seq bigint;
ALTER TABLE loans         ADD COLUMN IF NOT EXISTS change_seq bigint;
ALTER TABLE loan_payments ADD COLUMN IF NOT EXISTS change_seq bigint;
ALTER TABLE categories    ADD COLUMN IF NOT EXISTS change_seq bigint;
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS change_seq bigint;

-- Las filas que ya existen se numeran por (created_at, id), que es el orden
-- estable que ya usa la paginacion de movimientos. Da igual que no sea el orden
-- real de creacion al milisegundo: lo unico que importa es que sea un orden
-- total y que nadie lo vuelva a calcular.
CREATE TEMP TABLE backfill ON COMMIT DROP AS
WITH everything AS (
    SELECT 'movement'::text AS source, id, user_id, created_at FROM movements
    UNION ALL SELECT 'loan',     id, user_id, created_at FROM loans
    UNION ALL SELECT 'payment',  id, user_id, created_at FROM loan_payments
    UNION ALL SELECT 'category', id, user_id, created_at FROM categories
)
SELECT source,
       id,
       user_id,
       row_number() OVER (PARTITION BY user_id ORDER BY created_at, id) AS seq
FROM everything;

UPDATE movements     t SET change_seq = b.seq FROM backfill b WHERE b.source = 'movement' AND b.id = t.id;
UPDATE loans         t SET change_seq = b.seq FROM backfill b WHERE b.source = 'loan'     AND b.id = t.id;
UPDATE loan_payments t SET change_seq = b.seq FROM backfill b WHERE b.source = 'payment'  AND b.id = t.id;
UPDATE categories    t SET change_seq = b.seq FROM backfill b WHERE b.source = 'category' AND b.id = t.id;

-- Los ajustes van detras de todo lo demas del usuario: es una sola fila y su
-- numero solo tiene que ser mayor que el resto para que un cliente en frio la vea.
UPDATE user_settings s
   SET change_seq = COALESCE((SELECT max(seq) FROM backfill b WHERE b.user_id = s.user_id), 0) + 1;

INSERT INTO user_change_counters (user_id, last_seq)
SELECT user_id, max(seq)
  FROM (
        SELECT user_id, seq FROM backfill
        UNION ALL SELECT user_id, change_seq FROM user_settings WHERE change_seq IS NOT NULL
       ) AS todo
 GROUP BY user_id
ON CONFLICT (user_id) DO UPDATE SET last_seq = excluded.last_seq;

ALTER TABLE movements     ALTER COLUMN change_seq SET NOT NULL;
ALTER TABLE loans         ALTER COLUMN change_seq SET NOT NULL;
ALTER TABLE loan_payments ALTER COLUMN change_seq SET NOT NULL;
ALTER TABLE categories    ALTER COLUMN change_seq SET NOT NULL;
ALTER TABLE user_settings ALTER COLUMN change_seq SET NOT NULL;

-- Unico por usuario: si dos filas compartieran numero, el cursor no podria
-- pararse entre ellas y el cliente repetiria o se saltaria una.
CREATE UNIQUE INDEX IF NOT EXISTS movements_change_seq_idx     ON movements     (user_id, change_seq);
CREATE UNIQUE INDEX IF NOT EXISTS loans_change_seq_idx         ON loans         (user_id, change_seq);
CREATE UNIQUE INDEX IF NOT EXISTS loan_payments_change_seq_idx ON loan_payments (user_id, change_seq);
CREATE UNIQUE INDEX IF NOT EXISTS categories_change_seq_idx    ON categories    (user_id, change_seq);

-- Reparte el numero en el INSERT, y tambien en el UPDATE de lo que si muta:
-- una categoria renombrada o archivada es un cambio que el cliente tiene que ver.
CREATE OR REPLACE FUNCTION assign_change_seq() RETURNS trigger AS $$
BEGIN
    NEW.change_seq := next_change_seq(NEW.user_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER movements_change_seq
    BEFORE INSERT ON movements
    FOR EACH ROW EXECUTE FUNCTION assign_change_seq();

CREATE OR REPLACE TRIGGER loans_change_seq
    BEFORE INSERT ON loans
    FOR EACH ROW EXECUTE FUNCTION assign_change_seq();

CREATE OR REPLACE TRIGGER loan_payments_change_seq
    BEFORE INSERT ON loan_payments
    FOR EACH ROW EXECUTE FUNCTION assign_change_seq();

CREATE OR REPLACE TRIGGER categories_change_seq
    BEFORE INSERT OR UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION assign_change_seq();

CREATE OR REPLACE TRIGGER user_settings_change_seq
    BEFORE INSERT OR UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION assign_change_seq();
