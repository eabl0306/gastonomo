import { describe, expect, it, vi } from 'vitest';

import type { SessionStore, StoredSession } from '../../application/ports/session-store';

import { createApiClient } from './api-client';
import type { RotatedTokens } from './api-tokens';

const USER = { id: 'u1', name: 'Camilo', email: 'camilo@correo.com' };

function storeOf(initial: StoredSession | null): SessionStore & { current: StoredSession | null } {
  const store = {
    current: initial,
    read: () => store.current,
    save: () => undefined,
    rotate: (tokens: { accessToken: string; refreshToken: string }) => {
      store.current = { ...tokens, user: USER };
    },
    clear: () => {
      store.current = null;
    },
    subscribe: () => () => undefined,
  };
  return store;
}

/** Las llamadas a `fetch`, ya tipadas: el doble las guarda como `unknown[]`. */
function callsOf(mock: { mock: { calls: unknown[][] } }): [string, RequestInit][] {
  return mock.mock.calls as [string, RequestInit][];
}

function headerOf(init: RequestInit, name: string): string | undefined {
  return (init.headers as Record<string, string> | undefined)?.[name];
}

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'content-type': 'application/json' },
    ...init,
  });
}

describe('api client', () => {
  it('manda el Bearer de la sesion guardada', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() => Promise.resolve(jsonResponse({ ok: true })));
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    await client.request('me');

    const [url, init] = callsOf(fetchImpl)[0]!;
    expect(url).toBe('/v1/me');
    expect(headerOf(init, 'authorization')).toBe('Bearer a1');
  });

  it('las rutas de autenticacion van sin Bearer, y su 401 son credenciales', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() => Promise.resolve(jsonResponse({ title: 'no' }, { status: 401 })));
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    const outcome = await client.request('auth/login', { method: 'POST', anonymous: true });

    expect(outcome.ok).toBe(false);
    expect(outcome.ok ? null : outcome.error.kind).toBe('invalid-credentials');
    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });

  it('ante un 401 refresca una vez y reintenta', async () => {
    const session = storeOf({ accessToken: 'viejo', refreshToken: 'r1', user: USER });
    const fetchImpl = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({}, { status: 401 }))
      .mockResolvedValueOnce(jsonResponse({ ok: true }));
    const refresh = vi.fn((): Promise<RotatedTokens> =>
      Promise.resolve({ accessToken: 'nuevo', refreshToken: 'r2', expiresInSeconds: 900 }),
    );
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh,
      fetchImpl,
    });

    const outcome = await client.request('movements');

    expect(outcome.ok).toBe(true);
    expect(refresh).toHaveBeenCalledTimes(1);
    const [, second] = callsOf(fetchImpl)[1]!;
    expect(headerOf(second, 'authorization')).toBe('Bearer nuevo');
    expect(session.current?.refreshToken).toBe('r2');
  });

  /**
   * Refrescar ROTA: presentar un token ya rotado revoca la familia entera y echa al
   * usuario. Dos 401 a la vez tienen que compartir un solo refresco.
   */
  it('dos peticiones que caducan a la vez refrescan una sola vez', async () => {
    const session = storeOf({ accessToken: 'viejo', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn((_url: URL | RequestInfo, init?: RequestInit) => {
      const auth = init === undefined ? undefined : headerOf(init, 'authorization');
      return Promise.resolve(
        auth === 'Bearer viejo' ? jsonResponse({}, { status: 401 }) : jsonResponse({ ok: true }),
      );
    });
    const refresh = vi.fn(async (): Promise<RotatedTokens> => {
      await new Promise((resolve) => {
        setTimeout(resolve, 5);
      });
      return { accessToken: 'nuevo', refreshToken: 'r2', expiresInSeconds: 900 };
    });
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh,
      fetchImpl,
    });

    const [first, second] = await Promise.all([
      client.request('movements'),
      client.request('loans'),
    ]);

    expect(first.ok).toBe(true);
    expect(second.ok).toBe(true);
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('si el refresco falla, se limpia la sesion y la llamada queda sin autorizar', async () => {
    const session = storeOf({ accessToken: 'viejo', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() => Promise.resolve(jsonResponse({}, { status: 401 })));
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    const outcome = await client.request('movements');

    expect(outcome.ok ? null : outcome.error.kind).toBe('unauthorized');
    expect(session.current).toBeNull();
  });

  it('traduce un 422 a sus errores por campo', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() =>
      Promise.resolve(
        jsonResponse(
          {
            detail: 'Revisa los campos',
            errors: [{ field: 'amount', message: 'Falta el importe' }],
          },
          { status: 422 },
        ),
      ),
    );
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    const outcome = await client.request('movements', { method: 'POST', body: {} });

    expect(outcome.ok).toBe(false);
    if (outcome.ok) return;
    expect(outcome.error.kind).toBe('validation');
    if (outcome.error.kind !== 'validation') return;
    expect(outcome.error.fields).toEqual([{ field: 'amount', message: 'Falta el importe' }]);
  });

  it('una peticion que no llega es un error de red, no un 500', async () => {
    const session = storeOf(null);
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl: () => Promise.reject(new Error('sin red')),
    });

    const outcome = await client.request('health/live', { anonymous: true });

    expect(outcome.ok ? null : outcome.error.kind).toBe('network');
  });

  it('manda la clave de idempotencia y lee si fue un reintento', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() =>
      Promise.resolve(jsonResponse({ id: 'm1' }, { headers: { 'idempotent-replay': 'true' } })),
    );
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    const outcome = await client.request('movements', {
      method: 'POST',
      body: {},
      idempotencyKey: 'k1',
    });

    const [, init] = callsOf(fetchImpl)[0]!;
    expect(headerOf(init, 'idempotency-key')).toBe('k1');
    expect(outcome.ok && outcome.value.isReplay).toBe(true);
  });

  it('los parametros de consulta viajan en la direccion', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER });
    const fetchImpl = vi.fn(() => Promise.resolve(jsonResponse({ items: [] })));
    const client = createApiClient({
      baseUrl: '',
      session,
      refresh: () => Promise.resolve(null),
      fetchImpl,
    });

    await client.request('movements', {
      query: { from: '2026-09-01', to: '2026-09-30', kind: undefined, limit: 50 },
    });

    expect(callsOf(fetchImpl)[0]?.[0]).toBe('/v1/movements?from=2026-09-01&to=2026-09-30&limit=50');
  });
});
