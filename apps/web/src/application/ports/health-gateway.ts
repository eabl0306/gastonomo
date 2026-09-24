import type { HealthReport } from '../../domain/health-report';

/**
 * Puerto de salida. El nucleo declara que necesita saber si la API responde,
 * sin decir como se averigua. Los adaptadores viven en infrastructure.
 */
export interface HealthGateway {
  readReadiness: () => Promise<HealthReport>;
}

/** Fallo de un adaptador al hablar con el mundo exterior. */
export class HealthGatewayError extends Error {
  constructor(message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'HealthGatewayError';
  }
}
