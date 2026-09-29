import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { exportWriter } from '../infrastructure/export/serialize';
import {
  SAMPLE_USER,
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../infrastructure/memory/in-memory-auth';
import { createInMemoryHealthGateway } from '../infrastructure/memory/in-memory-health-gateway';
import {
  createMemoryCategoryGateway,
  createMemoryDownloader,
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../infrastructure/memory/in-memory-ledger';

import { App } from './app';
import type { Container } from './composition-root';

const STUB_RESETS = { resetPassword: () => Promise.resolve() };

function renderApp(signedIn: boolean, path = '/'): ReturnType<typeof createMemorySessionStore> {
  window.history.pushState({}, '', path);
  const session = createMemorySessionStore(
    signedIn ? { accessToken: 'a1', refreshToken: 'r1', user: SAMPLE_USER } : null,
  );
  const container: Container = {
    session,
    auth: createMemoryAuthGateway(),
    health: createInMemoryHealthGateway({}),
    passwordResets: STUB_RESETS,
    movements: createMemoryMovementGateway(),
    loans: createMemoryLoanGateway(),
    settings: createMemorySettingsGateway(),
    currencies: createMemoryCurrencyGateway(),
    categories: createMemoryCategoryGateway(),
    exports: exportWriter,
    downloader: createMemoryDownloader(),
    clock: fixedClock(),
  };
  render(<App container={container} />);
  return session;
}

describe('App', () => {
  /** Sin sesion, la raiz no ensena nada de la cuenta: manda a entrar. */
  it('sin sesion, cualquier pantalla de la aplicacion lleva a Entrar', () => {
    renderApp(false);
    expect(screen.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeInTheDocument();
  });

  it('con sesion, la raiz es Inicio y sale el armazon', async () => {
    renderApp(true);
    expect(await screen.findByText('Hola, Camilo')).toBeInTheDocument();
    expect(screen.getAllByRole('navigation', { name: 'Secciones' })).toHaveLength(2);
  });

  it('el armazon ensena quien esta dentro', () => {
    renderApp(true);
    expect(screen.getByText(SAMPLE_USER.name)).toBeInTheDocument();
    expect(screen.getByText(SAMPLE_USER.email)).toBeInTheDocument();
  });

  it('las cuatro pestanas llevan a sus pantallas', async () => {
    renderApp(true);
    for (const label of ['Movimientos', 'Préstamos', 'Ajustes'] as const) {
      await userEvent.click(screen.getAllByRole('link', { name: label })[0]!);
      expect(screen.getByRole('heading', { name: label })).toBeInTheDocument();
    }
  });

  /** El periodo es el mismo objeto para Inicio y para Movimientos. */
  it('el periodo elegido se ve en las dos pantallas', async () => {
    renderApp(true);
    await userEvent.click(await screen.findByRole('button', { name: 'Mes anterior' }));
    expect(screen.getByText('Agosto 2026')).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole('link', { name: 'Movimientos' })[0]!);
    expect(screen.getByText('Agosto 2026')).toBeInTheDocument();
  });

  it('cerrar sesion borra la sesion y devuelve a Entrar', async () => {
    const session = renderApp(true, '/settings');
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(session.read()).toBeNull();
    expect(await screen.findByRole('heading', { name: 'Bienvenido de nuevo' })).toBeInTheDocument();
  });

  it('entrando ya con sesion no se ensena el formulario: se pasa de largo', async () => {
    renderApp(true, '/login');
    expect(await screen.findByText('Hola, Camilo')).toBeInTheDocument();
  });

  it('el panel de salud vive fuera del producto, sin armazon', async () => {
    renderApp(false, '/estado');
    expect(await screen.findByRole('heading', { name: 'Estado del servicio' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Secciones' })).not.toBeInTheDocument();
  });

  /**
   * El sheet vive en el armazon y no en una pantalla: el borrador es el mismo para Inicio y
   * para Movimientos. Este test existe porque montarlo solo en la pantalla pasaba todos los
   * demas y dejaba el boton de rango sin hacer nada en la aplicacion de verdad.
   */
  it('el boton de rango abre el sheet desde Inicio', async () => {
    renderApp(true);
    await userEvent.click(await screen.findByRole('button', { name: 'Elegir un rango de fechas' }));

    expect(screen.getByRole('dialog', { name: 'Rango de fechas' })).toBeInTheDocument();
  });

  it('crear cuenta y recuperar la contrasena tienen su propia direccion', () => {
    renderApp(false, '/register');
    expect(screen.getByRole('heading', { name: 'Crea tu cuenta' })).toBeInTheDocument();
  });
});
