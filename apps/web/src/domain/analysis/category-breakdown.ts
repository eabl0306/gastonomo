import { isIncome, type Category } from '../ledger/category';
import type { Movement } from '../ledger/movement';
import type { CurrencyCode } from '../money/currency';
import {
  DIVISION_SCALE,
  compareDecimals,
  decimalOf,
  divideDecimals,
  multiplyDecimals,
  roundDecimal,
  signOf,
  type Decimal,
} from '../money/decimal';
import { sumDisplayed } from '../money/displayed-amount';
import type { ExchangeRates } from '../money/exchange-rates';
import type { Money } from '../money/money';

/** Una porcion del grafico de gastos por categoria. */
export interface CategorySlice {
  readonly category: Category;
  readonly total: Money;
  /** Fraccion del total del periodo, entre 0 y 1. Es el ancho de la porcion. */
  readonly fraction: number;
  /** El porcentaje redondeado que ensena la fila. */
  readonly percent: number;
}

/**
 * El grafico de gastos por categoria de Inicio: la barra apilada con las categorias de mas
 * gasto y las filas de debajo.
 *
 * Cada movimiento ya trae su categoria completa, asi que no hay que cruzar nada.
 */
export interface CategoryBreakdown {
  readonly total: Money;
  readonly slices: readonly CategorySlice[];
}

/** Cuantas categorias ensena la barra apilada del diseno. */
export const TOP_SLICES = 6;

const HUNDRED = decimalOf('100');

interface CategoryGroup {
  readonly category: Category;
  readonly movements: Movement[];
}

function groupByCategory(expenses: readonly Movement[]): readonly CategoryGroup[] {
  const groups = new Map<string, CategoryGroup>();
  for (const movement of expenses) {
    const group = groups.get(movement.category.id);
    if (group === undefined) {
      groups.set(movement.category.id, { category: movement.category, movements: [movement] });
    } else {
      group.movements.push(movement);
    }
  }
  return [...groups.values()];
}

/** Las categorias con mas gasto del periodo, de mayor a menor. */
export function breakdownOfExpenses(
  movements: readonly Movement[],
  display: CurrencyCode,
  rates: ExchangeRates,
  limit: number = TOP_SLICES,
): CategoryBreakdown {
  const expenses = movements.filter((movement) => !isIncome(movement.kind));
  const total = sumDisplayed(
    expenses.map((movement) => movement.money),
    display,
    rates,
  );
  if (signOf(total.amount) === 0) return { total, slices: [] };
  const slices = groupByCategory(expenses)
    .map((group) => sliceOf(group, display, rates, total.amount))
    .sort((a, b) => compareDecimals(b.total.amount, a.total.amount))
    .slice(0, limit);
  return { total, slices };
}

function sliceOf(
  group: CategoryGroup,
  display: CurrencyCode,
  rates: ExchangeRates,
  total: Decimal,
): CategorySlice {
  const amount = sumDisplayed(
    group.movements.map((movement) => movement.money),
    display,
    rates,
  );
  const ratio = divideDecimals(amount.amount, total, DIVISION_SCALE);
  return {
    category: group.category,
    total: amount,
    fraction: ratio === null ? 0 : Number(ratio.units) / 10 ** ratio.scale,
    percent: ratio === null ? 0 : Number(roundDecimal(multiplyDecimals(ratio, HUNDRED), 0).units),
  };
}
