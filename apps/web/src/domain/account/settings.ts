import { COP, type CurrencyCode } from '../money/currency';

/** Los dos idiomas de la aplicacion. Se elige en Ajustes, no en el navegador. */
export type AppLanguage = 'es' | 'en';

export const DEFAULT_LANGUAGE: AppLanguage = 'es';

/** La etiqueta de idioma con la que se formatean numeros y fechas. */
export function localeOf(language: AppLanguage): string {
  return language === 'en' ? 'en-US' : 'es-CO';
}

/**
 * Lo que se sincroniza con la cuenta: la moneda principal y el idioma. Nada mas.
 *
 * Ocultar los montos es del dispositivo, no de la cuenta, y por eso no esta aqui.
 */
export interface UserSettings {
  readonly displayCurrency: CurrencyCode;
  readonly language: AppLanguage;
}

export const DEFAULT_SETTINGS: UserSettings = {
  displayCurrency: COP,
  language: DEFAULT_LANGUAGE,
};

/**
 * Una fila del selector de moneda. La API ya filtra el catalogo: solo llegan las monedas
 * con tasa, sin metales ni XDR. El simbolo puede venir vacio.
 */
export interface CurrencyInfo {
  readonly code: CurrencyCode;
  readonly name: string;
  readonly symbol: string;
}
