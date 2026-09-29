import { USD, type CurrencyCode } from './currency';
import {
  DIVISION_SCALE,
  ONE,
  divideDecimals,
  multiplyDecimals,
  signOf,
  type Decimal,
} from './decimal';

/** Frescura de la tabla de tasas, tal como la marca `GET /v1/exchange-rates`. */
export type RatesStatus = 'ok' | 'stale' | 'unavailable';

/**
 * Tabla de tasas con base USD.
 *
 * La API **nunca** devuelve importes convertidos: sirve tasas y la conversion la hace el
 * cliente. Por eso esta tabla es del dominio y no un detalle de la pantalla.
 */
export interface ExchangeRates {
  readonly base: CurrencyCode;
  /** El dia de la pasada, en `YYYY-MM-DD`. Null si no hay ninguna guardada. */
  readonly date: string | null;
  readonly status: RatesStatus;
  readonly rates: ReadonlyMap<CurrencyCode, Decimal>;
}

export const EMPTY_RATES: ExchangeRates = {
  base: USD,
  date: null,
  status: 'unavailable',
  rates: new Map(),
};

function positiveRate(rates: ExchangeRates, currency: CurrencyCode): Decimal | null {
  const rate = rates.rates.get(currency);
  return rate !== undefined && signOf(rate) > 0 ? rate : null;
}

/** True si la tabla puede convertir a y desde [currency]. */
export function covers(rates: ExchangeRates, currency: CurrencyCode): boolean {
  return positiveRate(rates, currency) !== null;
}

/**
 * `convert(n, a, b) = n * rate[b] / rate[a]`, con las tasas en base USD.
 *
 * Devuelve null si falta alguna de las dos tasas, para que quien llama pueda caer a su plan
 * B en vez de guardar una equivalencia inventada.
 */
export function convert(
  rates: ExchangeRates,
  amount: Decimal,
  from: CurrencyCode,
  to: CurrencyCode,
): Decimal | null {
  if (from === to) return amount;
  const fromRate = positiveRate(rates, from);
  const toRate = positiveRate(rates, to);
  if (fromRate === null || toRate === null) return null;
  return divideDecimals(multiplyDecimals(amount, toRate), fromRate, DIVISION_SCALE);
}

/** Lo que vale una unidad de [from] en [to]. Es lo que ensena la caja de conversion. */
export function rateOf(rates: ExchangeRates, from: CurrencyCode, to: CurrencyCode): Decimal | null {
  return convert(rates, ONE, from, to);
}
