import { describe, expect, it } from 'vitest';

import {
  suggestedMoney,
  effectiveRate,
  equivalentAmount,
  isEdited,
  isForeign,
  isValidSuggestion,
  suggestionOf,
} from './conversion';
import { COP, EUR, USD } from './currency';
import { decimalOf, decimalToString } from './decimal';
import { EMPTY_RATES, convert, covers, rateOf, type ExchangeRates } from './exchange-rates';
import { moneyOf } from './money';

/** Las tasas del prototipo, con base USD. */
const RATES: ExchangeRates = {
  base: USD,
  date: '2026-09-22',
  status: 'ok',
  rates: new Map([
    [USD, decimalOf('1')],
    [COP, decimalOf('3199.41')],
    [EUR, decimalOf('0.87088')],
  ]),
};

describe('exchange rates', () => {
  it('convierte pasando por la base', () => {
    const converted = convert(RATES, decimalOf('140'), USD, COP);
    expect(converted).not.toBeNull();
    expect(decimalToString(converted!).startsWith('447917.4')).toBe(true);
  });

  it('convertir a la misma moneda devuelve el importe tal cual', () => {
    const same = convert(RATES, decimalOf('140'), USD, USD);
    expect(decimalToString(same!)).toBe('140');
  });

  /** Sin tasa no se inventa una equivalencia: se dice que no se puede. */
  it('sin una de las dos tasas devuelve null', () => {
    expect(convert(RATES, decimalOf('1'), USD, 'JPY' as typeof USD)).toBeNull();
    expect(convert(EMPTY_RATES, decimalOf('1'), USD, COP)).toBeNull();
  });

  it('una tasa que llega en cero cuenta como que no esta', () => {
    const broken: ExchangeRates = { ...RATES, rates: new Map([[COP, decimalOf('0')]]) };
    expect(covers(broken, COP)).toBe(false);
    expect(convert(broken, decimalOf('1'), USD, COP)).toBeNull();
  });

  it('la tasa de una unidad es lo que ensena la caja de conversion', () => {
    expect(decimalToString(rateOf(RATES, USD, COP)!).startsWith('3199.41')).toBe(true);
  });
});

describe('conversion suggestion', () => {
  it('en la moneda principal no hay caja y la equivalencia es el importe', () => {
    const suggestion = suggestionOf(moneyOf(decimalOf('45000'), COP), COP, RATES);
    expect(isForeign(suggestion)).toBe(false);
    expect(decimalToString(equivalentAmount(suggestion))).toBe('45000');
    expect(isValidSuggestion(suggestion)).toBe(true);
  });

  it('en otra moneda propone la tasa vigente y calcula la equivalencia', () => {
    const suggestion = suggestionOf(moneyOf(decimalOf('140'), USD), COP, RATES);
    expect(isForeign(suggestion)).toBe(true);
    expect(isEdited(suggestion)).toBe(false);
    const money = suggestedMoney(suggestion);
    expect(decimalToString(money.amount)).toBe('140.00');
    expect(decimalToString(money.base)).toBe('447917');
    expect(money.baseCurrency).toBe(COP);
  });

  /**
   * Lo que el usuario edita es la tasa, no la equivalencia: quien recibe dolares sabe a como
   * esta el cambio, no cuanto suman ciento cuarenta dolares en pesos.
   */
  it('la tasa que escribe el usuario manda sobre la vigente', () => {
    const suggestion = suggestionOf(moneyOf(decimalOf('140'), USD), COP, RATES, decimalOf('4000'));
    expect(isEdited(suggestion)).toBe(true);
    expect(decimalToString(effectiveRate(suggestion)!)).toBe('4000');
    expect(decimalToString(suggestedMoney(suggestion).base)).toBe('560000');
  });

  it('sin tasa no se puede guardar', () => {
    const suggestion = suggestionOf(moneyOf(decimalOf('140'), USD), COP, EMPTY_RATES);
    expect(isValidSuggestion(suggestion)).toBe(false);
    expect(decimalToString(equivalentAmount(suggestion))).toBe('0');
  });

  it('un importe en cero no se puede guardar aunque haya tasa', () => {
    const suggestion = suggestionOf(moneyOf(decimalOf('0'), USD), COP, RATES);
    expect(isValidSuggestion(suggestion)).toBe(false);
  });
});
