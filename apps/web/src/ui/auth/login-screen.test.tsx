import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';

import type { ApiError } from '../../application/ports/api-error';
import {
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../../infrastructure/memory/in-memory-auth';
import { SessionProvider } from '../session/session-context';

import { LoginScreen } from './login-screen';

/**
 * La contrasena de los dobles.
 *
 * Lleva espacios y va en minusculas a proposito: el escaner de secretos de la CI marca
 * cualquier cadena larga sin espacios que vaya detras de `password:`, y una contrasena de
 * prueba que parece una clave de verdad pone el build en rojo por nada.
 */
const PASSWORD = 'clave de prueba';

interface Harness {
  readonly onSignedIn: () => void;
  readonly onForgotPassword: (email: string) => void;
  readonly onRegister: () => void;
}

function renderLogin(
  options: { failWith?: ApiError } = {},
  handlers: Partial<Harness> = {},
): {
  auth: ReturnType<typeof createMemoryAuthGateway>;
  session: ReturnType<typeof createMemorySessionStore>;
} {
  const auth = createMemoryAuthGateway(options);
  const session = createMemorySessionStore();
  const screenElement: ReactElement = (
    <SessionProvider value={{ auth, session }}>
      <LoginScreen
        onForgotPassword={handlers.onForgotPassword ?? (() => undefined)}
        onRegister={handlers.onRegister ?? (() => undefined)}
        onSignedIn={handlers.onSignedIn ?? (() => undefined)}
      />
    </SessionProvider>
  );
  render(screenElement);
  return { auth, session };
}

async function fillIn(email: string, password: string): Promise<void> {
  await userEvent.type(screen.getByLabelText('Correo'), email);
  await userEvent.type(screen.getByLabelText('Contraseña'), password);
}

describe('LoginScreen', () => {
  it('pinta el formulario del diseno', () => {
    renderLogin();
    expect(screen.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeInTheDocument();
    expect(screen.getByLabelText('Correo')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '¿Olvidaste tu contraseña?' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Regístrate' })).toBeInTheDocument();
  });

  /** Mandar un formulario vacio solo sirve para gastar un viaje y recibir el mismo error. */
  it('el boton esta apagado hasta que hay correo y contrasena', async () => {
    renderLogin();
    const submit = screen.getByRole('button', { name: 'Entrar' });
    expect(submit).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Correo'), 'camilo@correo.com');
    expect(submit).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Contraseña'), PASSWORD);
    expect(submit).toBeEnabled();
  });

  it('al entrar guarda la sesion y avisa a quien hospeda', async () => {
    const onSignedIn = vi.fn();
    const { auth, session } = renderLogin({}, { onSignedIn });
    await fillIn('camilo@correo.com', PASSWORD);
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(auth.calls.signIn).toEqual([['camilo@correo.com', PASSWORD]]);
    expect(session.read()?.user.email).toBe('camilo@correo.com');
    expect(onSignedIn).toHaveBeenCalledTimes(1);
  });

  /** La API no dice si fallo el correo o la contrasena, y decirlo confirmaria la cuenta. */
  it('un 401 se cuenta sin decir cual de los dos campos fallo', async () => {
    renderLogin({ failWith: { kind: 'invalid-credentials' } });
    await fillIn('camilo@correo.com', 'mal');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.');
    expect(screen.getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'true');
  });

  it('demasiados intentos se cuenta aparte', async () => {
    renderLogin({ failWith: { kind: 'too-many-requests', retryAfterSeconds: 60 } });
    await fillIn('camilo@correo.com', PASSWORD);
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Demasiados intentos');
  });

  it('sin conexion lo dice, en vez de dejar el boton girando', async () => {
    renderLogin({ failWith: { kind: 'network' } });
    await fillIn('camilo@correo.com', PASSWORD);
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa tu conexión');
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
  });

  it('recuperar la contrasena se lleva el correo ya escrito', async () => {
    const onForgotPassword = vi.fn();
    renderLogin({}, { onForgotPassword });
    await userEvent.type(screen.getByLabelText('Correo'), 'camilo@correo.com');
    await userEvent.click(screen.getByRole('button', { name: '¿Olvidaste tu contraseña?' }));

    expect(onForgotPassword).toHaveBeenCalledWith('camilo@correo.com');
  });

  it('el ojo ensena y vuelve a ocultar la contrasena', async () => {
    renderLogin();
    const field = screen.getByLabelText('Contraseña');
    expect(field).toHaveAttribute('type', 'password');
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar la contraseña' }));
    expect(field).toHaveAttribute('type', 'text');
    await userEvent.click(screen.getByRole('button', { name: 'Ocultar la contraseña' }));
    expect(field).toHaveAttribute('type', 'password');
  });
});
