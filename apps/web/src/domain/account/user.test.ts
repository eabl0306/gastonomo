import { describe, expect, it } from 'vitest';

import { COP } from '../money/currency';

import { DEFAULT_SETTINGS, localeOf } from './settings';
import { firstNameOf, initialsOf, type User } from './user';

function user(name: string, email = 'camilo@correo.com'): User {
  return { id: 'u1', name, email };
}

describe('user', () => {
  it('las iniciales son una o dos letras en mayuscula', () => {
    expect(initialsOf(user('Camilo Restrepo'))).toBe('CR');
    expect(initialsOf(user('camilo'))).toBe('C');
    expect(initialsOf(user('Ana Maria Restrepo Vargas'))).toBe('AM');
  });

  it('sin nombre, la inicial sale del correo', () => {
    expect(initialsOf(user('   '))).toBe('C');
  });

  it('el saludo usa el primer nombre', () => {
    expect(firstNameOf(user('  Camilo Restrepo '))).toBe('Camilo');
    expect(firstNameOf(user(''))).toBe('');
  });
});

describe('settings', () => {
  it('una cuenta nueva empieza en pesos y en castellano', () => {
    expect(DEFAULT_SETTINGS.displayCurrency).toBe(COP);
    expect(DEFAULT_SETTINGS.language).toBe('es');
  });

  it('cada idioma trae su locale de formato', () => {
    expect(localeOf('es')).toBe('es-CO');
    expect(localeOf('en')).toBe('en-US');
  });
});
