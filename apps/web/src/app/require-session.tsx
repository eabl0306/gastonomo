import type { ReactElement } from 'react';
import { Navigate, useLocation } from 'react-router';

import { useSession } from '../ui/session/use-session';

import { ROUTES, type LoginLocationState } from './routes';

/**
 * Deja pasar solo con sesion.
 *
 * Guarda de donde venia en el estado de la navegacion y no en la direccion: un `?next=` es
 * una puerta abierta a que alguien mande un enlace que, despues de entrar, lleve a donde el
 * quiera.
 */
export function RequireSession({ children }: { children: ReactElement }): ReactElement {
  const session = useSession();
  const location = useLocation();
  if (session !== null) return children;
  const state: LoginLocationState = { from: location.pathname };
  return <Navigate replace state={state} to={ROUTES.login} />;
}
