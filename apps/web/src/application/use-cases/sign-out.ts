import type { AuthGateway } from '../ports/auth-gateway';
import type { SessionStore } from '../ports/session-store';

/**
 * Cierra la sesion.
 *
 * **La sesion local se borra pase lo que pase.** Revocar el refresh token en la API es lo
 * correcto, pero si esa llamada falla, dejar la sesion puesta significaria que el usuario
 * pulso "Cerrar sesion" y se quedo dentro. El token acaba caducando solo.
 */
export async function signOut(gateway: AuthGateway, session: SessionStore): Promise<void> {
  const stored = session.read();
  session.clear();
  if (stored === null) return;
  await gateway.signOut(stored.refreshToken);
}
