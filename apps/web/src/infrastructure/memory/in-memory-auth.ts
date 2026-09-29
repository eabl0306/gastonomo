import type { ApiError, Outcome } from '../../application/ports/api-error';
import type { AuthGateway, RegistrationDraft } from '../../application/ports/auth-gateway';
import type { SessionStore, StoredSession } from '../../application/ports/session-store';
import type { Session } from '../../domain/account/session';
import type { User } from '../../domain/account/user';

export const SAMPLE_USER: User = {
  id: 'u1',
  name: 'Camilo Restrepo',
  email: 'camilo@correo.com',
};

export const SAMPLE_SESSION: Session = {
  accessToken: 'a1',
  expiresInSeconds: 900,
  refreshToken: 'r1',
  user: SAMPLE_USER,
};

/** Una sesion en memoria, con su aviso a quien escuche. Para tests y capturas. */
export function createMemorySessionStore(initial: StoredSession | null = null): SessionStore {
  let current = initial;
  const listeners = new Set<() => void>();
  const announce = (): void => {
    for (const listener of listeners) listener();
  };
  return {
    read: () => current,
    save: (session) => {
      current = {
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      };
      announce();
    },
    rotate: (tokens) => {
      if (current === null) return;
      current = { ...current, ...tokens };
      announce();
    },
    clear: () => {
      current = null;
      announce();
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
  };
}

export interface MemoryAuthOptions {
  /** El error con el que contestan `signIn` y `signUp`. Sin el, entran bien. */
  readonly failWith?: ApiError;
  readonly session?: Session;
}

/** El gateway de autenticacion en memoria. Guarda lo ultimo que recibio. */
export function createMemoryAuthGateway(options: MemoryAuthOptions = {}): AuthGateway & {
  readonly calls: { signIn: [string, string][]; signUp: RegistrationDraft[]; signOut: string[] };
} {
  const calls = {
    signIn: [] as [string, string][],
    signUp: [] as RegistrationDraft[],
    signOut: [] as string[],
  };
  const session = options.session ?? SAMPLE_SESSION;
  const answer = <T>(value: T): Outcome<T> =>
    options.failWith === undefined ? { ok: true, value } : { ok: false, error: options.failWith };

  return {
    calls,
    signIn: (email, password) => {
      calls.signIn.push([email, password]);
      return Promise.resolve(answer(session));
    },
    signUp: (draft) => {
      calls.signUp.push(draft);
      return Promise.resolve(answer(session));
    },
    refresh: () =>
      Promise.resolve(
        answer({
          accessToken: session.accessToken,
          expiresInSeconds: session.expiresInSeconds,
          refreshToken: session.refreshToken,
        }),
      ),
    signOut: (refreshToken) => {
      calls.signOut.push(refreshToken);
      return Promise.resolve({ ok: true, value: undefined });
    },
    profile: () => Promise.resolve(answer(session.user)),
    requestPasswordReset: () => Promise.resolve(answer(undefined)),
    changePassword: () => Promise.resolve(answer(undefined)),
  };
}
