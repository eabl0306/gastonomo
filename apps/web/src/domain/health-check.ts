/**
 * Comprobacion individual que la API declara dentro de su informe de salud.
 * Es un modelo puro: aqui no hay HTTP, ni JSON, ni React.
 */
export type CheckStatus = 'ok' | 'error';

export interface HealthCheck {
  readonly name: string;
  readonly status: CheckStatus;
  readonly latencyMs: number;
}

export function isFailing(check: HealthCheck): boolean {
  return check.status === 'error';
}

export function createHealthCheck(
  name: string,
  status: CheckStatus,
  latencyMs: number,
): HealthCheck {
  return { name, status, latencyMs: Math.max(0, Math.round(latencyMs)) };
}
