import { useCallback, useMemo, useState } from 'react';

import {
  emptyMoneyField,
  viewOfMoneyField,
  type MoneyFieldState,
  type MoneyFieldView,
} from '../../application/state/money-field';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { parseAmount } from '../format/amount-input';

import type { MoneyPrefill } from './prefill';

export interface MoneyField {
  readonly state: MoneyFieldState;
  readonly view: MoneyFieldView;
  readonly setAmount: (raw: string) => void;
  /** Null vuelve a la equivalencia sugerida por la tasa. */
  readonly setConversion: (raw: string | null) => void;
  readonly setCurrency: (code: CurrencyCode) => void;
}

/**
 * El campo de importe de un formulario.
 *
 * Cambiar de moneda **tira la equivalencia tecleada**: la que habia era para la moneda
 * anterior, y dejarla guardaria una equivalencia que no se corresponde con nada.
 *
 * Con [prefill] arranca relleno, que es lo que hace "Editar" en el dictado. La equivalencia
 * arranca siempre en null: la que corresponda la calcula la tasa de hoy.
 */
export function useMoneyField(
  display: CurrencyCode,
  rates: ExchangeRates,
  prefill?: MoneyPrefill,
): MoneyField {
  const [state, setState] = useState<MoneyFieldState>(() =>
    prefill === undefined
      ? emptyMoneyField(display)
      : { amount: prefill.amount, currency: prefill.currency, conversion: null },
  );
  const view = useMemo(
    () =>
      viewOfMoneyField({
        state,
        amount: parseAmount(state.amount),
        conversion: state.conversion === null ? null : parseAmount(state.conversion),
        display,
        rates,
      }),
    [state, display, rates],
  );
  const setAmount = useCallback((amount: string) => {
    setState((previous) => ({ ...previous, amount }));
  }, []);
  const setConversion = useCallback((conversion: string | null) => {
    setState((previous) => ({ ...previous, conversion }));
  }, []);
  const setCurrency = useCallback((currency: CurrencyCode) => {
    setState((previous) => ({ ...previous, currency, conversion: null }));
  }, []);
  return { state, view, setAmount, setConversion, setCurrency };
}
