-- Catalogo de monedas y tasas de cambio.
--
-- Los datos vienen de Frankfurter y los escribe la tarea de refresco de la API:
-- los clientes nunca llaman a Frankfurter. Una moneda no se borra nunca, porque
-- los registros que la usan siguen existiendo; se marca con offered = false y
-- deja de ofrecerse en el sheet de moneda.

CREATE TABLE IF NOT EXISTS currencies (
    code       char(3)     PRIMARY KEY,
    name       text        NOT NULL,
    symbol     text        NOT NULL DEFAULT '',
    offered    boolean     NOT NULL DEFAULT TRUE,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT currencies_code_shape CHECK (code ~ '^[A-Z]{3}$')
);

COMMENT ON TABLE currencies IS
    'Catalogo de monedas. offered = false deja de ofrecerla sin romper el historico.';

CREATE TABLE IF NOT EXISTS exchange_rates (
    rate_date  date            NOT NULL,
    base       char(3)         NOT NULL,
    quote      char(3)         NOT NULL,
    rate       numeric(20, 10) NOT NULL,
    fetched_at timestamptz     NOT NULL DEFAULT now(),
    PRIMARY KEY (rate_date, base, quote),
    CONSTRAINT exchange_rates_rate_positive CHECK (rate > 0),
    CONSTRAINT exchange_rates_base_shape CHECK (base ~ '^[A-Z]{3}$'),
    CONSTRAINT exchange_rates_quote_shape CHECK (quote ~ '^[A-Z]{3}$')
);

COMMENT ON TABLE exchange_rates IS
    'Tasas con base USD. Se escriben con INSERT ... ON CONFLICT DO UPDATE.';

-- La consulta de /v1/exchange-rates pide la ultima fecha disponible por base.
CREATE INDEX IF NOT EXISTS exchange_rates_base_date_idx
    ON exchange_rates (base, rate_date DESC);
