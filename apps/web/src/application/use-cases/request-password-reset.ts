import type { AuthGateway } from '../ports/auth-gateway';

/**
 * Como acabo la peticion del enlace.
 *
 * `sent` no significa que exista una cuenta con ese correo: la API contesta lo mismo exista
 * o no, porque decir "ese correo no esta registrado" es decirle a cualquiera quien tiene
 * cuenta aqui. La pantalla tiene que ensenar lo mismo en los dos casos.
 */
export type PasswordResetRequestOutcome =
  | { readonly outcome: 'sent' }
  | { readonly outcome: 'too-many-attempts' }
  | { readonly outcome: 'failed' };

export async function requestPasswordReset(
  gateway: AuthGateway,
  email: string,
): Promise<PasswordResetRequestOutcome> {
  const outcome = await gateway.requestPasswordReset(email.trim());
  if (outcome.ok) return { outcome: 'sent' };
  return outcome.error.kind === 'too-many-requests'
    ? { outcome: 'too-many-attempts' }
    : { outcome: 'failed' };
}
