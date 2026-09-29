import { describe, expect, it } from 'vitest';

import {
  MAXIMUM_PASSWORD_LENGTH,
  MINIMUM_PASSWORD_LENGTH,
  checkPassword,
  measurePassword,
} from './password';

describe('la regla de la contrasena', () => {
  it('acepta la longitud que acepta la API', () => {
    expect(checkPassword('a'.repeat(MINIMUM_PASSWORD_LENGTH))).toBeNull();
    expect(checkPassword('a'.repeat(MAXIMUM_PASSWORD_LENGTH))).toBeNull();
  });

  it('dice por que no vale, para poder explicarlo sin ir a la API', () => {
    expect(checkPassword('')).toBe('empty');
    expect(checkPassword('a'.repeat(MINIMUM_PASSWORD_LENGTH - 1))).toBe('too-short');
    expect(checkPassword('a'.repeat(MAXIMUM_PASSWORD_LENGTH + 1))).toBe('too-long');
  });
});

describe('el medidor de fuerza', () => {
  it('sube por tramos y premia mezclar letras y numeros', () => {
    expect(measurePassword('')).toBe(0);
    expect(measurePassword('abc')).toBe(1);
    expect(measurePassword('abcdef')).toBe(2);
    expect(measurePassword('abcdefgh')).toBe(3);
    expect(measurePassword('abcdefg1')).toBe(4);
  });

  it('no es una regla de seguridad: una larga sin numeros sigue valiendo', () => {
    expect(checkPassword('correohorsebattery')).toBeNull();
    expect(measurePassword('correohorsebattery')).toBe(3);
  });
});
