import { createContext, useContext, useMemo, type ReactElement, type ReactNode } from 'react';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import { DEFAULT_SETTINGS, type UserSettings } from '../../domain/account/settings';

import { FALLBACK_SYMBOLS, withCatalog } from './currency-symbols';
import { gastonomoFormats, type GastonomoFormats } from './formats';

const FormatsContext = createContext<GastonomoFormats>(gastonomoFormats(DEFAULT_SETTINGS));

/**
 * Sirve el formato a toda la aplicacion.
 *
 * Se memoiza por idioma, moneda y catalogo: sin eso, cada render construiria trece
 * `Intl.NumberFormat` nuevos y tiraria su cache justo en la pantalla que mas importes pinta.
 */
export function FormatsProvider({
  settings,
  catalog,
  children,
}: {
  settings: UserSettings;
  catalog: readonly CurrencyInfo[];
  children: ReactNode;
}): ReactElement {
  const formats = useMemo(
    () => gastonomoFormats(settings, withCatalog(FALLBACK_SYMBOLS, catalog)),
    [settings, catalog],
  );
  return <FormatsContext.Provider value={formats}>{children}</FormatsContext.Provider>;
}

export function useFormats(): GastonomoFormats {
  return useContext(FormatsContext);
}
