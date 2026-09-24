import { z } from 'zod';

import { createHealthCheck, type HealthCheck } from '../../domain/health-check';
import { createHealthReport, type HealthReport } from '../../domain/health-report';

/**
 * Contrato de la API tal y como viaja por el cable. Si la API cambia el JSON,
 * el fallo salta aqui y no a mitad de la interfaz.
 */
const healthCheckDtoSchema = z.object({
  name: z.string().min(1),
  status: z.enum(['ok', 'error']),
  latencyMs: z.number().nonnegative(),
});

export const healthReportDtoSchema = z.object({
  status: z.enum(['ok', 'degraded']),
  checks: z.array(healthCheckDtoSchema),
  version: z.string(),
});

export type HealthReportDto = z.infer<typeof healthReportDtoSchema>;

function toHealthCheck(dto: z.infer<typeof healthCheckDtoSchema>): HealthCheck {
  return createHealthCheck(dto.name, dto.status, dto.latencyMs);
}

export function toHealthReport(dto: HealthReportDto): HealthReport {
  return createHealthReport({
    checks: dto.checks.map(toHealthCheck),
    version: dto.version,
    reportedStatus: dto.status,
  });
}
