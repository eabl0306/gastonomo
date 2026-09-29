import { checkPassword } from '../../domain/password';
import { messageFor } from '../ports/api-error';
import type { AuthGateway } from '../ports/auth-gateway';
import type { SessionStore } from '../ports/session-store';

/** Como acabo un intento de crear la cuenta. */
export type SignUpOutcome =
  | { readonly outcome: 'signed-up' }
  /** La contrasena no llega al minimo. Se comprueba aqui para no gastar un viaje. */
  | { readonly outcome: 'weak-password' }
  | { readonly outcome: 'terms-not-accepted' }
  /** Un 409 o un 422 de la API, con su mensaje por campo si lo trae. */
  | {
      readonly outcome: 'rejected';
      readonly emailMessage: string | null;
      readonly detail: string | null;
    }
  | { readonly outcome: 'too-many-attempts' }
  | { readonly outcome: 'failed' };

export interface SignUpCommand {
  readonly name: string;
  readonly email: string;
  readonly password: string;
  readonly acceptedTerms: boolean;
}

/**
 * Crea la cuenta y **entra con ella**: la API devuelve una sesion en el mismo 201, asi que
 * pedir la contrasena otra vez seria pedirla dos veces seguidas por gusto.
 *
 * Las dos comprobaciones de aqui no sustituyen a la API: son para no mandar algo que ya
 * sabemos que va a volver rechazado.
 */
export async function signUp(
  gateway: AuthGateway,
  session: SessionStore,
  command: SignUpCommand,
): Promise<SignUpOutcome> {
  if (!command.acceptedTerms) return { outcome: 'terms-not-accepted' };
  if (checkPassword(command.password) !== null) return { outcome: 'weak-password' };
  const outcome = await gateway.signUp({
    name: command.name.trim(),
    email: command.email.trim(),
    password: command.password,
    acceptedTerms: true,
  });
  if (outcome.ok) {
    session.save(outcome.value);
    return { outcome: 'signed-up' };
  }
  const error = outcome.error;
  if (error.kind === 'validation') {
    return { outcome: 'rejected', emailMessage: messageFor(error, 'email'), detail: error.detail };
  }
  if (error.kind === 'conflict') {
    return { outcome: 'rejected', emailMessage: null, detail: error.detail };
  }
  if (error.kind === 'too-many-requests') return { outcome: 'too-many-attempts' };
  return { outcome: 'failed' };
}
