import type { CurrencyCode } from './currency';
import type { Decimal } from './decimal';
import { moneyOf, type Money } from './money';

/**
 * Los cuatro campos que lleva todo registro de dinero: lo que teclo el usuario ([amount] y
 * [currency]) y su equivalencia en la moneda principal vigente al registrar ([base] y
 * [baseCurrency]).
 *
 * La equivalencia se guarda porque el usuario puede editar la tasa a mano, y porque cambiar
 * la moneda principal despues **no** debe tocar ningun registro ya hecho.
 */
export interface MonetaryAmount {
  readonly amount: Decimal;
  readonly currency: CurrencyCode;
  readonly base: Decimal;
  readonly baseCurrency: CurrencyCode;
}

/** El importe tal como lo teclo el usuario. */
export function originalOf(value: MonetaryAmount): Money {
  return moneyOf(value.amount, value.currency);
}

/** La equivalencia guardada. */
export function equivalentOf(value: MonetaryAmount): Money {
  return moneyOf(value.base, value.baseCurrency);
}

/** True si hay que ensenar tambien el importe original, porque no es la moneda que se pinta. */
export function isForeignTo(value: MonetaryAmount, display: CurrencyCode): boolean {
  return value.currency !== display;
}

/**
 * Registro en la moneda principal: la equivalencia es el propio importe. Es el caso en el
 * que la API deja omitir `base` y `baseCurrency`, los dos juntos.
 */
export function singleAmount(amount: Decimal, currency: CurrencyCode): MonetaryAmount {
  return { amount, currency, base: amount, baseCurrency: currency };
}

export function amountOf(original: Money, equivalent: Money): MonetaryAmount {
  return {
    amount: original.amount,
    currency: original.currency,
    base: equivalent.amount,
    baseCurrency: equivalent.currency,
  };
}
