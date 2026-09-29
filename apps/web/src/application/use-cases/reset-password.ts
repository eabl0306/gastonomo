import { checkPassword } from '../../domain/password';
import {
  PasswordResetGatewayError,
  type PasswordResetGateway,
} from '../ports/password-reset-gateway';

/**
 * Como acabo el intento. Un fallo del adaptador no es una excepcion para quien
 * llama: es un resultado mas, igual que en el resto de la web.
 */
export type PasswordResetOutcome =
  | { readonly outcome: 'changed' }
  | { readonly outcome: 'invalid'; readonly problem: 'mismatch' | 'weak' }
  | { readonly outcome: 'link-unusable' }
  | { readonly outcome: 'failed'; readonly reason: string };

const UNKNOWN_REASON = 'No pudimos cambiar la contrasena y la API no dijo por que.';

export interface ResetPasswordCommand {
  readonly token: string;
  readonly newPassword: string;
  readonly repeated: string;
}

function describe(error: unknown): string {
  if (error instanceof Error && error.message.trim() !== '') return error.message;
  return UNKNOWN_REASON;
}

/**
 * Cambia la contrasena con el token del correo.
 *
 * Las dos comprobaciones de aqui son para no gastar el token en balde: el token
 * es de un solo uso, asi que mandar una contrasena que ya sabemos que la API va a
 * rechazar con un 422 no lo quema, pero mandar dos distintas por una errata si
 * seria un viaje perdido. Quien decide de verdad sigue siendo la API.
 */
export async function resetPassword(
  gateway: PasswordResetGateway,
  command: ResetPasswordCommand,
): Promise<PasswordResetOutcome> {
  if (checkPassword(command.newPassword) !== null) {
    return { outcome: 'invalid', problem: 'weak' };
  }
  if (command.newPassword !== command.repeated) {
    return { outcome: 'invalid', problem: 'mismatch' };
  }
  try {
    await gateway.resetPassword(command.token, command.newPassword);
    return { outcome: 'changed' };
  } catch (error: unknown) {
    if (error instanceof PasswordResetGatewayError && error.failure === 'link-unusable') {
      return { outcome: 'link-unusable' };
    }
    return { outcome: 'failed', reason: describe(error) };
  }
}
