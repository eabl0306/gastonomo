import { describe, expect, it } from 'vitest';

import {
  ONE,
  ZERO,
  absDecimal,
  addDecimals,
  compareDecimals,
  decimalOf,
  decimalToFixed,
  decimalToString,
  decimalsAreEqual,
  divideDecimals,
  fromInteger,
  isZero,
  multiplyDecimals,
  negateDecimal,
  parseDecimal,
  roundDecimal,
  signOf,
  subtractDecimals,
  trimDecimal,
} from './decimal';

/**
 * El dinero de esta aplicacion es decimal exacto y nunca un float. Aqui se comprueba
 * justamente eso: las cuentas que un `number` haria mal.
 */
describe('decimal', () => {
  it('suma sin el error del binario', () => {
    // 0.1 + 0.2 en punto flotante da 0.30000000000000004.
    expect(decimalToString(addDecimals(decimalOf('0.1'), decimalOf('0.2')))).toBe('0.3');
  });

  it('conserva los cuatro decimales con los que viaja un importe', () => {
    const amount = decimalOf('186400.0000');
    expect(amount.scale).toBe(4);
    expect(decimalToString(amount)).toBe('186400.0000');
  });

  it('alinea escalas distintas antes de operar', () => {
    expect(decimalToString(addDecimals(decimalOf('1.5'), decimalOf('0.25')))).toBe('1.75');
    expect(decimalToString(subtractDecimals(decimalOf('1'), decimalOf('0.0001')))).toBe('0.9999');
  });

  it('multiplica sumando las escalas, sin perder un digito', () => {
    const product = multiplyDecimals(decimalOf('140'), decimalOf('3199.41'));
    expect(decimalToString(product)).toBe('447917.40');
  });

  it('divide con los decimales que se le pidan', () => {
    const third = divideDecimals(decimalOf('1'), decimalOf('3'), 6);
    expect(third).not.toBeNull();
    expect(decimalToString(third!)).toBe('0.333333');
  });

  it('dividir entre cero no da infinito: da null', () => {
    expect(divideDecimals(decimalOf('1'), ZERO)).toBeNull();
  });

  it('redondea half-up alejandose del cero, como la app de Android', () => {
    expect(decimalToString(roundDecimal(decimalOf('0.5'), 0))).toBe('1');
    expect(decimalToString(roundDecimal(decimalOf('-0.5'), 0))).toBe('-1');
    expect(decimalToString(roundDecimal(decimalOf('2.345'), 2))).toBe('2.35');
    expect(decimalToString(roundDecimal(decimalOf('-2.345'), 2))).toBe('-2.35');
  });

  it('redondear hacia arriba de escala no inventa precision, solo rellena', () => {
    expect(decimalToString(roundDecimal(decimalOf('2.5'), 4))).toBe('2.5000');
  });

  it('compara importes de escalas distintas', () => {
    expect(compareDecimals(decimalOf('1.50'), decimalOf('1.5'))).toBe(0);
    expect(decimalsAreEqual(decimalOf('1.50'), decimalOf('1.5'))).toBe(true);
    expect(compareDecimals(decimalOf('1.5'), decimalOf('1.51'))).toBeLessThan(0);
    expect(compareDecimals(decimalOf('-1'), decimalOf('-2'))).toBeGreaterThan(0);
  });

  it('sabe su signo', () => {
    expect(signOf(decimalOf('-0.0001'))).toBe(-1);
    expect(signOf(decimalOf('0.0000'))).toBe(0);
    expect(signOf(ONE)).toBe(1);
    expect(isZero(decimalOf('-0.00'))).toBe(true);
  });

  it('niega y toma el valor absoluto', () => {
    expect(decimalToString(negateDecimal(decimalOf('12.5')))).toBe('-12.5');
    expect(decimalToString(absDecimal(decimalOf('-12.5')))).toBe('12.5');
    expect(decimalToString(absDecimal(decimalOf('12.5')))).toBe('12.5');
  });

  it('lee el signo y los espacios de una cadena', () => {
    expect(decimalToString(decimalOf('  -12.50 '))).toBe('-12.50');
    expect(decimalToString(decimalOf('+7'))).toBe('7');
  });

  it('rechaza lo que no es un decimal', () => {
    for (const raw of ['', '   ', 'abc', '1e3', '1.', '.5', '--1', '1.2.3', 'Infinity', 'NaN']) {
      expect(parseDecimal(raw)).toBeNull();
    }
  });

  /** Aceptar cien decimales solo sirve para operar con enteros gigantes sin motivo. */
  it('rechaza mas decimales de los que el contrato puede mandar', () => {
    expect(parseDecimal(`0.${'1'.repeat(20)}`)).not.toBeNull();
    expect(parseDecimal(`0.${'1'.repeat(21)}`)).toBeNull();
  });

  it('escribe un importe con los decimales que se le pidan', () => {
    expect(decimalToFixed(decimalOf('45000'), 4)).toBe('45000.0000');
    expect(decimalToFixed(decimalOf('45000.456'), 2)).toBe('45000.46');
  });

  it('quita los ceros de relleno para meterlo en un campo', () => {
    expect(decimalToString(trimDecimal(decimalOf('12.5000')))).toBe('12.5');
    expect(decimalToString(trimDecimal(decimalOf('45000.0000')))).toBe('45000');
    expect(decimalToString(trimDecimal(ZERO))).toBe('0');
  });

  it('un entero se puede escribir sin pasar por una cadena', () => {
    expect(decimalToString(fromInteger(100))).toBe('100');
  });

  /**
   * El caso que de verdad importa: convertir 140 dolares a pesos con la tasa del prototipo
   * y redondear a los decimales del peso. Con floats esto pierde centavos.
   */
  it('convierte y redondea sin perder centavos', () => {
    const rate = divideDecimals(decimalOf('3199.41'), decimalOf('1'));
    expect(rate).not.toBeNull();
    const converted = multiplyDecimals(decimalOf('140'), rate!);
    expect(decimalToString(roundDecimal(converted, 0))).toBe('447917');
  });
});
