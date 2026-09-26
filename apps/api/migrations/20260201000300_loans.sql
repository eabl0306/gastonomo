-- Prestamos y sus abonos.
--
-- Cada fila guarda los cuatro campos de importe: lo que tecleo el usuario
-- (amount, currency) y su equivalencia en la moneda principal del momento
-- (base, base_currency), con la tasa que el usuario dejo puesta. La API no
-- recalcula nada.

CREATE TABLE IF NOT EXISTS loans (
    id            uuid           PRIMARY KEY,
    user_id       uuid           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    person        text           NOT NULL,
    note          text           NOT NULL DEFAULT '',
    direction     text           NOT NULL,
    opened_on     date           NOT NULL,
    amount        numeric(18, 4) NOT NULL,
    currency      char(3)        NOT NULL,
    base          numeric(18, 4) NOT NULL,
    base_currency char(3)        NOT NULL,
    created_at    timestamptz    NOT NULL DEFAULT now(),
    updated_at    timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT loans_direction_allowed CHECK (direction IN ('lent', 'borrowed')),
    CONSTRAINT loans_person_not_blank CHECK (length(btrim(person)) BETWEEN 1 AND 80),
    CONSTRAINT loans_amount_positive CHECK (amount > 0 AND base > 0),
    CONSTRAINT loans_currency_shape CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT loans_base_currency_shape CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT loans_same_currency_same_amount
        CHECK (currency <> base_currency OR amount = base)
);

COMMENT ON TABLE loans IS
    'Prestamos. El estado (Pagado, Parcial, Pendiente) lo deriva el cliente.';

CREATE INDEX IF NOT EXISTS loans_user_direction_idx
    ON loans (user_id, direction);
CREATE INDEX IF NOT EXISTS loans_user_opened_on_idx
    ON loans (user_id, opened_on DESC);

CREATE TABLE IF NOT EXISTS loan_payments (
    id            uuid           PRIMARY KEY,
    loan_id       uuid           NOT NULL REFERENCES loans (id) ON DELETE CASCADE,
    user_id       uuid           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    paid_on       date           NOT NULL,
    label         text,
    amount        numeric(18, 4) NOT NULL,
    currency      char(3)        NOT NULL,
    base          numeric(18, 4) NOT NULL,
    base_currency char(3)        NOT NULL,
    created_at    timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT loan_payments_amount_positive CHECK (amount > 0 AND base > 0),
    CONSTRAINT loan_payments_currency_shape CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT loan_payments_base_currency_shape CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT loan_payments_same_currency_same_amount
        CHECK (currency <> base_currency OR amount = base)
);

COMMENT ON TABLE loan_payments IS
    'Abonos de un prestamo. El saldo lo calcula el cliente con las tasas vigentes.';

CREATE INDEX IF NOT EXISTS loan_payments_loan_paid_on_idx
    ON loan_payments (loan_id, paid_on DESC);
CREATE INDEX IF NOT EXISTS loan_payments_user_idx
    ON loan_payments (user_id);
