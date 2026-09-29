import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactElement,
  type ReactNode,
} from 'react';

import type { CurrencyGateway, CurrencyInfo } from '../../application/ports/currency-gateway';
import type { SettingsGateway } from '../../application/ports/settings-gateway';
import { DEFAULT_SETTINGS, type UserSettings } from '../../domain/account/settings';
import { EMPTY_RATES, type ExchangeRates } from '../../domain/money/exchange-rates';
import { FormatsProvider } from '../format/formats-context';

/**
 * Los ajustes de la cuenta, el catalogo de monedas y la tabla de tasas.
 *
 * Se cargan **una vez** por sesion y no en cada pantalla: de ellos salen la moneda con la que
 * se convierte todo, el idioma con el que se formatea y las tasas con las que se convierte,
 * asi que dos pantallas con dos copias distintas ensenarian cifras que no cuadran entre si.
 */
export interface AccountState {
  readonly settings: UserSettings;
  readonly catalog: readonly CurrencyInfo[];
  readonly rates: ExchangeRates;
  /**
   * `GET /v1/exchange-rates` respondio 503 porque la tarea de refresco todavia no consiguio
   * una pasada. No es un fallo de pantalla: lo que esta en la moneda principal se ensena
   * igual y se avisa con una linea.
   */
  readonly ratesUnavailable: boolean;
  /** False hasta la primera respuesta. Las pantallas esperan a que sea true para cargar. */
  readonly ready: boolean;
  /** Lo llama Ajustes al cambiar la moneda o el idioma. */
  readonly apply: (settings: UserSettings) => void;
}

const AccountContext = createContext<AccountState | null>(null);

const EMPTY_CATALOG: readonly CurrencyInfo[] = [];

export function AccountProvider({
  settings: gateway,
  currencies,
  children,
}: {
  settings: SettingsGateway;
  currencies: CurrencyGateway;
  children: ReactNode;
}): ReactElement {
  const [settings, setSettings] = useState<UserSettings>(DEFAULT_SETTINGS);
  const [catalog, setCatalog] = useState<readonly CurrencyInfo[]>(EMPTY_CATALOG);
  const [rates, setRates] = useState<ExchangeRates>(EMPTY_RATES);
  const [ratesUnavailable, setRatesUnavailable] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let live = true;
    void Promise.all([gateway.read(), currencies.catalog(), currencies.rates()]).then(
      ([read, listed, table]) => {
        if (!live) return;
        // Un fallo deja los valores por defecto: peso colombiano y castellano. La pantalla
        // sigue siendo usable y es lo mismo que ve una cuenta recien creada.
        if (read.ok) setSettings(read.value);
        if (listed.ok) setCatalog(listed.value);
        if (table.ok) setRates(table.value);
        setRatesUnavailable(!table.ok);
        setReady(true);
      },
    );
    return () => {
      live = false;
    };
  }, [gateway, currencies]);

  const apply = useCallback((updated: UserSettings) => {
    setSettings(updated);
  }, []);

  const value = useMemo(
    () => ({ settings, catalog, rates, ratesUnavailable, ready, apply }),
    [settings, catalog, rates, ratesUnavailable, ready, apply],
  );
  return (
    <AccountContext.Provider value={value}>
      <FormatsProvider catalog={catalog} settings={settings}>
        {children}
      </FormatsProvider>
    </AccountContext.Provider>
  );
}

export function useAccount(): AccountState {
  const value = useContext(AccountContext);
  if (value === null) throw new Error('useAccount se usa dentro de AccountProvider');
  return value;
}
