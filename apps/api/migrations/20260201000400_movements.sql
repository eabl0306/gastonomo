-- Movimientos: ingresos y gastos.
--
-- La fecha es DATE, sin hora ni zona: un gasto del 22 de septiembre es del 22 en
-- Bogota y en Madrid. Los instantes tecnicos (created_at) si son timestamptz.

CREATE TABLE IF NOT EXISTS movements (
    id              uuid           PRIMARY KEY,
    user_id         uuid           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    occurred_on     date           NOT NULL,
    kind            text           NOT NULL,
    category_id     uuid           NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    title           text           NOT NULL,
    amount          numeric(18, 4) NOT NULL,
    currency        char(3)        NOT NULL,
    base            numeric(18, 4) NOT NULL,
    base_currency   char(3)        NOT NULL,
    loan_id         uuid           REFERENCES loans (id) ON DELETE SET NULL,
    loan_payment_id uuid           REFERENCES loan_payments (id) ON DELETE SET NULL,
    created_at      timestamptz    NOT NULL DEFAULT now(),
    updated_at      timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT movements_kind_allowed CHECK (kind IN ('in', 'out')),
    CONSTRAINT movements_title_length CHECK (length(btrim(title)) BETWEEN 1 AND 120),
    CONSTRAINT movements_amount_positive CHECK (amount > 0 AND base > 0),
    CONSTRAINT movements_currency_shape CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT movements_base_currency_shape CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT movements_same_currency_same_amount
        CHECK (currency <> base_currency OR amount = base)
);

COMMENT ON TABLE movements IS
    'Ingresos y gastos. loan_id y loan_payment_id rastrean los que genero un prestamo.';

-- Filtro de periodo y lista agrupada por dia, que es como se pinta la pantalla.
CREATE INDEX IF NOT EXISTS movements_user_occurred_on_idx
    ON movements (user_id, occurred_on DESC, id DESC);
-- Conteo de uso de la pantalla de Categorias y reasignacion al borrar.
CREATE INDEX IF NOT EXISTS movements_category_idx
    ON movements (category_id);
CREATE INDEX IF NOT EXISTS movements_loan_idx
    ON movements (loan_id)
    WHERE loan_id IS NOT NULL;
