import { isIncome } from '../ledger/category';
import type { Movement } from '../ledger/movement';
import type { CurrencyCode } from '../money/currency';
import { sumDisplayed } from '../money/displayed-amount';
import type { ExchangeRates } from '../money/exchange-rates';
import { subtractMoney, type Money } from '../money/money';

/**
 * Ingresos, gastos y neto del periodo. Alimenta la tarjeta de balance de Inicio y el
 * resumen de tres columnas de Movimientos.
 *
 * Ojo con ese resumen: se calcula sobre el periodo **sin filtrar**, asi que hay que pedir
 * la lista sin `kind` y filtrar las pills en local. Pidiendo `kind=out` no se puede pintar
 * la columna de Ingresos.
 */
export interface PeriodSummary {
  readonly income: Money;
  readonly expense: Money;
  readonly net: Money;
}

/** Suma los movimientos del periodo en [display] con las tasas de hoy. */
export function summarizePeriod(
  movements: readonly Movement[],
  display: CurrencyCode,
  rates: ExchangeRates,
): PeriodSummary {
  const incomes = movements.filter((movement) => isIncome(movement.kind));
  const expenses = movements.filter((movement) => !isIncome(movement.kind));
  const income = sumDisplayed(
    incomes.map((movement) => movement.money),
    display,
    rates,
  );
  const expense = sumDisplayed(
    expenses.map((movement) => movement.money),
    display,
    rates,
  );
  return { income, expense, net: subtractMoney(income, expense) };
}

/** Suma de una lista de movimientos en la moneda que se ensena. */
export function totalIn(
  movements: readonly Movement[],
  display: CurrencyCode,
  rates: ExchangeRates,
): Money {
  return sumDisplayed(
    movements.map((movement) => movement.money),
    display,
    rates,
  );
}
