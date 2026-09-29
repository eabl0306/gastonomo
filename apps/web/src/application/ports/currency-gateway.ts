import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';

import type { Outcome } from './api-error';

/** Una moneda del catalogo de `GET /v1/currencies`. */
export interface CurrencyInfo {
  readonly code: CurrencyCode;
  /** El nombre tal como lo publica la fuente, en ingles. */
  readonly name: string;
  /** Nunca vacio: si la fuente no da simbolo, es el propio codigo. */
  readonly symbol: string;
}

export interface CurrencyGateway {
  catalog: () => Promise<Outcome<readonly CurrencyInfo[]>>;
  /**
   * La tabla en base USD.
   *
   * Un 503 aqui no es un fallo: es que la tarea de fondo todavia no ha guardado ninguna
   * pasada. Quien llama cae a [EMPTY_RATES] y apaga la multimoneda con su explicacion.
   */
  rates: () => Promise<Outcome<ExchangeRates>>;
}
