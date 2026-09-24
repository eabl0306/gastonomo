import { describe, expect, it } from 'vitest';

import { createHealthCheck, isFailing } from './health-check';

describe('health check', () => {
  it('marca como fallida la comprobacion en error', () => {
    expect(isFailing(createHealthCheck('database', 'error', 10))).toBe(true);
  });

  it('no marca como fallida la comprobacion correcta', () => {
    expect(isFailing(createHealthCheck('database', 'ok', 10))).toBe(false);
  });

  it('redondea la latencia y nunca la deja negativa', () => {
    expect(createHealthCheck('database', 'ok', 12.6).latencyMs).toBe(13);
    expect(createHealthCheck('database', 'ok', -5).latencyMs).toBe(0);
  });
});
