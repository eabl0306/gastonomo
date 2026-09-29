import { describe, expect, it, vi } from 'vitest';

import { PasswordResetGatewayError } from '../../application/ports/password-reset-gateway';

import { createHttpPasswordResetGateway } from './http-password-reset-gateway';

function respondWith(status: number, body?: unknown): typeof fetch {
  return vi.fn(() =>
    Promise.resolve(
      new Response(body === undefined ? null : JSON.stringify(body), {
        status,
        ...(body === undefined ? {} : { headers: { 'content-type': 'application/problem+json' } }),
      }),
    ),
  );
}

function gatewayOver(fetchImpl: typeof fetch) {
  return createHttpPasswordResetGateway({ baseUrl: 'https://api.test/', fetchImpl });
}

async function failureOf(promise: Promise<unknown>): Promise<PasswordResetGatewayError> {
  try {
    await promise;
  } catch (error: unknown) {
    if (error instanceof PasswordResetGatewayError) return error;
  }
  throw new Error('se esperaba un PasswordResetGatewayError');
}

describe('el adaptador de restablecer contrasena', () => {
  it('manda el token y la contrasena al endpoint, en el mismo origen', async () => {
    const fetchImpl = respondWith(204);

    await gatewayOver(fetchImpl).resetPassword('t0ken', 'contrasena9');

    expect(fetchImpl).toHaveBeenCalledWith(
      'https://api.test/v1/auth/password/reset',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ token: 't0ken', newPassword: 'contrasena9' }),
      }),
    );
  });

  /**
   * El endpoint esta limitado por frecuencia, asi que el 429 es alcanzable desde
   * que el limite se le colgo. El cuerpo es el que manda la API de verdad, no uno
   * vacio: con uno vacio se probaba el texto de reserva, que es justo el caso que
   * esta API nunca produce, y el test habria pasado igual sin nada que lo cubra.
   */
  it('el limite de peticiones llega con la explicacion de la API, no con el numero', async () => {
    const limited = await failureOf(
      gatewayOver(
        respondWith(429, {
          type: 'https://gastonomo.app/problems/too-many-requests',
          title: 'Demasiadas peticiones',
          status: 429,
          detail: 'Has hecho demasiados intentos seguidos. Prueba en un momento.',
        }),
      ).resetPassword('t', 'x'),
    );

    expect(limited.message).toBe('Has hecho demasiados intentos seguidos. Prueba en un momento.');
    expect(limited.message).not.toContain('429');
  });

  /**
   * Los dos errores de la API son los dos caminos de la pantalla: con un 401 no
   * hay nada que corregir en el formulario, con un 422 si. Mezclarlos dejaria al
   * usuario reescribiendo la contrasena contra un enlace muerto.
   */
  it('separa el enlace inservible de la contrasena rechazada', async () => {
    const expired = await failureOf(gatewayOver(respondWith(401)).resetPassword('t', 'x'));
    expect(expired.failure).toBe('link-unusable');

    const rejected = await failureOf(
      gatewayOver(
        respondWith(422, { errors: [{ field: 'newPassword', message: 'Usa al menos 8.' }] }),
      ).resetPassword('t', 'x'),
    );
    expect(rejected.failure).toBe('rejected');
    expect(rejected.message).toBe('Usa al menos 8.');
  });

  it('un cuerpo que no es JSON no tumba nada: queda el mensaje de reserva', async () => {
    const broken = vi.fn(() =>
      Promise.resolve(new Response('<html>vaya</html>', { status: 422 })),
    ) as unknown as typeof fetch;

    const failure = await failureOf(gatewayOver(broken).resetPassword('t', 'x'));

    expect(failure.failure).toBe('rejected');
    expect(failure.message).toBe('La API no acepto esa contrasena.');
  });

  it('sin red es inalcanzable, no un enlace caducado', async () => {
    const offline = vi.fn(() => Promise.reject(new Error('boom'))) as unknown as typeof fetch;

    const failure = await failureOf(gatewayOver(offline).resetPassword('t', 'x'));

    expect(failure.failure).toBe('unreachable');
  });
});
