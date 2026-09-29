import type { CurrencyCode } from './currency';
import { ZERO, multiplyDecimals, signOf, type Decimal } from './decimal';
import { rateOf, type ExchangeRates } from './exchange-rates';
import { amountOf, type MonetaryAmount } from './monetary-amount';
import { moneyOf, roundedMoney, type Money } from './money';

/**
 * La caja de conversion de los tres formularios de creacion, como estado del dominio.
 *
 * Aparece cuando el registro se teclea en una moneda distinta de la principal. Propone la
 * tasa vigente y **deja que el usuario la edite**; la equivalencia sale de multiplicar el
 * importe por la tasa que quede, y es la que se guarda como `base`.
 *
 * **Lo que se edita es la tasa, no la equivalencia.** Al reves era pedirle al usuario la
 * cuenta ya hecha: quien recibe dolares sabe a como esta el cambio, no cuanto suman ciento
 * cuarenta dolares en pesos.
 */
export interface ConversionSuggestion {
  readonly entered: Money;
  readonly display: CurrencyCode;
  readonly suggestedRate: Decimal | null;
  /** La tasa que escribio el usuario, o null para usar la sugerida. */
  readonly editedRate: Decimal | null;
}

export function suggestionOf(
  entered: Money,
  display: CurrencyCode,
  rates: ExchangeRates,
  editedRate: Decimal | null = null,
): ConversionSuggestion {
  return {
    entered,
    display,
    suggestedRate: rateOf(rates, entered.currency, display),
    editedRate,
  };
}

/** True si hay que ensenar la caja: el registro no esta en la moneda principal. */
export function isForeign(suggestion: ConversionSuggestion): boolean {
  return suggestion.entered.currency !== suggestion.display;
}

/** True si el usuario cambio la tasa y el enlace "Usar sugerida" tiene sentido. */
export function isEdited(suggestion: ConversionSuggestion): boolean {
  return suggestion.editedRate !== null;
}

/** La tasa que se usa: la del usuario si la escribio, y si no la vigente. */
export function effectiveRate(suggestion: ConversionSuggestion): Decimal | null {
  return suggestion.editedRate ?? suggestion.suggestedRate;
}

/**
 * La equivalencia que se guardara. En la moneda principal es el propio importe; en otra es
 * el importe por la tasa. Sin tasa no hay equivalencia, y entonces no se puede guardar.
 */
export function equivalentAmount(suggestion: ConversionSuggestion): Decimal {
  if (!isForeign(suggestion)) return suggestion.entered.amount;
  const rate = effectiveRate(suggestion);
  return rate === null ? ZERO : multiplyDecimals(suggestion.entered.amount, rate);
}

/** Guardar se habilita con importe mayor que cero y equivalencia mayor que cero. */
export function isValidSuggestion(suggestion: ConversionSuggestion): boolean {
  return signOf(suggestion.entered.amount) > 0 && signOf(equivalentAmount(suggestion)) > 0;
}

/** Los cuatro campos que se mandan a la API, ya redondeados a su moneda. */
export function suggestedMoney(suggestion: ConversionSuggestion): MonetaryAmount {
  return amountOf(
    roundedMoney(suggestion.entered),
    roundedMoney(moneyOf(equivalentAmount(suggestion), suggestion.display)),
  );
}
