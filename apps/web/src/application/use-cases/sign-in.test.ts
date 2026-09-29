import { describe, expect, it, vi } from 'vitest';

import type { Session } from '../../domain/account/session';
import type { ApiError, Outcome } from '../ports/api-error';
import type { AuthGateway } from '../ports/auth-gateway';
import type { SessionStore, StoredSession } from '../ports/session-store';

import { signIn } from './sign-in';
import { signOut } from './sign-out';

const SESSION: Session = {
  accessToken: 'a1',
  expiresInSeconds: 900,
  refreshToken: 'r1',
  user: { id: 'u1', name: 'Camilo', email: 'camilo@correo.com' },
};

function storeOf(
  initial: StoredSession | null = null,
): SessionStore & { current: StoredSession | null } {
  const store = {
    current: initial,
    read: () => store.current,
    save: (session: Session) => {
      store.current = {
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      };
    },
    rotate: () => undefined,
    clear: () => {
      store.current = null;
    },
    subscribe: () => () => undefined,
  };
  return store;
}

function gatewayOf(result: Outcome<Session>, signOutResult?: Outcome<void>): AuthGateway {
  return {
    signIn: () => Promise.resolve(result),
    signUp: () => Promise.resolve(result),
    refresh: () => Promise.resolve({ ok: false, error: { kind: 'unauthorized' } }),
    signOut: () => Promise.resolve(signOutResult ?? { ok: true, value: undefined }),
    profile: () => Promise.resolve({ ok: false, error: { kind: 'unauthorized' } }),
    requestPasswordReset: () => Promise.resolve({ ok: true, value: undefined }),
  };
}

function withError(error: ApiError): Outcome<Session> {
  return { ok: false, error };
}

describe('sign in', () => {
  it('al entrar guarda la sesion antes de decir que si', async () => {
    const session = storeOf();
    const outcome = await signIn(
      gatewayOf({ ok: true, value: SESSION }),
      session,
      'camilo@correo.com',
      'clave',
    );
    expect(outcome).toEqual({ outcome: 'signed-in' });
    expect(session.current?.accessToken).toBe('a1');
  });

  it('quita los espacios del correo, que es lo que sobra al pegarlo', async () => {
    const gateway = gatewayOf({ ok: true, value: SESSION });
    const spy = vi.spyOn(gateway, 'signIn');
    await signIn(gateway, storeOf(), '  camilo@correo.com  ', 'clave');
    expect(spy).toHaveBeenCalledWith('camilo@correo.com', 'clave');
  });

  /** La API no dice si fallo el correo o la contrasena, y decirlo confirmaria la cuenta. */
  it('un 401 son credenciales incorrectas, sin decir cual', async () => {
    const outcome = await signIn(
      gatewayOf(withError({ kind: 'invalid-credentials' })),
      storeOf(),
      'camilo@correo.com',
      'mal',
    );
    expect(outcome).toEqual({ outcome: 'invalid-credentials' });
  });

  it('demasiados intentos dice cuanto hay que esperar si la API lo manda', async () => {
    const outcome = await signIn(
      gatewayOf(withError({ kind: 'too-many-requests', retryAfterSeconds: 60 })),
      storeOf(),
      'camilo@correo.com',
      'clave',
    );
    expect(outcome).toEqual({ outcome: 'too-many-attempts', retryAfterSeconds: 60 });
  });

  it('cualquier otro fallo no guarda nada', async () => {
    const session = storeOf();
    const outcome = await signIn(
      gatewayOf(withError({ kind: 'network' })),
      session,
      'camilo@correo.com',
      'clave',
    );
    expect(outcome).toEqual({ outcome: 'failed' });
    expect(session.current).toBeNull();
  });
});

describe('sign out', () => {
  it('revoca el refresh token y borra la sesion', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: SESSION.user });
    const gateway = gatewayOf({ ok: true, value: SESSION });
    const spy = vi.spyOn(gateway, 'signOut');
    await signOut(gateway, session);
    expect(spy).toHaveBeenCalledWith('r1');
    expect(session.current).toBeNull();
  });

  /** Pulsar "Cerrar sesion" y quedarse dentro es peor que un token que caduca solo. */
  it('si revocar falla, la sesion local se borra igual', async () => {
    const session = storeOf({ accessToken: 'a1', refreshToken: 'r1', user: SESSION.user });
    await signOut(gatewayOf({ ok: true, value: SESSION }, withError({ kind: 'network' })), session);
    expect(session.current).toBeNull();
  });

  it('sin sesion no llama a la API', async () => {
    const gateway = gatewayOf({ ok: true, value: SESSION });
    const spy = vi.spyOn(gateway, 'signOut');
    await signOut(gateway, storeOf());
    expect(spy).not.toHaveBeenCalled();
  });
});
