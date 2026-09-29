import type { SessionStore } from '../../application/ports/session-store';

/** Lo que devuelve un refresco: el par rotado. */
export interface RotatedTokens {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly expiresInSeconds: number;
}

export interface TokenSourceOptions {
  readonly session: SessionStore;
  /** Rota el par contra la API. Lo pone la raiz de composicion. */
  readonly refresh: (refreshToken: string) => Promise<RotatedTokens | null>;
}

export interface TokenSource {
  /**
   * Un access token valido, refrescando si hace falta.
   *
   * @param rejected el token que la API acaba de rechazar con un 401, o null en la primera
   *   llamada. Si mientras se esperaba otra peticion ya refresco, el guardado sera distinto
   *   del rechazado y se usa sin gastar otro refresco.
   */
  accessToken: (rejected: string | null) => Promise<string | null>;
}

/**
 * De donde sale el `Bearer`, y lo mas delicado de la capa de red.
 *
 * `POST /v1/auth/refresh` **rota**: el token usado queda revocado y apunta al nuevo.
 * Presentar uno ya rotado significa, para la API, que alguien tiene una copia, asi que
 * revoca la familia entera y echa al usuario.
 *
 * Si dos peticiones fallan con 401 a la vez y las dos refrescan, la segunda presenta lo que
 * la primera ya rotó y la aplicacion se auto expulsa. Compartir la promesa lo impide: solo
 * una refresca y la otra se encuentra el token nuevo ya guardado.
 */
export function createTokenSource(options: TokenSourceOptions): TokenSource {
  let refreshing: Promise<string | null> | null = null;

  async function rotateOnce(): Promise<string | null> {
    const stored = options.session.read();
    if (stored === null) return null;
    const rotated = await options.refresh(stored.refreshToken);
    if (rotated === null) {
      // El refresh token guardado ya no sirve. Dejarlo solo invita a presentarlo otra vez,
      // y eso es lo que revoca la familia entera de la cuenta.
      options.session.clear();
      return null;
    }
    options.session.rotate(rotated);
    return rotated.accessToken;
  }

  return {
    accessToken: async (rejected) => {
      const current = options.session.read()?.accessToken ?? null;
      if (current !== null && current !== rejected) return current;
      refreshing ??= rotateOnce().finally(() => {
        refreshing = null;
      });
      return refreshing;
    },
  };
}
