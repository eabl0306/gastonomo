import { z } from 'zod';

import type { SessionStore, StoredSession } from '../../application/ports/session-store';

const KEY = 'gastonomo.session';

const storedSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  user: z.object({ id: z.string(), name: z.string(), email: z.string() }),
});

/**
 * La sesion en el almacenamiento del navegador.
 *
 * **El refresh token queda al alcance de cualquier script que se cuele en la pagina.** La
 * alternativa buena es una cookie `HttpOnly`, y esa la tiene que poner la API: hoy devuelve
 * los tokens en el cuerpo, asi que lo unico que puede hacer la web es guardarlos. Se elige
 * `localStorage` sobre `sessionStorage` porque el diseno da por hecho que volver a la
 * pestana no pide entrar otra vez.
 *
 * Un cuerpo guardado que no cuadra con el esquema se borra en vez de usarse a medias: una
 * sesion incompleta manda peticiones sin token y echa al usuario de formas raras.
 */
function parseStored(storage: Storage, raw: string): StoredSession | null {
  try {
    const parsed = storedSchema.safeParse(JSON.parse(raw));
    if (parsed.success) return parsed.data;
  } catch {
    // Cae al borrado de abajo: lo guardado no es ni JSON.
  }
  storage.removeItem(KEY);
  return null;
}

/**
 * Lee la sesion y **devuelve siempre el mismo objeto mientras no cambie**.
 *
 * La cache no es una optimizacion: `useSyncExternalStore` compara el resultado por
 * identidad, asi que devolver un objeto nuevo en cada lectura lo hace volver a pintar sin
 * parar. React lo corta con "Maximum update depth exceeded" y la pagina se queda en blanco.
 * Aqui pasaba, porque cada lectura parsea el JSON de `localStorage`.
 */
function createReader(storage: Storage): () => StoredSession | null {
  let cachedRaw: string | null = null;
  let cached: StoredSession | null = null;
  return () => {
    const raw = storage.getItem(KEY);
    if (raw === cachedRaw) return cached;
    cachedRaw = raw;
    cached = raw === null ? null : parseStored(storage, raw);
    return cached;
  };
}

export function createLocalSessionStore(storage: Storage = globalThis.localStorage): SessionStore {
  const listeners = new Set<() => void>();

  function announce(): void {
    for (const listener of listeners) listener();
  }

  const read = createReader(storage);

  function write(session: StoredSession): void {
    storage.setItem(KEY, JSON.stringify(session));
    announce();
  }

  return {
    read,
    save: (session) => {
      write({
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      });
    },
    rotate: (tokens) => {
      const current = read();
      if (current === null) return;
      write({ ...current, accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
    },
    clear: () => {
      storage.removeItem(KEY);
      announce();
    },
    subscribe: (listener) => {
      listeners.add(listener);
      // Entrar o salir en otra pestana tiene que llegar a esta: el evento `storage` solo se
      // dispara en las demas, que es justo lo que hace falta.
      const onStorage = (event: StorageEvent): void => {
        if (event.key === KEY || event.key === null) listener();
      };
      globalThis.addEventListener('storage', onStorage);
      return () => {
        listeners.delete(listener);
        globalThis.removeEventListener('storage', onStorage);
      };
    },
  };
}
