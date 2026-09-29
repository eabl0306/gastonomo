import { addDays, isAfter, isBefore, type Day } from '../calendar/day';
import { periodFrom, periodTo, type Period } from '../calendar/period';
import { isIncome } from '../ledger/category';
import type { Movement } from '../ledger/movement';
import type { CurrencyCode } from '../money/currency';
import {
  ZERO,
  addDecimals,
  compareDecimals,
  subtractDecimals,
  type Decimal,
} from '../money/decimal';
import { displayedAmount } from '../money/displayed-amount';
import type { ExchangeRates } from '../money/exchange-rates';

/** Un punto de la linea de tendencia: un dia y el balance acumulado hasta el. */
interface TrendPoint {
  readonly date: Day;
  readonly cumulative: Decimal;
}

/**
 * La tendencia del balance de la tarjeta de Inicio: el balance acumulado dia a dia del
 * periodo, hasta hoy.
 *
 * Acumula **desde cero** el primer dia del periodo y no arrastra saldo anterior, que es lo
 * que hace el prototipo y por lo que no hace falta un endpoint de saldo previo.
 */
export interface BalanceTrend {
  readonly points: readonly TrendPoint[];
}

export function isEmptyTrend(trend: BalanceTrend): boolean {
  return trend.points.length === 0;
}

/** El minimo acumulado, para escalar el eje. */
export function trendMinimum(trend: BalanceTrend): Decimal {
  return trend.points.reduce(
    (low, point) => (compareDecimals(point.cumulative, low) < 0 ? point.cumulative : low),
    trend.points[0]?.cumulative ?? ZERO,
  );
}

/** El maximo acumulado, para escalar el eje. */
export function trendMaximum(trend: BalanceTrend): Decimal {
  return trend.points.reduce(
    (high, point) => (compareDecimals(point.cumulative, high) > 0 ? point.cumulative : high),
    trend.points[0]?.cumulative ?? ZERO,
  );
}

export interface TrendInput {
  readonly movements: readonly Movement[];
  readonly period: Period;
  readonly today: Day;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
}

/** Un punto por dia del periodo, cortando en el dia de hoy. */
export function balanceTrend(input: TrendInput): BalanceTrend {
  const from = periodFrom(input.period);
  const to = periodTo(input.period);
  const last = isAfter(to, input.today) ? input.today : to;
  if (isBefore(last, from)) return { points: [] };
  const daily = dailyTotals(input, from, last);
  const points: TrendPoint[] = [];
  let running: Decimal = ZERO;
  for (let day = from; !isAfter(day, last); day = addDays(day, 1)) {
    running = addDecimals(running, daily.get(day) ?? ZERO);
    points.push({ date: day, cumulative: running });
  }
  return { points };
}

function dailyTotals(input: TrendInput, from: Day, last: Day): ReadonlyMap<Day, Decimal> {
  const totals = new Map<Day, Decimal>();
  for (const movement of input.movements) {
    if (isBefore(movement.occurredOn, from) || isAfter(movement.occurredOn, last)) continue;
    const value = displayedAmount(movement.money, input.display, input.rates);
    const current = totals.get(movement.occurredOn) ?? ZERO;
    totals.set(
      movement.occurredOn,
      isIncome(movement.kind) ? addDecimals(current, value) : subtractDecimals(current, value),
    );
  }
  return totals;
}
