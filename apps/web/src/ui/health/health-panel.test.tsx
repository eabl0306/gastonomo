import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { HealthGateway } from '../../application/ports/health-gateway';
import {
  createInMemoryHealthGateway,
  degradedReport,
  healthyReport,
} from '../../infrastructure/memory/in-memory-health-gateway';

import { HealthGatewayProvider } from './health-gateway-provider';
import { HealthPanel } from './health-panel';

/**
 * El panel se monta con su proveedor, no a traves de `App`.
 *
 * Antes se renderizaba la aplicacion entera porque el panel ocupaba la ruta raiz. Ya no: la
 * raiz es Inicio y exige sesion, asi que montar `App` aqui probaria el guardia en vez del
 * panel.
 */
function renderWith(gateway: HealthGateway): void {
  render(
    <HealthGatewayProvider gateway={gateway}>
      <HealthPanel />
    </HealthGatewayProvider>,
  );
}

describe('HealthPanel', () => {
  it('muestra el servicio operativo con sus comprobaciones', async () => {
    renderWith(createInMemoryHealthGateway({ report: healthyReport }));

    expect(await screen.findByTestId('health-status')).toHaveTextContent('Servicio operativo');
    expect(screen.getByTestId('health-version')).toHaveTextContent('0.1.0');
    expect(screen.getAllByTestId('health-check')).toHaveLength(1);
  });

  it('muestra el servicio degradado cuando una comprobacion falla', async () => {
    renderWith(createInMemoryHealthGateway({ report: degradedReport }));

    const status = await screen.findByTestId('health-status');
    expect(status).toHaveTextContent('Servicio degradado');
    expect(status).toHaveAttribute('data-tone', 'degraded');
  });

  it('muestra el motivo cuando la API no responde', async () => {
    renderWith(createInMemoryHealthGateway({ failWith: 'conexion rechazada' }));

    expect(await screen.findByTestId('health-status')).toHaveTextContent('API inalcanzable');
    expect(screen.getByTestId('health-reason')).toHaveTextContent('conexion rechazada');
  });

  it('vuelve a preguntar a la API al pulsar reintentar', async () => {
    const readReadiness = vi.fn(() => Promise.resolve(healthyReport));
    renderWith({ readReadiness });

    await screen.findByTestId('health-version');
    const callsBefore = readReadiness.mock.calls.length;
    await userEvent.click(screen.getByTestId('health-refresh'));
    await screen.findByTestId('health-version');

    expect(readReadiness.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('avisa si alguien usa el panel sin inyectar el adaptador', () => {
    // React escribe el error en consola antes de propagarlo: lo silenciamos aqui.
    const logged = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => {
      render(<HealthPanel />);
    }).toThrow('Falta HealthGatewayProvider');
    logged.mockRestore();
  });
});
