import { describe, expect, it } from 'vitest';

import { createHealthCheck } from '../../domain/health-check';
import { createHealthReport } from '../../domain/health-report';
import { HealthGatewayError, type HealthGateway } from '../ports/health-gateway';

import { checkApiHealth } from './check-api-health';

const report = createHealthReport({
  checks: [createHealthCheck('database', 'ok', 2)],
  version: '0.1.0',
});

function gatewayReturning(value: Promise<never> | Promise<typeof report>): HealthGateway {
  return { readReadiness: () => value };
}

describe('checkApiHealth', () => {
  it('devuelve el informe cuando la API responde', async () => {
    const probe = await checkApiHealth(gatewayReturning(Promise.resolve(report)));
    expect(probe).toEqual({ outcome: 'reachable', report });
  });

  it('convierte el fallo del adaptador en un resultado, no en una excepcion', async () => {
    const gateway = gatewayReturning(Promise.reject(new HealthGatewayError('sin ruta al host')));
    const probe = await checkApiHealth(gateway);
    expect(probe).toEqual({ outcome: 'unreachable', reason: 'sin ruta al host' });
  });

  it('explica el fallo aunque el adaptador lance algo que no es un Error', async () => {
    const gateway: HealthGateway = {
      // eslint-disable-next-line @typescript-eslint/prefer-promise-reject-errors -- justo el caso raro que probamos
      readReadiness: () => Promise.reject('vaya'),
    };
    const probe = await checkApiHealth(gateway);
    expect(probe).toEqual({
      outcome: 'unreachable',
      reason: 'La API no respondio y no dijo por que',
    });
  });
});
