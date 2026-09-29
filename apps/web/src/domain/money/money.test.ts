import { describe, expect, it } from 'vitest';

import {
  COP,
  DEFAULT_DECIMALS,
  EUR,
  FREQUENT_CURRENCIES,
  NO_DECIMALS,
  USD,
  currencyOf,
  decimalsOf,
  hasNoFraction,
  parseCurrency,
} from './currency';
import { decimalOf, decimalToString } from './decimal';
import { amountOf, equivalentOf, isForeignTo, originalOf, singleAmount } from './monetary-amount';
import {
  addMoney,
  compareMoney,
  isNegativeMoney,
  isZeroMoney,
  moneyOf,
  parseMoney,
  roundedMoney,
  subtractMoney,
  zeroMoney,
} from './money';

describe('currency', () => {
  it('normaliza a mayusculas y quita espacios', () => {
    expect(parseCurrency(' cop ')).toBe(COP);
  });

  it('rechaza lo que no son tres letras', () => {
    for (const raw of ['', 'CO', 'COPP', 'C0P', '123']) {
      expect(parseCurrency(raw)).toBeNull();
    }
  });

  it('el peso colombiano no tiene decimales y el dolar si', () => {
    expect(decimalsOf(COP)).toBe(NO_DECIMALS);
    expect(hasNoFraction(COP)).toBe(true);
    expect(decimalsOf(USD)).toBe(DEFAULT_DECIMALS);
    expect(hasNoFraction(USD)).toBe(false);
  });

  it('una moneda que no esta en la lista de sin fraccion lleva dos decimales', () => {
    expect(decimalsOf(currencyOf('GBP'))).toBe(DEFAULT_DECIMALS);
  });

  it('las frecuentes son las cuatro del diseno, en orden', () => {
    expect(FREQUENT_CURRENCIES).toEqual([COP, USD, EUR, currencyOf('MXN')]);
  });
});

describe('money', () => {
  it('lee un importe tal como lo manda la API', () => {
    const money = parseMoney('186400.0000', COP);
    expect(money).not.toBeNull();
    expect(decimalToString(money!.amount)).toBe('186400.0000');
    expect(money!.currency).toBe(COP);
  });

  it('un importe que no es decimal no se lee a medias', () => {
    expect(parseMoney('mucho', COP)).toBeNull();
  });

  it('suma y resta dentro de la misma moneda', () => {
    const a = moneyOf(decimalOf('1000'), COP);
    const b = moneyOf(decimalOf('250.5'), COP);
    expect(decimalToString(addMoney(a, b).amount)).toBe('1250.5');
    expect(decimalToString(subtractMoney(a, b).amount)).toBe('749.5');
  });

  /**
   * Un total que suma pesos con dolares no es un numero peor: es un numero que no significa
   * nada. Tiene que romper aqui y no en la pantalla.
   */
  it('mezclar monedas lanza en vez de devolver un total sin sentido', () => {
    const pesos = moneyOf(decimalOf('1000'), COP);
    const dolares = moneyOf(decimalOf('1'), USD);
    expect(() => addMoney(pesos, dolares)).toThrow(/COP/u);
    expect(() => compareMoney(pesos, dolares)).toThrow(/USD/u);
  });

  it('redondea a los decimales de su moneda', () => {
    expect(decimalToString(roundedMoney(moneyOf(decimalOf('45000.6'), COP)).amount)).toBe('45001');
    expect(decimalToString(roundedMoney(moneyOf(decimalOf('12.345'), USD)).amount)).toBe('12.35');
  });

  it('sabe si es cero o negativo', () => {
    expect(isZeroMoney(zeroMoney(COP))).toBe(true);
    expect(isNegativeMoney(moneyOf(decimalOf('-1'), COP))).toBe(true);
  });
});

describe('monetary amount', () => {
  it('en la moneda principal la equivalencia es el propio importe', () => {
    const amount = singleAmount(decimalOf('186400'), COP);
    expect(amount.base).toBe(amount.amount);
    expect(amount.baseCurrency).toBe(COP);
    expect(isForeignTo(amount, COP)).toBe(false);
  });

  it('en otra moneda guarda los dos importes y avisa de que hay que ensenar el original', () => {
    const amount = amountOf(moneyOf(decimalOf('140'), USD), moneyOf(decimalOf('447917'), COP));
    expect(decimalToString(originalOf(amount).amount)).toBe('140');
    expect(decimalToString(equivalentOf(amount).amount)).toBe('447917');
    expect(isForeignTo(amount, COP)).toBe(true);
  });
});
