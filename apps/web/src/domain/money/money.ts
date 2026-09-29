import { decimalsOf, type CurrencyCode } from './currency';
import {
  ZERO,
  absDecimal,
  addDecimals,
  compareDecimals,
  isZero,
  negateDecimal,
  parseDecimal,
  roundDecimal,
  signOf,
  subtractDecimals,
  type Decimal,
} from './decimal';

/**
 * Un importe con su moneda.
 *
 * Siempre sobre [Decimal], nunca sobre `number`: la API manda los importes como cadena
 * justamente para que nadie los meta en un float y pierda centavos.
 */
export interface Money {
  readonly amount: Decimal;
  readonly currency: CurrencyCode;
}

export function moneyOf(amount: Decimal, currency: CurrencyCode): Money {
  return { amount, currency };
}

export function zeroMoney(currency: CurrencyCode): Money {
  return { amount: ZERO, currency };
}

/** Lee un importe tal como lo manda la API: una cadena decimal. Null si no lo es. */
export function parseMoney(raw: string, currency: CurrencyCode): Money | null {
  const amount = parseDecimal(raw);
  return amount === null ? null : { amount, currency };
}

export function isZeroMoney(value: Money): boolean {
  return isZero(value.amount);
}

export function isNegativeMoney(value: Money): boolean {
  return signOf(value.amount) < 0;
}

/**
 * Suma dos importes de la **misma** moneda.
 *
 * Mezclar monedas lanza en vez de devolver algo: un total que suma pesos con dolares no es
 * un numero peor, es un numero que no significa nada, y el sitio donde se descubre tiene
 * que ser este y no la pantalla.
 */
export function addMoney(a: Money, b: Money): Money {
  requireSameCurrency(a, b);
  return { amount: addDecimals(a.amount, b.amount), currency: a.currency };
}

export function subtractMoney(a: Money, b: Money): Money {
  requireSameCurrency(a, b);
  return { amount: subtractDecimals(a.amount, b.amount), currency: a.currency };
}

export function negateMoney(value: Money): Money {
  return { amount: negateDecimal(value.amount), currency: value.currency };
}

export function absMoney(value: Money): Money {
  return { amount: absDecimal(value.amount), currency: value.currency };
}

export function compareMoney(a: Money, b: Money): number {
  requireSameCurrency(a, b);
  return compareDecimals(a.amount, b.amount);
}

/** Redondea a los decimales de su moneda. Es lo que se manda a la API. */
export function roundedMoney(value: Money): Money {
  return {
    amount: roundDecimal(value.amount, decimalsOf(value.currency)),
    currency: value.currency,
  };
}

function requireSameCurrency(a: Money, b: Money): void {
  if (a.currency !== b.currency) {
    throw new Error(`No se pueden combinar ${a.currency} y ${b.currency}`);
  }
}
