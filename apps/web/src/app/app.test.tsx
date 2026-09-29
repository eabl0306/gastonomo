import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import {
  SAMPLE_USER,
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../infrastructure/memory/in-memory-auth';
import { createInMemoryHealthGateway } from '../infrastructure/memory/in-memory-health-gateway';

import { App } from './app';

const STUB_RESETS = { resetPassword: () => Promise.resolve() };

function renderApp(signedIn: boolean, path = '/'): ReturnType<typeof createMemorySessionStore> {
  window.history.pushState({}, '', path);
  const session = createMemorySessionStore(
    signedIn ? { accessToken: 'a1', refreshToken: 'r1', user: SAMPLE_USER } : null,
  );
  render(
    <App
      auth={createMemoryAuthGateway()}
      health={createInMemoryHealthGateway({})}
      passwordResets={STUB_RESETS}
      session={session}
    />,
  );
  return session;
}

describe('App', () => {
  /** Sin sesion, la raiz no ensena nada de la cuenta: manda a entrar. */
  it('sin sesion, cualquier pantalla de la aplicacion lleva a Entrar', () => {
    renderApp(false);
    expect(screen.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeInTheDocument();
  });

  it('con sesion, la raiz es Inicio y sale el armazon', () => {
    renderApp(true);
    expect(screen.getByRole('heading', { name: 'Inicio' })).toBeInTheDocument();
    expect(screen.getAllByRole('navigation', { name: 'Secciones' })).toHaveLength(2);
  });

  it('el armazon ensena quien esta dentro', () => {
    renderApp(true);
    expect(screen.getByText(SAMPLE_USER.name)).toBeInTheDocument();
    expect(screen.getByText(SAMPLE_USER.email)).toBeInTheDocument();
  });

  it('las cuatro pestanas llevan a sus pantallas', async () => {
    renderApp(true);
    for (const [label, heading] of [
      ['Movimientos', 'Movimientos'],
      ['Préstamos', 'Préstamos'],
      ['Ajustes', 'Ajustes'],
    ] as const) {
      await userEvent.click(screen.getAllByRole('link', { name: label })[0]!);
      expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument();
    }
  });

  it('cerrar sesion borra la sesion y devuelve a Entrar', async () => {
    const session = renderApp(true, '/settings');
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(session.read()).toBeNull();
    expect(await screen.findByRole('heading', { name: 'Bienvenido de nuevo' })).toBeInTheDocument();
  });

  it('entrando ya con sesion no se ensena el formulario: se pasa de largo', () => {
    renderApp(true, '/login');
    expect(screen.getByRole('heading', { name: 'Inicio' })).toBeInTheDocument();
  });

  it('el panel de salud vive fuera del producto, sin armazon', async () => {
    renderApp(false, '/estado');
    expect(await screen.findByRole('heading', { name: 'Estado del servicio' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Secciones' })).not.toBeInTheDocument();
  });

  it('crear cuenta y recuperar la contrasena tienen su propia direccion', () => {
    renderApp(false, '/register');
    expect(screen.getByRole('heading', { name: 'Crea tu cuenta' })).toBeInTheDocument();
  });
});
