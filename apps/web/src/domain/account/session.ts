import type { User } from './user';

/**
 * Lo que devuelven `register` y `login`.
 *
 * El access token es un JWT de 15 minutos y puede vivir en memoria; el refresh es opaco y
 * de 30 dias.
 */
export interface Session {
  readonly accessToken: string;
  readonly expiresInSeconds: number;
  readonly refreshToken: string;
  readonly user: User;
}

/**
 * El par de tokens sin el usuario, que es lo que devuelve un refresco.
 *
 * `POST /v1/auth/refresh` **rota**: hay que guardar el nuevo y tirar el viejo. Presentar
 * uno ya rotado revoca la familia entera y echa al usuario.
 */
export interface TokenPair {
  readonly accessToken: string;
  readonly expiresInSeconds: number;
  readonly refreshToken: string;
}
