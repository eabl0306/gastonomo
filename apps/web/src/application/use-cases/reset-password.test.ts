import { describe, expect, it, vi } from 'vitest';

import {
  PasswordResetGatewayError,
  type PasswordResetGateway,
} from '../ports/password-reset-gateway';

import { resetPassword } from './reset-password';

const GOOD = 'contrasena9';

function gatewayThat(behaviour: () => Promise<void>): PasswordResetGateway {
  return { resetPassword: vi.fn(behaviour) };
}

describe('restablecer la contrasena', () => {
  it('manda el token y la contrasena nueva cuando todo cuadra', async () => {
    const gateway = gatewayThat(() => Promise.resolve());

    const result = await resetPassword(gateway, {
      token: 'abc',
      newPassword: GOOD,
      repeated: GOOD,
    });

    expect(result).toEqual({ outcome: 'changed' });
    expect(gateway.resetPassword).toHaveBeenCalledWith('abc', GOOD);
  });

  it('no gasta el viaje si las dos contrasenas no coinciden', async () => {
    const gateway = gatewayThat(() => Promise.resolve());

    const result = await resetPassword(gateway, {
      token: 'abc',
      newPassword: GOOD,
      repeated: 'otra cosa',
    });

    expect(result).toEqual({ outcome: 'invalid', problem: 'mismatch' });
    expect(gateway.resetPassword).not.toHaveBeenCalled();
  });

  it('tampoco si es demasiado corta', async () => {
    const gateway = gatewayThat(() => Promise.resolve());

    const result = await resetPassword(gateway, { token: 'abc', newPassword: 'x', repeated: 'x' });

    expect(result).toEqual({ outcome: 'invalid', problem: 'weak' });
    expect(gateway.resetPassword).not.toHaveBeenCalled();
  });

  /**
   * El enlace caducado tiene su propia salida porque la pantalla hace otra cosa:
   * no pide otra contrasena, manda a pedir otro enlace.
   */
  it('separa el enlace inservible de cualquier otro fallo', async () => {
    const expired = gatewayThat(() =>
      Promise.reject(new PasswordResetGatewayError('link-unusable', 'caducado')),
    );
    const broken = gatewayThat(() =>
      Promise.reject(new PasswordResetGatewayError('unreachable', 'sin red')),
    );

    await expect(
      resetPassword(expired, { token: 'abc', newPassword: GOOD, repeated: GOOD }),
    ).resolves.toEqual({ outcome: 'link-unusable' });
    await expect(
      resetPassword(broken, { token: 'abc', newPassword: GOOD, repeated: GOOD }),
    ).resolves.toEqual({ outcome: 'failed', reason: 'sin red' });
  });
});
