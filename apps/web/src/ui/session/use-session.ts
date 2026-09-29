import { useSyncExternalStore } from 'react';

import type { StoredSession } from '../../application/ports/session-store';

import { useSessionPorts } from './session-context';

/**
 * La sesion guardada, y se vuelve a pintar cuando cambia.
 *
 * Va por `useSyncExternalStore` y no por un estado propio porque la sesion vive fuera de
 * React: la puede cambiar el cliente al refrescar, o **otra pestana**. Con un `useState` y
 * un efecto, cerrar sesion en una pestana dejaba la otra dentro hasta recargar.
 */
export function useSession(): StoredSession | null {
  const { session } = useSessionPorts();
  return useSyncExternalStore(session.subscribe, session.read, session.read);
}
