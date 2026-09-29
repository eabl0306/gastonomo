import { failure, success, type Outcome } from '../../application/ports/api-error';
import type { AuthGateway, RegistrationDraft } from '../../application/ports/auth-gateway';
import type { Session, TokenPair } from '../../domain/account/session';
import type { User } from '../../domain/account/user';

import type { ApiClient } from './api-client';
import { readSession, readTokens, readUser } from './auth-dto';

/**
 * El cuerpo llego pero no tiene la forma del contrato. No es un fallo de red ni un error de
 * la API: es algo que no se puede usar, y tratarlo como sesion valida dejaria al usuario
 * dentro sin token.
 */
function malformed<T>(): Outcome<T> {
  return failure({ kind: 'unexpected', status: null, detail: null });
}

function sessionFrom(body: unknown): Outcome<Session> {
  const session = readSession(body);
  return session === null ? malformed<Session>() : success(session);
}

/** Un POST publico: los de autenticacion no llevan Bearer. */
async function postPublic(
  client: ApiClient,
  path: string,
  body: unknown,
): Promise<Outcome<unknown>> {
  const outcome = await client.request(path, { method: 'POST', body, anonymous: true });
  return outcome.ok ? success(outcome.value.body) : outcome;
}

async function openSession(
  client: ApiClient,
  path: string,
  body: unknown,
): Promise<Outcome<Session>> {
  const outcome = await postPublic(client, path, body);
  return outcome.ok ? sessionFrom(outcome.value) : outcome;
}

/** Autenticacion contra la API. */
export function createHttpAuthGateway(client: ApiClient): AuthGateway {
  return {
    signIn: (email, password) => openSession(client, 'auth/login', { email, password }),

    signUp: (draft: RegistrationDraft) => openSession(client, 'auth/register', draft),

    refresh: async (refreshToken) => {
      const outcome = await postPublic(client, 'auth/refresh', { refreshToken });
      if (!outcome.ok) return outcome;
      const tokens = readTokens(outcome.value);
      return tokens === null ? malformed<TokenPair>() : success(tokens);
    },

    signOut: async (refreshToken) => {
      const outcome = await postPublic(client, 'auth/logout', { refreshToken });
      return outcome.ok ? success(undefined) : outcome;
    },

    profile: async () => {
      const outcome = await client.request('me');
      if (!outcome.ok) return outcome;
      const user = readUser(outcome.value.body);
      return user === null ? malformed<User>() : success(user);
    },

    requestPasswordReset: async (email) => {
      const outcome = await postPublic(client, 'auth/password/forgot', { email });
      return outcome.ok ? success(undefined) : outcome;
    },

    changePassword: async (currentPassword, newPassword, refreshToken) => {
      const outcome = await client.request('auth/password/change', {
        method: 'POST',
        body: { currentPassword, newPassword, refreshToken },
      });
      return outcome.ok ? success(undefined) : outcome;
    },
  };
}
