import type { HealthGateway } from '../application/ports/health-gateway';
import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { createHttpHealthGateway } from '../infrastructure/http/http-health-gateway';
import { createHttpPasswordResetGateway } from '../infrastructure/http/http-password-reset-gateway';

/** Lo unico que la web necesita saber del entorno. */
export interface WebEnv {
  readonly VITE_API_URL?: string;
}

/**
 * Vacio significa **el mismo origen**, que es como se sirve desplegado: Caddy
 * pone la API bajo el dominio de la web, asi que una ruta relativa acierta. En
 * desarrollo lo consigue el proxy de Vite. `VITE_API_URL` solo hace falta para
 * apuntar a una API de otro sitio.
 */
const SAME_ORIGIN = '';
const DEFAULT_TIMEOUT_MS = 5000;

export function resolveApiUrl(env: WebEnv): string {
  return env.VITE_API_URL?.trim() ?? SAME_ORIGIN;
}

/**
 * Raiz de composicion: el unico sitio del arbol donde la interfaz y los
 * adaptadores se conocen. Cambiar de adaptador se hace aqui y en ningun otro lado.
 */
export function createHealthGateway(env: WebEnv): HealthGateway {
  return createHttpHealthGateway({
    baseUrl: resolveApiUrl(env),
    timeoutMs: DEFAULT_TIMEOUT_MS,
  });
}

/**
 * El restablecimiento tiene su propio plazo: cambiar la contrasena hashea con
 * Argon2 en el servidor, que tarda mas que responder una consulta.
 */
const RESET_TIMEOUT_MS = 10_000;

export function createPasswordResetGateway(env: WebEnv): PasswordResetGateway {
  return createHttpPasswordResetGateway({
    baseUrl: resolveApiUrl(env),
    timeoutMs: RESET_TIMEOUT_MS,
  });
}
