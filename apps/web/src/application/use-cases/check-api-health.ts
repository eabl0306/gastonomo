import type { HealthReport } from '../../domain/health-report';
import type { HealthGateway } from '../ports/health-gateway';

export type HealthProbe =
  | { readonly outcome: 'reachable'; readonly report: HealthReport }
  | { readonly outcome: 'unreachable'; readonly reason: string };

const UNKNOWN_REASON = 'La API no respondio y no dijo por que';

function describeFailure(error: unknown): string {
  if (error instanceof Error && error.message.trim() !== '') return error.message;
  return UNKNOWN_REASON;
}

/**
 * Caso de uso "comprobar la salud del servicio". Un fallo del adaptador no es
 * una excepcion para quien llama: es un resultado mas, el de API inalcanzable.
 */
export async function checkApiHealth(gateway: HealthGateway): Promise<HealthProbe> {
  try {
    const report = await gateway.readReadiness();
    return { outcome: 'reachable', report };
  } catch (error: unknown) {
    return { outcome: 'unreachable', reason: describeFailure(error) };
  }
}
