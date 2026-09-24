import { HealthGatewayError, type HealthGateway } from '../../application/ports/health-gateway';

import { healthReportDtoSchema, toHealthReport } from './health-dto';

export interface HttpHealthGatewayOptions {
  readonly baseUrl: string;
  readonly timeoutMs?: number;
  readonly fetchImpl?: typeof fetch;
}

const READINESS_PATH = '/health/ready';
const DEFAULT_TIMEOUT_MS = 5000;
const READABLE_STATUSES = new Set([200, 503]);

function buildUrl(baseUrl: string): string {
  return `${baseUrl.replace(/\/+$/u, '')}${READINESS_PATH}`;
}

function abortSignal(timeoutMs: number): AbortSignal | undefined {
  return typeof AbortSignal.timeout === 'function' ? AbortSignal.timeout(timeoutMs) : undefined;
}

async function readPayload(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch (error: unknown) {
    throw new HealthGatewayError('La API devolvio algo que no es JSON', { cause: error });
  }
}

async function request(url: string, options: HttpHealthGatewayOptions): Promise<Response> {
  const call = options.fetchImpl ?? globalThis.fetch;
  const signal = abortSignal(options.timeoutMs ?? DEFAULT_TIMEOUT_MS);
  try {
    return await call(url, {
      headers: { accept: 'application/json' },
      ...(signal ? { signal } : {}),
    });
  } catch (error: unknown) {
    throw new HealthGatewayError('No se pudo contactar con la API', { cause: error });
  }
}

/**
 * Adaptador de salida sobre HTTP. Un 503 no es un error de transporte: la API
 * usa ese codigo para decir "estoy degradado", asi que su cuerpo tambien se lee.
 */
export function createHttpHealthGateway(options: HttpHealthGatewayOptions): HealthGateway {
  const url = buildUrl(options.baseUrl);
  return {
    readReadiness: async () => {
      const response = await request(url, options);
      if (!READABLE_STATUSES.has(response.status)) {
        throw new HealthGatewayError(`La API respondio con estado ${String(response.status)}`);
      }
      const parsed = healthReportDtoSchema.safeParse(await readPayload(response));
      if (!parsed.success) {
        throw new HealthGatewayError('La respuesta de salud no cumple el contrato');
      }
      return toHealthReport(parsed.data);
    },
  };
}
