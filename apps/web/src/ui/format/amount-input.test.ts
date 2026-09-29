import { describe, expect, it } from 'vitest';

import { COP, USD } from '../../domain/money/currency';
import { decimalOf, decimalToString } from '../../domain/money/decimal';

import { amountInput, amountToRaw, caretAfter, digitsBefore, parseAmount } from './amount-input';
import { localizedNumbers } from './localized-numbers';

const spanish = amountInput(localizedNumbers('es-CO'));
const english = amountInput(localizedNumbers('en-US'));

describe('teclear un importe', () => {
  it('deja solo digitos y un separador', () => {
    expect(spanish.sanitize('1a2b3', COP)).toBe('123');
    // El segundo separador se ignora, y lo que viene detras ya no cabe en dos decimales.
    expect(spanish.sanitize('12,50,25', USD)).toBe('12.50');
  });

  /** Con las reglas del peso, teclear una coma en un importe en pesos no significa nada. */
  it('una moneda sin fraccion no admite separador', () => {
    expect(spanish.sanitize('1250,75', COP)).toBe('125075');
    expect(spanish.sanitize('12,50', USD)).toBe('12.50');
  });

  it('recorta los decimales a los de la moneda', () => {
    expect(spanish.sanitize('12,5099', USD)).toBe('12.50');
  });

  it('quita los ceros a la izquierda y corta a once digitos enteros', () => {
    expect(spanish.sanitize('000123', COP)).toBe('123');
    expect(spanish.sanitize('1234567890123', COP)).toBe('12345678901');
  });

  /** Quien empieza por la coma esta escribiendo los decimales: la parte entera es cero. */
  it('teclear solo el separador deja un cero delante', () => {
    expect(spanish.sanitize(',5', USD)).toBe('0.5');
  });

  it('el separador es el del idioma', () => {
    expect(english.sanitize('12.50', USD)).toBe('12.50');
    expect(english.sanitize('12,50', USD)).toBe('1250');
  });

  it('pinta el valor con la agrupacion del idioma', () => {
    expect(spanish.display('1250000.5')).toBe('1.250.000,5');
    expect(english.display('1250000.5')).toBe('1,250,000.5');
    expect(spanish.display('')).toBe('');
  });
});

describe('leer y escribir valores raw', () => {
  it('lo que esta a medio teclear vale cero', () => {
    expect(decimalToString(parseAmount(''))).toBe('0');
    expect(decimalToString(parseAmount('12.'))).toBe('12');
    expect(decimalToString(parseAmount('12.50'))).toBe('12.50');
  });

  it('un valor calculado sale redondeado a la moneda y sin ceros de relleno', () => {
    expect(amountToRaw(decimalOf('9915172.9819'), COP)).toBe('9915173');
    expect(amountToRaw(decimalOf('12.5000'), USD)).toBe('12.5');
    expect(amountToRaw(decimalOf('0.0000'), USD)).toBe('0');
  });
});

/**
 * El campo reescribe el texto en cada pulsacion, asi que teclear un digito puede meter un
 * separador de miles y correr todo lo que hay detras. Sin esto el cursor se va al final.
 */
describe('el cursor al reescribir', () => {
  it('cuenta los digitos que hay antes de la posicion', () => {
    expect(digitsBefore('1.250,5', 4)).toBe(3);
    expect(digitsBefore('1.250,5', 5)).toBe(4);
    expect(digitsBefore('1.250,5', 0)).toBe(0);
  });

  it('vuelve a colocarse detras del mismo digito', () => {
    expect(caretAfter('1.250,5', 3)).toBe(4);
    expect(caretAfter('1.250,5', 0)).toBe(0);
    expect(caretAfter('1.250,5', 99)).toBe(7);
  });
});
