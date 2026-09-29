import { useMemo, useState, type ReactElement } from 'react';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import { FREQUENT_CURRENCIES, type CurrencyCode } from '../../domain/money/currency';
import { covers, type ExchangeRates } from '../../domain/money/exchange-rates';
import { Icon } from '../auth/auth-chrome';
import { Sheet } from '../common/sheet';
import { useFormats } from '../format/formats-context';

export interface CurrencySheetProps {
  readonly catalog: readonly CurrencyInfo[];
  readonly rates: ExchangeRates;
  readonly selected: CurrencyCode;
  readonly title: string;
  readonly subtitle: string;
  readonly onPick: (code: CurrencyCode) => void;
  readonly onClose: () => void;
}

/**
 * El selector de moneda.
 *
 * Solo se ofrecen las monedas con tasa, mas la principal: elegir una sin tasa dejaria el
 * registro sin equivalencia y sin forma de calcularla.
 */
export function CurrencySheet(props: CurrencySheetProps): ReactElement {
  const formats = useFormats();
  const [query, setQuery] = useState('');
  const available = useMemo(
    () =>
      props.catalog.filter(
        (currency) =>
          covers(props.rates, currency.code) || currency.code === formats.displayCurrency,
      ),
    [props.catalog, props.rates, formats.displayCurrency],
  );
  const matching = useMemo(() => filterBy(available, query), [available, query]);
  // Frecuentes va en el orden del diseno, no en el del catalogo.
  const frequent = query === '' ? [...matching.filter(isFrequent)].sort(byFrequentOrder) : [];
  const rest = query === '' ? matching.filter((currency) => !isFrequent(currency)) : matching;
  return (
    <Sheet onClose={props.onClose} subtitle={props.subtitle} title={props.title}>
      <div className="g-search">
        <Icon name="search" />
        <input
          aria-label="Buscar moneda"
          onChange={(event) => {
            setQuery(event.target.value);
          }}
          placeholder="Buscar por código o nombre"
          value={query}
        />
      </div>
      <div className="g-currencies">
        {frequent.length === 0 ? null : (
          <>
            <h3 className="g-currencies__group">Frecuentes</h3>
            <CurrencyList currencies={frequent} onPick={props.onPick} selected={props.selected} />
          </>
        )}
        {/* El encabezado de "Todas" solo tiene sentido si debajo queda alguna. */}
        {frequent.length > 0 && rest.length > 0 ? (
          <h3 className="g-currencies__group">Todas</h3>
        ) : null}
        {matching.length === 0 ? (
          <p className="g-card__empty">No encontramos esa moneda.</p>
        ) : (
          <CurrencyList currencies={rest} onPick={props.onPick} selected={props.selected} />
        )}
      </div>
    </Sheet>
  );
}

function isFrequent(currency: CurrencyInfo): boolean {
  return FREQUENT_CURRENCIES.includes(currency.code);
}

function byFrequentOrder(a: CurrencyInfo, b: CurrencyInfo): number {
  return FREQUENT_CURRENCIES.indexOf(a.code) - FREQUENT_CURRENCIES.indexOf(b.code);
}

function filterBy(currencies: readonly CurrencyInfo[], query: string): readonly CurrencyInfo[] {
  const needle = query.trim().toLowerCase();
  if (needle === '') return currencies;
  return currencies.filter(
    (currency) =>
      currency.code.toLowerCase().includes(needle) || currency.name.toLowerCase().includes(needle),
  );
}

function CurrencyList({
  currencies,
  selected,
  onPick,
}: {
  currencies: readonly CurrencyInfo[];
  selected: CurrencyCode;
  onPick: (code: CurrencyCode) => void;
}): ReactElement {
  const formats = useFormats();
  return (
    <ul className="g-currencies__list">
      {currencies.map((currency) => (
        <li key={currency.code}>
          <button
            aria-pressed={currency.code === selected}
            className="g-currency"
            data-active={currency.code === selected}
            onClick={() => {
              onPick(currency.code);
            }}
            type="button"
          >
            <span className="g-currency__code">{currency.code}</span>
            <span className="g-currency__name">{currency.name}</span>
            <span className="g-currency__symbol">{formats.symbolOf(currency.code)}</span>
            {currency.code === selected ? <Icon name="check" /> : null}
          </button>
        </li>
      ))}
    </ul>
  );
}
