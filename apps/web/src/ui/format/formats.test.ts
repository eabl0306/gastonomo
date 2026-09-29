import { describe, expect, it } from 'vitest';

import type { UserSettings } from '../../domain/account/settings';
import { COP, EUR, USD, currencyOf } from '../../domain/money/currency';
import { decimalOf } from '../../domain/money/decimal';
import { moneyOf } from '../../domain/money/money';

import { withCatalog, FALLBACK_SYMBOLS } from './currency-symbols';
import { MINUS, gastonomoFormats } from './formats';

const SPANISH: UserSettings = { displayCurrency: COP, language: 'es' };
const ENGLISH: UserSettings = { displayCurrency: COP, language: 'en' };

const formats = gastonomoFormats(SPANISH);

describe('formato de dinero', () => {
  it('pone el simbolo delante con un espacio y los decimales de la moneda', () => {
    expect(formats.amount(decimalOf('186400.0000'))).toBe('$ 186.400');
    expect(formats.amount(decimalOf('140.0000'), USD)).toBe('US$ 140,00');
    expect(formats.amount(decimalOf('99.9000'), EUR)).toBe('€ 99,90');
  });

  /** Dos monedas distintas no se pueden ver igual: el peso mexicano no es el colombiano. */
  it('desambigua las monedas que tambien usan el signo de dolar', () => {
    expect(formats.amount(decimalOf('250'), currencyOf('MXN'))).toBe('MX$ 250,00');
    expect(formats.amount(decimalOf('10'), currencyOf('NOK'))).toBe('NOK 10,00');
  });

  it('el catalogo de la API anade simbolos sin perder los que ya habia', () => {
    const withNok = gastonomoFormats(
      SPANISH,
      withCatalog(FALLBACK_SYMBOLS, [{ code: currencyOf('NOK'), name: 'Krone', symbol: 'kr' }]),
    );
    expect(withNok.amount(decimalOf('10'), currencyOf('NOK'))).toBe('kr 10,00');
    expect(withNok.amount(decimalOf('99.90'), EUR)).toBe('€ 99,90');
  });

  it('el importe va siempre en valor absoluto y el signo delante', () => {
    expect(formats.amount(decimalOf('-186400'))).toBe('$ 186.400');
    expect(formats.signed(decimalOf('2800000'), true)).toBe('+$ 2.800.000');
    expect(formats.signed(decimalOf('186400'), false)).toBe(`${MINUS}$ 186.400`);
  });

  /** El guion normal queda mas corto y desalineado en la tipografia del diseno. */
  it('un gasto usa el menos tipografico y nunca el guion', () => {
    expect(formats.signed(decimalOf('100'), false)).not.toContain('-');
    expect(formats.signed(decimalOf('100'), false).startsWith('−')).toBe(true);
  });

  it('el neto siempre lleva signo y el balance solo si es negativo', () => {
    expect(formats.net(decimalOf('0'))).toBe('+$ 0');
    expect(formats.net(decimalOf('-100'))).toBe(`${MINUS}$ 100`);
    expect(formats.balance(decimalOf('100'))).toBe('$ 100');
    expect(formats.balance(decimalOf('-100'))).toBe(`${MINUS}$ 100`);
  });

  it('la mascara del ojo lleva el simbolo y seis puntos', () => {
    expect(formats.masked()).toBe('$ ••••••');
  });

  it('un Money se formatea con su propia moneda', () => {
    expect(formats.amountOf(moneyOf(decimalOf('140'), USD))).toBe('US$ 140,00');
  });

  /**
   * Un importe de quince digitos no cabe en un float. Se le pasa la cadena al formateador,
   * no un `number`, y por eso no se pierde ninguna cifra.
   */
  it('un importe enorme no pierde cifras', () => {
    expect(formats.amount(decimalOf('123456789012345.0000'))).toBe('$ 123.456.789.012.345');
  });

  it('las tasas llevan mas decimales cuanto menores son, y sin ceros de relleno', () => {
    expect(formats.rate(decimalOf('0.870880'))).toBe('0,87088');
    expect(formats.rate(decimalOf('17.240200'))).toBe('17,2402');
    expect(formats.rate(decimalOf('3199.410000'))).toBe('3.199,41');
    expect(formats.rate(decimalOf('1.000000'))).toBe('1');
  });

  /** El idioma sale de Ajustes, no del navegador: el movil y la web deben coincidir. */
  it('en ingles cambian los separadores', () => {
    const english = gastonomoFormats(ENGLISH);
    expect(english.amount(decimalOf('140'), USD)).toBe('US$ 140.00');
    expect(english.amount(decimalOf('1250000'))).toBe('$ 1,250,000');
  });
});
