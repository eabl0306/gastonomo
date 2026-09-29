import { decimalsOf, type CurrencyCode } from '../../domain/money/currency';
import {
  ZERO,
  decimalToString,
  parseDecimal,
  roundDecimal,
  trimDecimal,
  type Decimal,
} from '../../domain/money/decimal';

import type { LocalizedNumbers } from './localized-numbers';

/** Maximo de digitos de la parte entera. */
const MAX_INTEGER_DIGITS = 11;

/**
 * La caja de conversion de los formularios pide la **equivalencia**, no la tasa: se teclea
 * "560.000 COP", no "4000 por dolar". Por eso aqui no hay un modo tasa con sus propios
 * decimales, que es lo que si necesita el movil.
 */
/**
 * El teclado de importes de los tres formularios.
 *
 * Hay dos representaciones y conviene no mezclarlas:
 * - **raw**: lo que se guarda en el estado. Siempre con punto decimal y sin separador de
 *   miles. Es lo que se convierte a [Decimal].
 * - **display**: lo que se pinta en el campo, con el separador y la agrupacion del idioma.
 */
export interface AmountInput {
  /** El separador decimal del idioma, que es el unico que el campo deja teclear. */
  readonly separator: string;
  /** Limpia lo tecleado y devuelve un valor raw, con los decimales de la moneda. */
  sanitize: (typed: string, currency: CurrencyCode) => string;
  /** Pinta un valor raw con el separador y la agrupacion del idioma. */
  display: (raw: string) => string;
}

function isDigit(character: string): boolean {
  return character >= '0' && character <= '9';
}

/** Las dos partes de lo tecleado, ya recortadas, y si llego a escribirse el separador. */
interface Typed {
  readonly integer: string;
  readonly fraction: string;
  readonly separated: boolean;
}

function split(typed: string, separator: string, maxDecimals: number): Typed {
  let integer = '';
  let fraction = '';
  let separated = false;
  for (const character of typed) {
    if (!isDigit(character)) {
      if (character === separator && !separated && maxDecimals > 0) separated = true;
    } else if (!separated) {
      integer += character;
    } else if (fraction.length < maxDecimals) {
      fraction += character;
    }
  }
  return { integer, fraction, separated };
}

function clean(typed: string, separator: string, maxDecimals: number): string {
  const parts = split(typed, separator, maxDecimals);
  const head = parts.integer.replace(/^0+(?=\d)/u, '').slice(0, MAX_INTEGER_DIGITS);
  // Quien teclea solo el separador esta empezando por los decimales: la parte entera es cero.
  const whole = head === '' && parts.separated ? '0' : head;
  return parts.separated ? `${whole}.${parts.fraction}` : whole;
}

export function amountInput(numbers: LocalizedNumbers): AmountInput {
  const separator = numbers.decimalSeparator;
  return {
    separator,
    sanitize: (typed, currency) => clean(typed, separator, decimalsOf(currency)),
    display: (raw) => {
      if (raw === '') return '';
      const [whole = '', fraction] = raw.split('.');
      const grouped = numbers.group(whole);
      return fraction === undefined ? grouped : grouped + separator + fraction;
    },
  };
}

/** Lee un valor raw. Una cadena vacia o a medio teclear vale cero. */
export function parseAmount(raw: string): Decimal {
  return parseDecimal(raw.replace(/\.$/u, '')) ?? ZERO;
}

/**
 * Convierte un valor calculado en raw, redondeado a los decimales de la moneda y sin ceros
 * de relleno. Es lo que rellena la caja de conversion y los chips de "Todo el saldo".
 */
export function amountToRaw(value: Decimal, currency: CurrencyCode): string {
  return toRaw(value, decimalsOf(currency));
}

function toRaw(value: Decimal, decimals: number): string {
  const rounded = trimDecimal(roundDecimal(value, decimals));
  return rounded.units === 0n ? '0' : decimalToString(rounded);
}

/**
 * Donde dejar el cursor en [shown] para que quede justo detras del digito numero [digits],
 * contando desde la izquierda.
 *
 * Hace falta porque el campo reescribe el texto en cada pulsacion: teclear un digito puede
 * meter un separador de miles y correr todo lo que hay a la derecha. Sin esto el cursor se va
 * al final y editar el medio de un importe es imposible.
 */
export function caretAfter(shown: string, digits: number): number {
  if (digits <= 0) return 0;
  let seen = 0;
  for (let index = 0; index < shown.length; index += 1) {
    const character = shown[index] ?? '';
    if (character >= '0' && character <= '9') {
      seen += 1;
      if (seen === digits) return index + 1;
    }
  }
  return shown.length;
}

/** Cuantos digitos hay hasta [position] en [shown]. Es lo que se conserva al reescribir. */
export function digitsBefore(shown: string, position: number): number {
  let count = 0;
  for (let index = 0; index < Math.min(position, shown.length); index += 1) {
    const character = shown[index] ?? '';
    if (character >= '0' && character <= '9') count += 1;
  }
  return count;
}
