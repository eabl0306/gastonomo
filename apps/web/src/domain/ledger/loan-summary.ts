import { hasNoFraction, type CurrencyCode } from '../money/currency';
import {
  ZERO,
  compareDecimals,
  decimalOf,
  divideDecimals,
  multiplyDecimals,
  roundDecimal,
  signOf,
  subtractDecimals,
  type Decimal,
} from '../money/decimal';
import { displayedMoney, sumDisplayed } from '../money/displayed-amount';
import type { ExchangeRates } from '../money/exchange-rates';
import { addMoney, moneyOf, zeroMoney, type Money } from '../money/money';

import type { Loan, LoanDirection } from './loan';

/** Estado derivado de un prestamo. La API no lo guarda ni lo calcula. */
export type LoanStatus = 'paid' | 'partial' | 'pending';

export function isOpen(status: LoanStatus): boolean {
  return status !== 'paid';
}

/**
 * Lo que la tarjeta y el detalle de un prestamo necesitan y la API no da: saldo, abonado,
 * estado, porcentaje e intereses ganados.
 */
export interface LoanSummary {
  readonly total: Money;
  readonly paid: Money;
  /** Saldo pendiente, ya en cero cuando lo que queda cabe dentro del epsilon de la moneda. */
  readonly outstanding: Money;
  /** El excedente cuando lo abonado se pasa del monto. */
  readonly interest: Money;
  readonly status: LoanStatus;
  readonly percent: number;
}

/** Porcentaje maximo que ensena la barra, aunque lo abonado se pase del monto. */
export const MAX_PERCENT = 100;

const HALF_CENT = decimalOf('0.005');
const HALF_UNIT = decimalOf('0.5');
const HUNDRED = decimalOf('100');

/**
 * El margen con el que se da por pagado un prestamo.
 *
 * Sin el, una conversion deja un saldo de 0,0001 y un prestamo pagado sale como Parcial
 * para siempre.
 */
function epsilonOf(display: CurrencyCode): Decimal {
  return hasNoFraction(display) ? HALF_UNIT : HALF_CENT;
}

function statusOf(outstanding: Decimal, paid: Decimal): LoanStatus {
  if (signOf(outstanding) === 0) return 'paid';
  return signOf(paid) > 0 ? 'partial' : 'pending';
}

function percentOf(paid: Decimal, total: Decimal): number {
  if (signOf(total) === 0) return MAX_PERCENT;
  const ratio = divideDecimals(multiplyDecimals(paid, HUNDRED), total);
  if (ratio === null) return MAX_PERCENT;
  return Math.min(MAX_PERCENT, Number(roundDecimal(ratio, 0).units));
}

/** Calcula el resumen en [display] con las tasas de hoy. */
export function summarize(loan: Loan, display: CurrencyCode, rates: ExchangeRates): LoanSummary {
  const total = displayedMoney(loan.money, display, rates);
  const paid = sumDisplayed(
    loan.payments.map((payment) => payment.money),
    display,
    rates,
  );
  const epsilon = epsilonOf(display);
  const remainder = subtractDecimals(total.amount, paid.amount);
  const outstanding = compareDecimals(remainder, epsilon) < 0 ? ZERO : remainder;
  const surplus = subtractDecimals(paid.amount, total.amount);
  const interest = compareDecimals(surplus, epsilon) > 0 ? surplus : ZERO;
  return {
    total,
    paid,
    outstanding: moneyOf(outstanding, display),
    interest: moneyOf(interest, display),
    status: statusOf(outstanding, paid.amount),
    percent: percentOf(paid.amount, total.amount),
  };
}

/** Un prestamo con su resumen, que es como viajan por las pantallas. */
export interface LoanRow {
  readonly loan: Loan;
  readonly summary: LoanSummary;
}

/**
 * Las dos tarjetas de arriba de Prestamos: el saldo pendiente total de una direccion y
 * cuantos prestamos siguen abiertos.
 *
 * Solo cuentan los abiertos, asi que la tarjeta dice "Nada pendiente" aunque haya
 * prestamos pagados en la lista. No depende del periodo: son saldos de hoy.
 */
export interface LoanTotals {
  readonly outstanding: Money;
  readonly openCount: number;
}

export function loanTotals(
  rows: readonly LoanRow[],
  direction: LoanDirection,
  display: CurrencyCode,
): LoanTotals {
  const open = rows.filter((row) => row.loan.direction === direction && isOpen(row.summary.status));
  const outstanding = open.reduce(
    (sum, row) => addMoney(sum, row.summary.outstanding),
    zeroMoney(display),
  );
  return { outstanding, openCount: open.length };
}

/** Ordena por estado y, dentro de cada grupo, del mas reciente al mas antiguo. */
export function sortedLoans(rows: readonly LoanRow[]): readonly LoanRow[] {
  return [...rows].sort((a, b) => {
    const byStatus = Number(isOpen(b.summary.status)) - Number(isOpen(a.summary.status));
    if (byStatus !== 0) return byStatus;
    const byDate = b.loan.openedOn.localeCompare(a.loan.openedOn);
    return byDate !== 0 ? byDate : b.loan.id.localeCompare(a.loan.id);
  });
}
