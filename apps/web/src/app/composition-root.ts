import type { HealthGateway } from '../application/ports/health-gateway';
import { createHttpHealthGateway } from '../infrastructure/http/http-health-gateway';

/** Lo unico que la web necesita saber del entorno. */
export interface WebEnv {
  readonly VITE_API_URL?: string;
}

const DEFAULT_API_URL = 'http://localhost:8080';
const DEFAULT_TIMEOUT_MS = 5000;

export function resolveApiUrl(env: WebEnv): string {
  const configured = env.VITE_API_URL?.trim() ?? '';
  return configured === '' ? DEFAULT_API_URL : configured;
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
