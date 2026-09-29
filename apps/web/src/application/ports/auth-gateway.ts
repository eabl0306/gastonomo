import type { Session, TokenPair } from '../../domain/account/session';
import type { User } from '../../domain/account/user';

import type { Outcome } from './api-error';

/** Lo que se manda a `POST /v1/auth/register`. */
export interface RegistrationDraft {
  readonly name: string;
  readonly email: string;
  readonly password: string;
  readonly acceptedTerms: boolean;
}

/** Autenticacion contra la API. */
export interface AuthGateway {
  signIn: (email: string, password: string) => Promise<Outcome<Session>>;
  signUp: (draft: RegistrationDraft) => Promise<Outcome<Session>>;
  /** Rota el par. El token usado queda revocado, asi que hay que guardar el nuevo. */
  refresh: (refreshToken: string) => Promise<Outcome<TokenPair>>;
  /** Revoca el refresh token. Un fallo no impide salir: la sesion local se borra igual. */
  signOut: (refreshToken: string) => Promise<Outcome<void>>;
  /** `GET /v1/me`. */
  profile: () => Promise<Outcome<User>>;
  requestPasswordReset: (email: string) => Promise<Outcome<void>>;
  /**
   * Cambia la contrasena desde dentro.
   *
   * Se manda el refresh token de esta sesion para no cerrarla: sin el, la API revoca todas y
   * quien acaba de cambiar su contrasena se encuentra en la pantalla de entrar.
   */
  changePassword: (
    currentPassword: string,
    newPassword: string,
    refreshToken: string,
  ) => Promise<Outcome<void>>;
}
