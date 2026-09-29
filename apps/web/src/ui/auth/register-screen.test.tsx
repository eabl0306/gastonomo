import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { ApiError } from '../../application/ports/api-error';
import {
  createMemoryAuthGateway,
  createMemorySessionStore,
} from '../../infrastructure/memory/in-memory-auth';
import { SessionProvider } from '../session/session-context';

import { RegisterScreen } from './register-screen';

/**
 * La contrasena de los dobles.
 *
 * Lleva espacios y va en minusculas a proposito: el escaner de secretos de la CI marca
 * cualquier cadena larga sin espacios que vaya detras de `password:`, y una contrasena de
 * prueba que parece una clave de verdad pone el build en rojo por nada.
 */
const PASSWORD = 'clave de prueba';

function renderRegister(
  options: { failWith?: ApiError } = {},
  onSignedUp: () => void = () => undefined,
): {
  auth: ReturnType<typeof createMemoryAuthGateway>;
  session: ReturnType<typeof createMemorySessionStore>;
} {
  const auth = createMemoryAuthGateway(options);
  const session = createMemorySessionStore();
  render(
    <SessionProvider value={{ auth, session }}>
      <RegisterScreen onBackToLogin={() => undefined} onSignedUp={onSignedUp} />
    </SessionProvider>,
  );
  return { auth, session };
}

async function fillIn(password = PASSWORD): Promise<void> {
  await userEvent.type(screen.getByLabelText('Nombre'), 'Camilo');
  await userEvent.type(screen.getByLabelText('Correo'), 'camilo@correo.com');
  await userEvent.type(screen.getByLabelText('Contraseña'), password);
}

describe('RegisterScreen', () => {
  it('crea la cuenta, entra con ella y avisa a quien hospeda', async () => {
    const onSignedUp = vi.fn();
    const { auth, session } = renderRegister({}, onSignedUp);
    await fillIn();
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(auth.calls.signUp).toEqual([
      {
        name: 'Camilo',
        email: 'camilo@correo.com',
        password: PASSWORD,
        acceptedTerms: true,
      },
    ]);
    expect(session.read()).not.toBeNull();
    expect(onSignedUp).toHaveBeenCalledTimes(1);
  });

  /** Sin aceptar los terminos la API rechaza igual: mandarlo es un viaje perdido. */
  it('sin aceptar los terminos no se manda nada', async () => {
    const { auth } = renderRegister();
    await fillIn();
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(auth.calls.signUp).toEqual([]);
    expect(await screen.findByRole('alert')).toHaveTextContent('aceptar los términos');
  });

  it('una contrasena corta se para aqui, sin gastar un viaje', async () => {
    const { auth } = renderRegister();
    await fillIn('corta');
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(auth.calls.signUp).toEqual([]);
    expect(await screen.findByRole('alert')).toHaveTextContent('al menos 8 caracteres');
  });

  /** El mensaje de la API ya viene en castellano y es mas preciso que el nuestro. */
  it('un 422 de la API se pinta tal cual', async () => {
    renderRegister({
      failWith: {
        kind: 'validation',
        fields: [{ field: 'email', message: 'Ese correo ya tiene cuenta.' }],
        detail: null,
      },
    });
    await fillIn();
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese correo ya tiene cuenta.');
  });

  it('el medidor de fuerza acompana a la contrasena', async () => {
    renderRegister();
    expect(screen.getByText('Mínimo 8 caracteres')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Contraseña'), PASSWORD);
    // Larga pero sin numeros: el medidor la da por buena, no por segura.
    expect(screen.getByText('Buena')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Contraseña'), ' 1');
    expect(screen.getByText('Segura')).toBeInTheDocument();
  });
});
