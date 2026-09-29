import type { AppLanguage, UserSettings } from '../../domain/account/settings';
import type { CurrencyCode } from '../../domain/money/currency';

import type { Outcome } from './api-error';

/**
 * El cuerpo de `PATCH /v1/settings`.
 *
 * Los dos campos son opcionales porque la moneda y el idioma se cambian en sitios
 * distintos y cada uno manda solo lo suyo.
 */
export interface SettingsPatch {
  readonly displayCurrency?: CurrencyCode;
  readonly language?: AppLanguage;
}

export interface SettingsGateway {
  read: () => Promise<Outcome<UserSettings>>;
  update: (patch: SettingsPatch) => Promise<Outcome<UserSettings>>;
}
