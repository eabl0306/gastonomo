import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { SettingsPatch } from '../../application/ports/settings-gateway';
import {
  createMemoryAuthGateway,
  createMemorySessionStore,
  SAMPLE_USER,
} from '../../infrastructure/memory/in-memory-auth';
import {
  createMemoryCurrencyGateway,
  createMemorySettingsGateway,
} from '../../infrastructure/memory/in-memory-ledger';
import { AccountProvider } from '../account/account-context';
import { SessionProvider } from '../session/session-context';

import { SettingsScreen, type SettingsActions } from './settings-screen';

const NOOP: SettingsActions = {
  onOpenCategories: () => undefined,
  onOpenExport: () => undefined,
  onSignedOut: () => undefined,
};

function renderSettings(actions: SettingsActions = NOOP, patches: SettingsPatch[] = []): void {
  const currencies = createMemoryCurrencyGateway();
  const memory = createMemorySettingsGateway();
  const settings = {
    read: memory.read,
    update: (patch: SettingsPatch) => {
      patches.push(patch);
      return memory.update(patch);
    },
  };
  const session = createMemorySessionStore({
    accessToken: 'a1',
    refreshToken: 'r1',
    user: SAMPLE_USER,
  });
  const auth = createMemoryAuthGateway();
  render(
    <SessionProvider value={{ session, auth }}>
      <AccountProvider currencies={currencies} settings={settings}>
        <SettingsScreen actions={actions} ports={{ settings, currencies, auth }} />
      </AccountProvider>
    </SessionProvider>,
  );
}

describe('Ajustes', () => {
  it('ensena quien esta dentro', () => {
    renderSettings();
    expect(screen.getByText(SAMPLE_USER.name)).toBeInTheDocument();
    expect(screen.getByText(SAMPLE_USER.email)).toBeInTheDocument();
  });

  it('la fila de tasas dice de cuando son', async () => {
    renderSettings();
    expect(await screen.findByText('22 sept')).toBeInTheDocument();
  });

  /**
   * La moneda se aplica en local antes de que conteste la API: la pantalla se repinta entera y
   * esperar deja las cifras en la moneda vieja durante medio segundo.
   */
  it('cambiar la moneda se ve al momento y se manda a la API', async () => {
    const patches: SettingsPatch[] = [];
    renderSettings(NOOP, patches);
    await screen.findByText('22 sept');

    await userEvent.click(screen.getByRole('button', { name: /Moneda/u }));
    await userEvent.click(await screen.findByRole('button', { name: /US Dollar/u }));

    expect(screen.getByRole('button', { name: /Moneda/u })).toHaveTextContent('USD');
    expect(patches).toEqual([{ displayCurrency: 'USD' }]);
  });

  it('los dos atajos llevan a sus pantallas', async () => {
    const actions = {
      onOpenCategories: vi.fn(),
      onOpenExport: vi.fn(),
      onSignedOut: vi.fn(),
    };
    renderSettings(actions);

    await userEvent.click(screen.getByRole('button', { name: /Categorías/u }));
    expect(actions.onOpenCategories).toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /Exportar datos/u }));
    expect(actions.onOpenExport).toHaveBeenCalled();
  });

  it('cambiar la contrasena pide la actual y la nueva', async () => {
    renderSettings();
    await userEvent.click(screen.getByRole('button', { name: /Cambiar contraseña/u }));

    const dialog = within(screen.getByRole('dialog', { name: 'Cambiar contraseña' }));
    const save = dialog.getByRole('button', { name: 'Cambiar contraseña' });
    expect(save).toBeDisabled();

    await userEvent.type(dialog.getByLabelText('Contraseña actual'), 'la de antes');
    await userEvent.type(dialog.getByLabelText('Contraseña nueva'), 'corta');
    expect(save).toBeDisabled();

    await userEvent.type(dialog.getByLabelText('Contraseña nueva'), ' pero larga');
    expect(save).toBeEnabled();
  });

  it('cerrar sesion borra la sesion y avisa', async () => {
    const actions = { ...NOOP, onSignedOut: vi.fn() };
    renderSettings(actions);

    await userEvent.click(screen.getByRole('button', { name: /Cerrar sesión/u }));
    expect(actions.onSignedOut).toHaveBeenCalled();
  });
});
