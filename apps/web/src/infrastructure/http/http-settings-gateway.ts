import { success, type Outcome } from '../../application/ports/api-error';
import type { SettingsGateway, SettingsPatch } from '../../application/ports/settings-gateway';
import type { UserSettings } from '../../domain/account/settings';

import type { ApiClient } from './api-client';
import { readSettings } from './catalog-dto';
import { malformed } from './malformed';

function settingsFrom(body: unknown): Outcome<UserSettings> {
  const settings = readSettings(body);
  return settings === null ? malformed<UserSettings>() : success(settings);
}

/** Ajustes de la cuenta contra la API. */
export function createHttpSettingsGateway(client: ApiClient): SettingsGateway {
  return {
    read: async () => {
      const outcome = await client.request('settings');
      return outcome.ok ? settingsFrom(outcome.value.body) : outcome;
    },

    update: async (patch: SettingsPatch) => {
      const outcome = await client.request('settings', { method: 'PATCH', body: patch });
      return outcome.ok ? settingsFrom(outcome.value.body) : outcome;
    },
  };
}
