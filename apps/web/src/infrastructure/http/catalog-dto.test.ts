import { describe, expect, it } from 'vitest';

import { decimalToString } from '../../domain/money/decimal';

import { readCurrencyList, readExchangeRates, readSettings } from './catalog-dto';

describe('ajustes', () => {
  it('lee la moneda y el idioma', () => {
    expect(readSettings({ displayCurrency: 'COP', language: 'es' })).toEqual({
      displayCurrency: 'COP',
      language: 'es',
    });
  });

  it('una moneda que no es un codigo deja los ajustes fuera', () => {
    expect(readSettings({ displayCurrency: 'pesos', language: 'es' })).toBeNull();
    expect(readSettings({ displayCurrency: 'COP', language: 'fr' })).toBeNull();
  });
});

describe('tasas de cambio', () => {
  const BODY = {
    base: 'USD',
    date: '2026-09-22',
    fetchedAt: '2026-09-22T06:00:00Z',
    status: 'ok',
    rates: { COP: '4000.000000', EUR: '0.920000' },
  };

  it('lee la tabla con su fecha y su frescura', () => {
    const rates = readExchangeRates(BODY);
    expect(rates?.status).toBe('ok');
    expect(rates?.date).toBe('2026-09-22');
    expect(decimalToString(rates?.rates.get('COP' as never) ?? { units: 0n, scale: 0 })).toBe(
      '4000.000000',
    );
  });

  /**
   * Un cero en la tabla daria una division por cero o un importe de cero, y un balance de
   * cero pesos parece un dato y no un fallo.
   */
  it('descarta las tasas que no son positivas o no se leen', () => {
    const rates = readExchangeRates({
      ...BODY,
      rates: { COP: '4000', EUR: '0', GBP: '-1', XX: '2', ZZZ: 'nada' },
    });
    expect([...(rates?.rates.keys() ?? [])]).toEqual(['COP']);
  });

  it('un cuerpo que no cuadra no es una tabla', () => {
    expect(readExchangeRates({ ...BODY, status: 'raro' })).toBeNull();
    expect(readExchangeRates({ ...BODY, base: 'dolares' })).toBeNull();
  });
});

describe('catalogo de monedas', () => {
  it('lee las monedas y descarta las que no tienen codigo', () => {
    const catalog = readCurrencyList({
      items: [
        { code: 'COP', name: 'Colombian Peso', symbol: '$' },
        { code: 'pesos', name: 'Nada', symbol: '$' },
      ],
    });
    expect(catalog).toHaveLength(1);
    expect(catalog?.[0]?.symbol).toBe('$');
  });
});
