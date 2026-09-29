declare const currencyBrand: unique symbol;

/**
 * Codigo ISO 4217 de tres letras, siempre en mayusculas.
 *
 * Es un tipo marcado y no un `string` a secas porque **todo el dominio compara monedas**:
 * si `'cop'` y `'COP'` pudieran convivir, dos importes de la misma moneda dejarian de
 * sumar y nadie se enteraria hasta ver un total raro. Se construye por [currencyOf] o por
 * [parseCurrency], que normalizan.
 */
export type CurrencyCode = string & { readonly [currencyBrand]: true };

const CODE_PATTERN = /^[A-Z]{3}$/u;

/** Acepta minusculas y espacios. Devuelve null si no es un codigo de tres letras. */
export function parseCurrency(raw: string): CurrencyCode | null {
  const normalized = raw.trim().toUpperCase();
  return CODE_PATTERN.test(normalized) ? (normalized as CurrencyCode) : null;
}

/** Lo mismo, pero lanza. Es para las constantes del codigo, no para datos de fuera. */
export function currencyOf(raw: string): CurrencyCode {
  const parsed = parseCurrency(raw);
  if (parsed === null) throw new Error(`No es un codigo de moneda: '${raw}'`);
  return parsed;
}

/** Moneda principal por defecto de una cuenta nueva. */
export const COP = currencyOf('COP');

/** Base de las tasas de cambio. */
export const USD = currencyOf('USD');

export const EUR = currencyOf('EUR');
const MXN = currencyOf('MXN');

/** Las cuatro que el selector de moneda ensena en Frecuentes, en orden. */
export const FREQUENT_CURRENCIES: readonly CurrencyCode[] = [COP, USD, EUR, MXN];

/** Decimales de una moneda con fraccion. */
export const DEFAULT_DECIMALS = 2;

/** Decimales de una moneda sin fraccion. */
export const NO_DECIMALS = 0;

/**
 * Las monedas sin parte decimal, copiadas enteras del prototipo. Deciden el formato de los
 * importes, cuantos decimales acepta el teclado y con que margen se da por pagado un
 * prestamo.
 */
const ZERO_DECIMAL = new Set(
  (
    'COP CLP JPY KRW IDR VND PYG HUF ISK UGX IRR LAK KHR MMK LBP IQD UZS ' +
    'GNF MGA RWF BIF XAF XOF KPW SYP TZS MNT KMF DJF VUV XPF SLE SOS SSP'
  ).split(' '),
);

/** Decimales de [currency]: 0 en las monedas sin fraccion y 2 en el resto. */
export function decimalsOf(currency: CurrencyCode): number {
  return ZERO_DECIMAL.has(currency) ? NO_DECIMALS : DEFAULT_DECIMALS;
}

/** True si la moneda no admite parte decimal. */
export function hasNoFraction(currency: CurrencyCode): boolean {
  return decimalsOf(currency) === NO_DECIMALS;
}
