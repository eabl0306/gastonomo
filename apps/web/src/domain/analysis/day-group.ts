import type { Day } from '../calendar/day';
import type { Movement } from '../ledger/movement';
import type { CurrencyCode } from '../money/currency';
import type { ExchangeRates } from '../money/exchange-rates';
import type { Money } from '../money/money';

import { summarizePeriod } from './period-summary';

/** Un grupo de dia de la lista de Movimientos, con su neto a la derecha del encabezado. */
export interface DayGroup {
  readonly date: Day;
  readonly net: Money;
  readonly movements: readonly Movement[];
}

/** Agrupa los movimientos por dia, del mas reciente al mas antiguo. */
export function groupByDay(
  movements: readonly Movement[],
  display: CurrencyCode,
  rates: ExchangeRates,
): readonly DayGroup[] {
  const byDay = new Map<Day, Movement[]>();
  for (const movement of movements) {
    const group = byDay.get(movement.occurredOn);
    if (group === undefined) byDay.set(movement.occurredOn, [movement]);
    else group.push(movement);
  }
  return [...byDay.entries()]
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([date, group]) => ({
      date,
      net: summarizePeriod(group, display, rates).net,
      movements: group,
    }));
}
