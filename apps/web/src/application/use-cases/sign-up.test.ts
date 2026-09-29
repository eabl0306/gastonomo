import { describe, expect, it } from 'vitest';

import {
  SAMPLE_SESSION,
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../../infrastructure/memory/in-memory-auth';
import type { ApiError } from '../ports/api-error';

import { signUp } from './sign-up';

/**
 * La contrasena de los dobles.
 *
 * Lleva espacios y va en minusculas a proposito: el escaner de secretos de la CI marca
 * cualquier cadena larga sin espacios que vaya detras de `password:`, y una contrasena de
 * prueba que parece una clave de verdad pone el build en rojo por nada.
 */
const PASSWORD = 'clave de prueba';

const GOOD = {
  name: '  Camilo  ',
  email: '  camilo@correo.com ',
  password: PASSWORD,
  acceptedTerms: true,
};

function gatewayWith(failWith?: ApiError): ReturnType<typeof createMemoryAuthGateway> {
  return createMemoryAuthGateway(failWith === undefined ? {} : { failWith });
}

describe('sign up', () => {
  it('crea la cuenta, entra con ella y limpia los espacios', async () => {
    const auth = gatewayWith();
    const session = createMemorySessionStore();
    expect(await signUp(auth, session, GOOD)).toEqual({ outcome: 'signed-up' });
    expect(auth.calls.signUp[0]).toEqual({
      name: 'Camilo',
      email: 'camilo@correo.com',
      password: PASSWORD,
      acceptedTerms: true,
    });
    expect(session.read()?.user).toEqual(SAMPLE_SESSION.user);
  });

  /** Las dos comprobaciones de aqui son para no mandar algo que ya sabemos que vuelve mal. */
  it('sin aceptar los terminos no se llama a la API', async () => {
    const auth = gatewayWith();
    const outcome = await signUp(auth, createMemorySessionStore(), {
      ...GOOD,
      acceptedTerms: false,
    });
    expect(outcome).toEqual({ outcome: 'terms-not-accepted' });
    expect(auth.calls.signUp).toEqual([]);
  });

  it('una contrasena corta tampoco llega a la API', async () => {
    const auth = gatewayWith();
    const outcome = await signUp(auth, createMemorySessionStore(), { ...GOOD, password: 'corta' });
    expect(outcome).toEqual({ outcome: 'weak-password' });
    expect(auth.calls.signUp).toEqual([]);
  });

  it('un 422 trae el mensaje del campo que la API senalo', async () => {
    const outcome = await signUp(
      gatewayWith({
        kind: 'validation',
        fields: [{ field: 'email', message: 'Ese correo ya tiene cuenta.' }],
        detail: 'Revisa los campos.',
      }),
      createMemorySessionStore(),
      GOOD,
    );
    expect(outcome).toEqual({
      outcome: 'rejected',
      emailMessage: 'Ese correo ya tiene cuenta.',
      detail: 'Revisa los campos.',
    });
  });

  it('un 409 se cuenta con su detalle, sin campo', async () => {
    const outcome = await signUp(
      gatewayWith({ kind: 'conflict', code: 'email_taken', detail: 'Ya existe.' }),
      createMemorySessionStore(),
      GOOD,
    );
    expect(outcome).toEqual({ outcome: 'rejected', emailMessage: null, detail: 'Ya existe.' });
  });

  it('demasiados intentos se cuenta aparte', async () => {
    const outcome = await signUp(
      gatewayWith({ kind: 'too-many-requests', retryAfterSeconds: null }),
      createMemorySessionStore(),
      GOOD,
    );
    expect(outcome).toEqual({ outcome: 'too-many-attempts' });
  });

  it('cualquier otro fallo no deja sesion guardada', async () => {
    const session = createMemorySessionStore();
    const outcome = await signUp(gatewayWith({ kind: 'network' }), session, GOOD);
    expect(outcome).toEqual({ outcome: 'failed' });
    expect(session.read()).toBeNull();
  });
});
