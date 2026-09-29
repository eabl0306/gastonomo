import type { AuthGateway } from '../ports/auth-gateway';
import type { SessionStore } from '../ports/session-store';

/**
 * Como acabo un intento de entrar.
 *
 * `invalid-credentials` sale de un 401 y no distingue si el que fallo fue el correo o la
 * contrasena: la API tampoco lo dice, y decirlo seria confirmar que ese correo tiene cuenta.
 */
export type SignInOutcome =
  | { readonly outcome: 'signed-in' }
  | { readonly outcome: 'invalid-credentials' }
  | { readonly outcome: 'too-many-attempts'; readonly retryAfterSeconds: number | null }
  | { readonly outcome: 'failed' };

/**
 * Entra con correo y contrasena, y **guarda la sesion** antes de decir que si.
 *
 * Guardarla aqui y no en la pantalla es lo que hace que el armazon ya se encuentre la sesion
 * puesta cuando reacciona: si lo hiciera la pantalla, habria un instante con sesion en la
 * API y sin sesion en la aplicacion.
 */
export async function signIn(
  gateway: AuthGateway,
  session: SessionStore,
  email: string,
  password: string,
): Promise<SignInOutcome> {
  const outcome = await gateway.signIn(email.trim(), password);
  if (outcome.ok) {
    session.save(outcome.value);
    return { outcome: 'signed-in' };
  }
  const error = outcome.error;
  if (error.kind === 'invalid-credentials' || error.kind === 'unauthorized') {
    return { outcome: 'invalid-credentials' };
  }
  if (error.kind === 'too-many-requests') {
    return { outcome: 'too-many-attempts', retryAfterSeconds: error.retryAfterSeconds };
  }
  return { outcome: 'failed' };
}
