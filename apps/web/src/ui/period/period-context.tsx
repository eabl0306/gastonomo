import {
  createContext,
  useContext,
  useMemo,
  useReducer,
  type Dispatch,
  type ReactElement,
  type ReactNode,
} from 'react';

import {
  applyRange,
  closeRangeSheet,
  editFrom,
  editTo,
  initialFilter,
  nextMonthOrClearRange,
  openRangeSheet,
  previousMonth,
  selectPreset,
  type PeriodFilterState,
} from '../../application/state/period-filter';
import type { Day } from '../../domain/calendar/day';
import type { PeriodPreset } from '../../domain/calendar/period';
import { useToday } from '../clock/today-context';

/** Lo que se le pide al filtro. Una accion por control del selector y del sheet. */
type FilterAction =
  | { readonly type: 'previous' }
  | { readonly type: 'next-or-clear' }
  | { readonly type: 'open' }
  | { readonly type: 'close' }
  | { readonly type: 'preset'; readonly preset: PeriodPreset }
  | { readonly type: 'from'; readonly day: Day }
  | { readonly type: 'to'; readonly day: Day }
  | { readonly type: 'apply' };

function reduce(today: Day) {
  return (state: PeriodFilterState, action: FilterAction): PeriodFilterState => {
    switch (action.type) {
      case 'previous':
        return previousMonth(state);
      case 'next-or-clear':
        return nextMonthOrClearRange(state, today);
      case 'open':
        return openRangeSheet(state);
      case 'close':
        return closeRangeSheet(state);
      case 'preset':
        return selectPreset(state, action.preset, today);
      case 'from':
        return editFrom(state, action.day);
      case 'to':
        return editTo(state, action.day);
      case 'apply':
        return applyRange(state);
    }
  };
}

interface PeriodContextValue {
  readonly state: PeriodFilterState;
  readonly today: Day;
  readonly dispatch: Dispatch<FilterAction>;
}

const PeriodContext = createContext<PeriodContextValue | null>(null);

/**
 * El filtro de periodo, montado **por encima** de Inicio y de Movimientos.
 *
 * Es el mismo objeto para las dos pantallas porque el diseno dice que el periodo elegido se
 * ve en las dos. Si viviera dentro de una, cambiar de pestana lo perderia.
 */
export function PeriodProvider({ children }: { children: ReactNode }): ReactElement {
  const today = useToday();
  const [state, dispatch] = useReducer(reduce(today), today, initialFilter);
  const value = useMemo(() => ({ state, today, dispatch }), [state, today]);
  return <PeriodContext.Provider value={value}>{children}</PeriodContext.Provider>;
}

export function usePeriod(): PeriodContextValue {
  const value = useContext(PeriodContext);
  if (value === null) throw new Error('usePeriod se usa dentro de PeriodProvider');
  return value;
}
