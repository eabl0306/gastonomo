import { HealthGatewayError, type HealthGateway } from '../../application/ports/health-gateway';
import { createHealthCheck } from '../../domain/health-check';
import { createHealthReport, type HealthReport } from '../../domain/health-report';

export interface InMemoryHealthGatewayOptions {
  readonly report?: HealthReport;
  readonly failWith?: string;
}

export const healthyReport: HealthReport = createHealthReport({
  checks: [createHealthCheck('database', 'ok', 3)],
  version: '0.1.0',
  reportedStatus: 'ok',
});

export const degradedReport: HealthReport = createHealthReport({
  checks: [createHealthCheck('database', 'error', 120)],
  version: '0.1.0',
  reportedStatus: 'degraded',
});

/**
 * Adaptador falso. Sirve para los tests y para arrancar la interfaz sin API,
 * y demuestra que la interfaz solo depende del puerto.
 */
export function createInMemoryHealthGateway(
  options: InMemoryHealthGatewayOptions = {},
): HealthGateway {
  return {
    readReadiness: () => {
      if (options.failWith !== undefined) {
        return Promise.reject(new HealthGatewayError(options.failWith));
      }
      return Promise.resolve(options.report ?? healthyReport);
    },
  };
}
