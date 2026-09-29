import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { HealthGateway } from '../../application/ports/health-gateway';
import type { HealthReport } from '../../domain/health-report';
import {
  degradedReport,
  healthyReport,
} from '../../infrastructure/memory/in-memory-health-gateway';

import { HealthGatewayProvider } from './health-gateway-provider';
import { HealthPanel } from './health-panel';

/** Puerta cuyas respuestas se resuelven a mano, para controlar el orden de llegada. */
function deferredGateway(): {
  readReadiness: () => Promise<HealthReport>;
  settle: (index: number, report: HealthReport) => void;
  calls: () => number;
} {
  const pending: ((report: HealthReport) => void)[] = [];
  const readReadiness = vi.fn(
    () =>
      new Promise<HealthReport>((resolve) => {
        pending.push(resolve);
      }),
  );
  return {
    readReadiness,
    settle: (index, report) => {
      pending[index]?.(report);
    },
    calls: () => pending.length,
  };
}

/** El panel con su proveedor. La aplicacion entera ya no sirve: la raiz exige sesion. */
function renderWith(gateway: HealthGateway): void {
  render(
    <HealthGatewayProvider gateway={gateway}>
      <HealthPanel />
    </HealthGatewayProvider>,
  );
}

describe('useHealthReport', () => {
  it('mientras la API no contesta, la pantalla sigue cargando', () => {
    const gateway = deferredGateway();
    renderWith(gateway);

    expect(screen.getByTestId('health-status')).toHaveTextContent('Consultando la API...');
    expect(screen.getByTestId('health-status')).toHaveAttribute('data-tone', 'loading');
  });

  it('descarta la respuesta vieja si se reintenta antes de que llegue', async () => {
    const gateway = deferredGateway();
    renderWith(gateway);
    expect(gateway.calls()).toBe(1);

    await userEvent.click(screen.getByTestId('health-refresh'));
    expect(gateway.calls()).toBe(2);

    // Llega primero el reintento, y solo despues la peticion que quedo colgada.
    gateway.settle(1, healthyReport);
    expect(await screen.findByTestId('health-version')).toHaveTextContent('0.1.0');

    gateway.settle(0, degradedReport);
    await Promise.resolve();

    expect(screen.getByTestId('health-status')).toHaveTextContent('Servicio operativo');
  });
});
