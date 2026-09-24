import { isFailing, type HealthCheck } from './health-check';

export type HealthStatus = 'ok' | 'degraded';

export interface HealthReport {
  readonly status: HealthStatus;
  readonly checks: readonly HealthCheck[];
  readonly version: string;
}

export interface HealthReportInput {
  readonly checks: readonly HealthCheck[];
  readonly version: string;
  readonly reportedStatus?: HealthStatus;
}

const UNKNOWN_VERSION = 'desconocida';

/**
 * Regla de degradacion. El servicio esta degradado si falla al menos una
 * comprobacion, o si el propio servicio se declara degradado aunque sus
 * comprobaciones salgan bien. Nunca al reves: un "ok" del servidor no borra
 * una comprobacion en error.
 */
export function deriveStatus(
  checks: readonly HealthCheck[],
  reportedStatus?: HealthStatus,
): HealthStatus {
  if (checks.some(isFailing)) return 'degraded';
  return reportedStatus === 'degraded' ? 'degraded' : 'ok';
}

export function createHealthReport(input: HealthReportInput): HealthReport {
  return {
    status: deriveStatus(input.checks, input.reportedStatus),
    checks: [...input.checks],
    version: input.version.trim() === '' ? UNKNOWN_VERSION : input.version,
  };
}

export function failingChecks(report: HealthReport): readonly HealthCheck[] {
  return report.checks.filter(isFailing);
}

export function isDegraded(report: HealthReport): boolean {
  return report.status === 'degraded';
}
