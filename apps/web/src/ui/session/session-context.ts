import { createContext, useContext } from 'react';

import type { AuthGateway } from '../../application/ports/auth-gateway';
import type { SessionStore } from '../../application/ports/session-store';

/** Lo que la interfaz necesita para entrar, salir y saber quien esta dentro. */
export interface SessionPorts {
  readonly session: SessionStore;
  readonly auth: AuthGateway;
}

const SessionContext = createContext<SessionPorts | null>(null);

export const SessionProvider = SessionContext.Provider;

/**
 * Los puertos de sesion. Lanza si falta el proveedor, en vez de devolver null: un
 * componente sin proveedor es un error de montaje, y descubrirlo al renderizar es mejor que
 * descubrirlo cuando alguien pulsa Entrar.
 */
export function useSessionPorts(): SessionPorts {
  const ports = useContext(SessionContext);
  if (ports === null) throw new Error('Falta el proveedor de sesion');
  return ports;
}
