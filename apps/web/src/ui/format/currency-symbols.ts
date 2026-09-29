import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import { COP, USD, type CurrencyCode } from '../../domain/money/currency';

/** Codigo de moneda a simbolo, tal como lo sirve `GET /v1/currencies`. */
export type CurrencySymbolTable = Readonly<Record<string, string>>;

const DOLLAR = '$';
const DOLLAR_US = 'US$';
const PREFIX_LENGTH = 2;

/**
 * Tabla de reserva, la del prototipo.
 *
 * Sirve para el arranque en frio: hasta que llega el catalogo, y cuando la API responde con
 * la lista vacia porque la tarea de tasas todavia no ha corrido.
 */
export const FALLBACK_SYMBOLS: CurrencySymbolTable = {
  COP: '$',
  USD: '$',
  EUR: '€',
  MXN: '$',
  GBP: '£',
  BRL: 'R$',
  ARS: '$',
  CLP: '$',
  PEN: 'S/',
  CAD: '$',
  JPY: '¥',
  CHF: 'CHF',
};

/**
 * El simbolo que se pinta delante del importe. Las reglas son las del diseno y estan
 * pensadas para que dos monedas distintas no se vean igual:
 *
 * - COP usa `$`, porque es la moneda principal por defecto y es su casa.
 * - USD usa `US$`.
 * - Cualquier otra cuyo simbolo tambien sea `$` usa las dos primeras letras de su codigo mas
 *   `$`: MX$, AR$, CA$, CL$.
 * - El resto usa su simbolo, y el propio codigo si no hay ninguno.
 */
export function symbolOf(table: CurrencySymbolTable, currency: CurrencyCode): string {
  if (currency === USD) return DOLLAR_US;
  const symbol = table[currency];
  if (symbol === undefined || symbol.trim() === '') return currency;
  if (symbol === DOLLAR && currency !== COP) {
    return currency.slice(0, PREFIX_LENGTH) + DOLLAR;
  }
  return symbol;
}

/** Anade el catalogo de la API sin perder lo que ya habia. Uno vacio no borra nada. */
export function withCatalog(
  table: CurrencySymbolTable,
  catalog: readonly CurrencyInfo[],
): CurrencySymbolTable {
  if (catalog.length === 0) return table;
  const merged: Record<string, string> = { ...table };
  for (const currency of catalog) merged[currency.code] = currency.symbol;
  return merged;
}
