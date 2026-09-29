import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { ApiError } from '../../application/ports/api-error';
import {
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../../infrastructure/memory/in-memory-auth';
import { SessionProvider } from '../session/session-context';

import { ForgotScreen } from './forgot-screen';

function renderForgot(email = '', options: { failWith?: ApiError } = {}): void {
  render(
    <SessionProvider
      value={{ auth: createMemoryAuthGateway(options), session: createMemorySessionStore() }}
    >
      <ForgotScreen email={email} onBackToLogin={() => undefined} />
    </SessionProvider>,
  );
}

describe('ForgotScreen', () => {
  it('llega con el correo que ya estaba escrito en Entrar', () => {
    renderForgot('camilo@correo.com');
    expect(screen.getByLabelText('Correo')).toHaveValue('camilo@correo.com');
  });

  /**
   * La API contesta lo mismo exista la cuenta o no, y la pantalla tambien: decir "ese correo
   * no esta registrado" le diria a cualquiera quien tiene cuenta aqui.
   */
  it('al enviar dice que revises el correo, sin confirmar que la cuenta exista', async () => {
    renderForgot('camilo@correo.com');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar enlace' }));

    expect(await screen.findByRole('heading', { name: 'Revisa tu correo' })).toBeInTheDocument();
    expect(screen.getByText(/tiene una cuenta/u)).toBeInTheDocument();
  });

  it('sin correo no se puede enviar', () => {
    renderForgot('');
    expect(screen.getByRole('button', { name: 'Enviar enlace' })).toBeDisabled();
  });

  it('si no se pudo enviar lo dice y deja reintentar', async () => {
    renderForgot('camilo@correo.com', { failWith: { kind: 'network' } });
    await userEvent.click(screen.getByRole('button', { name: 'Enviar enlace' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa tu conexión');
    expect(screen.getByRole('button', { name: 'Enviar enlace' })).toBeEnabled();
  });
});
