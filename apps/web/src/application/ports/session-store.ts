import type { Session, TokenPair } from '../../domain/account/session';
import type { User } from '../../domain/account/user';

/** Lo que la web guarda de una sesion entre recargas. */
export interface StoredSession {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly user: User;
}

/**
 * Donde vive la sesion.
 *
 * Es sincrono porque el almacenamiento del navegador lo es, y porque el armazon tiene que
 * saber si hay sesion **antes** del primer pintado: con una promesa, la aplicacion
 * parpadearia del login a Inicio en cada recarga.
 */
export interface SessionStore {
  read: () => StoredSession | null;
  save: (session: Session) => void;
  /** Guarda el par rotado sin tocar el usuario. Es lo que devuelve un refresco. */
  rotate: (tokens: TokenPair) => void;
  clear: () => void;
  /** Avisa de los cambios, incluidos los de otra pestana. Devuelve como dejar de escuchar. */
  subscribe: (listener: () => void) => () => void;
}
