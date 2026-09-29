import {
  PasswordResetGatewayError,
  type PasswordResetGateway,
} from '../../application/ports/password-reset-gateway';

import { readProblemMessage } from './problem-dto';

export interface HttpPasswordResetGatewayOptions {
  readonly baseUrl: string;
  readonly timeoutMs?: number;
  readonly fetchImpl?: typeof fetch;
}

const RESET_PATH = '/v1/auth/password/reset';
const DEFAULT_TIMEOUT_MS = 10_000;

const NO_CONTENT = 204;
const UNAUTHORIZED = 401;
const UNPROCESSABLE = 422;

const LINK_UNUSABLE = 'El enlace ya no sirve.';
const UNREACHABLE = 'No se pudo contactar con la API.';
const REJECTED = 'La API no acepto esa contrasena.';

function buildUrl(baseUrl: string): string {
  return `${baseUrl.replace(/\/+$/u, '')}${RESET_PATH}`;
}

function abortSignal(timeoutMs: number): AbortSignal | undefined {
  return typeof AbortSignal.timeout === 'function' ? AbortSignal.timeout(timeoutMs) : undefined;
}

/** El cuerpo puede no ser JSON; eso no cambia lo que hay que contarle a nadie. */
async function readMessage(response: Response): Promise<string | null> {
  try {
    return readProblemMessage(await response.json());
  } catch {
    return null;
  }
}

/**
 * Adaptador de salida sobre HTTP.
 *
 * Los dos errores que la API distingue son los dos que la pantalla necesita, y
 * por eso no se mezclan: un **401** es el enlace caducado o ya usado, y no hay
 * nada que corregir en el formulario; un **422** es la contrasena, y el mensaje
 * viene en castellano listo para pintar debajo del campo.
 *
 * Va al mismo origen que la web: la API no manda cabeceras CORS a proposito, y
 * en produccion Caddy sirve `/v1/` bajo el dominio de la web.
 */
export function createHttpPasswordResetGateway(
  options: HttpPasswordResetGatewayOptions,
): PasswordResetGateway {
  const url = buildUrl(options.baseUrl);
  return {
    resetPassword: async (token: string, newPassword: string) => {
      const call = options.fetchImpl ?? globalThis.fetch;
      const signal = abortSignal(options.timeoutMs ?? DEFAULT_TIMEOUT_MS);
      let response: Response;
      try {
        response = await call(url, {
          method: 'POST',
          headers: { 'content-type': 'application/json', accept: 'application/problem+json' },
          body: JSON.stringify({ token, newPassword }),
          ...(signal ? { signal } : {}),
        });
      } catch (error: unknown) {
        throw new PasswordResetGatewayError('unreachable', UNREACHABLE, { cause: error });
      }

      if (response.status === NO_CONTENT) return;
      if (response.status === UNAUTHORIZED) {
        throw new PasswordResetGatewayError('link-unusable', LINK_UNUSABLE);
      }
      if (response.status === UNPROCESSABLE) {
        throw new PasswordResetGatewayError('rejected', (await readMessage(response)) ?? REJECTED);
      }
      throw new PasswordResetGatewayError(
        'unreachable',
        (await readMessage(response)) ?? `La API respondio con estado ${String(response.status)}`,
      );
    },
  };
}
