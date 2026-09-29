/**
 * Puerto de salida del restablecimiento. El nucleo dice que hace falta cambiar
 * la contrasena con un token; no dice que sea HTTP.
 */
export interface PasswordResetGateway {
  resetPassword: (token: string, newPassword: string) => Promise<void>;
}

/**
 * Por que fallo, ya traducido a los casos que la pantalla distingue.
 *
 * - `link-unusable`: el 401 de la API. El enlace caduco, ya se uso, o no es de
 *   aqui. Los tres son el mismo callejon para quien lo lee: pedir otro.
 * - `rejected`: el 422. La contrasena no cumple, y la API dice cual es el motivo.
 * - `unreachable`: no se pudo hablar con la API, o contesto algo ilegible.
 */
export type PasswordResetFailure = 'link-unusable' | 'rejected' | 'unreachable';

export class PasswordResetGatewayError extends Error {
  readonly failure: PasswordResetFailure;

  constructor(failure: PasswordResetFailure, message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'PasswordResetGatewayError';
    this.failure = failure;
  }
}
