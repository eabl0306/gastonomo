import { describe, expect, it } from 'vitest';

import { failure, messageFor, success, valueOrNull, type ApiError } from './api-error';

const VALIDATION: ApiError = {
  kind: 'validation',
  fields: [
    { field: 'email', message: 'Ese correo ya tiene cuenta.' },
    { field: 'amount', message: 'El importe es obligatorio.' },
  ],
  detail: 'Revisa los campos marcados.',
};

describe('api error', () => {
  /** El mensaje de un 422 ya viene en castellano y se pinta junto a su campo. */
  it('saca el mensaje del campo que pregunta', () => {
    expect(messageFor(VALIDATION, 'email')).toBe('Ese correo ya tiene cuenta.');
    expect(messageFor(VALIDATION, 'amount')).toBe('El importe es obligatorio.');
  });

  it('un campo del que el 422 no hablaba no tiene mensaje', () => {
    expect(messageFor(VALIDATION, 'password')).toBeNull();
  });

  it('un error que no es de validacion no tiene mensajes por campo', () => {
    expect(messageFor({ kind: 'network' }, 'email')).toBeNull();
    expect(messageFor({ kind: 'conflict', code: 'x', detail: null }, 'email')).toBeNull();
  });

  it('un resultado envuelve su valor o su error', () => {
    expect(success(42)).toEqual({ ok: true, value: 42 });
    expect(failure({ kind: 'not-found' })).toEqual({ ok: false, error: { kind: 'not-found' } });
  });

  it('el valor, o null si la llamada fallo', () => {
    expect(valueOrNull(success('hola'))).toBe('hola');
    expect(valueOrNull(failure({ kind: 'network' }))).toBeNull();
  });
});
