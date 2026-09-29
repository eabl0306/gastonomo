import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { App } from '../../app/app';
import type { HealthReport } from '../../domain/health-report';
import {
  degradedReport,
  healthyReport,
} from '../../infrastructure/memory/in-memory-health-gateway';

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

/** La pantalla de restablecer no entra en estos tests; el doble solo cumple el tipo. */
const stubPasswordResets = { resetPassword: () => Promise.resolve() };

describe('useHealthReport', () => {
  it('mientras la API no contesta, la pantalla sigue cargando', () => {
    const gateway = deferredGateway();
    render(<App gateway={gateway} passwordResets={stubPasswordResets} />);

    expect(screen.getByTestId('health-status')).toHaveTextContent('Consultando la API...');
    expect(screen.getByTestId('health-status')).toHaveAttribute('data-tone', 'loading');
  });

  it('descarta la respuesta vieja si se reintenta antes de que llegue', async () => {
    const gateway = deferredGateway();
    render(<App gateway={gateway} passwordResets={stubPasswordResets} />);
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
