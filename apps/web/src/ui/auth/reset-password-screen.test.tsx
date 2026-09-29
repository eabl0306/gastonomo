import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import {
  PasswordResetGatewayError,
  type PasswordResetGateway,
} from '../../application/ports/password-reset-gateway';

import { ResetPasswordScreen } from './reset-password-screen';

const GOOD = 'contrasena9';

function gatewayThat(behaviour: () => Promise<void>): PasswordResetGateway {
  return { resetPassword: vi.fn(behaviour) };
}

function renderScreen(gateway: PasswordResetGateway, token = 't0ken') {
  const onFinished = vi.fn();
  render(<ResetPasswordScreen gateway={gateway} onFinished={onFinished} token={token} />);
  return { onFinished };
}

async function fill(newPassword: string, repeated: string): Promise<void> {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Contraseña nueva'), newPassword);
  await user.type(screen.getByLabelText('Repite la contraseña'), repeated);
  await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }));
}

describe('la pantalla de restablecer contrasena', () => {
  it('manda el token del enlace y confirma el cambio', async () => {
    const gateway = gatewayThat(() => Promise.resolve());
    renderScreen(gateway);

    await fill(GOOD, GOOD);

    expect(gateway.resetPassword).toHaveBeenCalledWith('t0ken', GOOD);
    expect(await screen.findByText('Contraseña cambiada')).toBeInTheDocument();
  });

  /**
   * Es el final distinto que justifica separar el 401 del resto: aqui no se pide
   * otra contrasena, se pide otro enlace. El formulario desaparece.
   */
  it('con el enlace caducado deja de pedir la contrasena y manda a pedir otro', async () => {
    const gateway = gatewayThat(() =>
      Promise.reject(new PasswordResetGatewayError('link-unusable', 'caducado')),
    );
    renderScreen(gateway);

    await fill(GOOD, GOOD);

    expect(await screen.findByText('El enlace ya no sirve')).toBeInTheDocument();
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument();
  });

  it('con cualquier otro fallo el formulario se queda, para reintentar', async () => {
    const gateway = gatewayThat(() =>
      Promise.reject(new PasswordResetGatewayError('unreachable', 'No se pudo contactar.')),
    );
    renderScreen(gateway);

    await fill(GOOD, GOOD);

    expect(await screen.findByText('No se pudo contactar.')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña nueva')).toBeInTheDocument();
  });

  it('no gasta el enlace si hay una errata al repetirla', async () => {
    const gateway = gatewayThat(() => Promise.resolve());
    renderScreen(gateway);

    await fill(GOOD, 'otra cosa');

    expect(gateway.resetPassword).not.toHaveBeenCalled();
    expect(await screen.findByText('Las dos contraseñas no son iguales.')).toBeInTheDocument();
  });

  /**
   * Alguien que llega sin token, por copiar la direccion a medias o por entrar a
   * mano. Sin esto veria un formulario que no puede funcionar.
   */
  it('sin token no ensena el formulario', () => {
    renderScreen(
      gatewayThat(() => Promise.resolve()),
      '',
    );

    expect(screen.getByText('Falta el enlace')).toBeInTheDocument();
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument();
  });

  it('el ojo ensena y vuelve a ocultar lo tecleado', async () => {
    const user = userEvent.setup();
    renderScreen(gatewayThat(() => Promise.resolve()));
    const field = screen.getByLabelText('Contraseña nueva');

    expect(field).toHaveAttribute('type', 'password');
    await user.click(screen.getAllByRole('button', { name: 'Mostrar la contraseña' })[0]!);
    expect(field).toHaveAttribute('type', 'text');
  });
});
