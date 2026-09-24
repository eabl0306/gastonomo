import { describe, expect, it } from 'vitest';

import { createHealthCheck } from './health-check';
import {
  createHealthReport,
  deriveStatus,
  failingChecks,
  isDegraded,
  type HealthReport,
} from './health-report';

const okCheck = createHealthCheck('database', 'ok', 3);
const errorCheck = createHealthCheck('database', 'error', 120);

function reportWith(checks: readonly ReturnType<typeof createHealthCheck>[]): HealthReport {
  return createHealthReport({ checks, version: '0.1.0' });
}

describe('deriveStatus', () => {
  it('degrada si alguna comprobacion falla, aunque el servidor diga que esta bien', () => {
    expect(deriveStatus([okCheck, errorCheck], 'ok')).toBe('degraded');
  });

  it('respeta el degradado que declara el propio servidor', () => {
    expect(deriveStatus([okCheck], 'degraded')).toBe('degraded');
  });

  it('esta ok cuando no falla nada y el servidor no dice lo contrario', () => {
    expect(deriveStatus([okCheck], 'ok')).toBe('ok');
    expect(deriveStatus([okCheck])).toBe('ok');
    expect(deriveStatus([])).toBe('ok');
  });
});

describe('createHealthReport', () => {
  it('copia las comprobaciones y calcula el estado', () => {
    const checks = [okCheck];
    const report = reportWith(checks);
    expect(report.status).toBe('ok');
    expect(report.checks).toEqual(checks);
    expect(report.checks).not.toBe(checks);
  });

  it('sustituye la version vacia por una etiqueta legible', () => {
    expect(createHealthReport({ checks: [], version: '   ' }).version).toBe('desconocida');
    expect(createHealthReport({ checks: [], version: '1.2.3' }).version).toBe('1.2.3');
  });
});

describe('failingChecks e isDegraded', () => {
  it('lista solo las comprobaciones en error', () => {
    expect(failingChecks(reportWith([okCheck, errorCheck]))).toEqual([errorCheck]);
    expect(failingChecks(reportWith([okCheck]))).toEqual([]);
  });

  it('dice si el informe esta degradado', () => {
    expect(isDegraded(reportWith([errorCheck]))).toBe(true);
    expect(isDegraded(reportWith([okCheck]))).toBe(false);
  });
});
