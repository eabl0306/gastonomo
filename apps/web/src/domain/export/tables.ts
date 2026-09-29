import { isIncome } from '../ledger/category';
import type { Loan } from '../ledger/loan';
import { summarize, type LoanStatus } from '../ledger/loan-summary';
import type { Movement } from '../ledger/movement';
import { decimalsOf, type CurrencyCode } from '../money/currency';
import { negateDecimal, roundDecimal } from '../money/decimal';
import { displayedAmount } from '../money/displayed-amount';
import type { ExchangeRates } from '../money/exchange-rates';

import { numberCell, textCell, type ExportTable } from './table';

/** Los tres conjuntos que se pueden exportar. */
export type ExportSet = 'movements' | 'loans' | 'payments';

export const EXPORT_SETS: readonly ExportSet[] = ['movements', 'loans', 'payments'];

export const EXPORT_SET_NAMES: Readonly<Record<ExportSet, string>> = {
  movements: 'Movimientos',
  loans: 'Préstamos',
  payments: 'Abonos',
};

const STATUS_NAMES: Readonly<Record<LoanStatus, string>> = {
  paid: 'Pagado',
  partial: 'Parcial',
  pending: 'Pendiente',
};

export interface ExportInput {
  readonly movements: readonly Movement[];
  readonly loans: readonly Loan[];
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
}

/**
 * Los movimientos, del mas antiguo al mas reciente.
 *
 * Un gasto va con signo negativo: quien sume la columna en la hoja tiene que obtener el neto,
 * y con todo en positivo obtendria la suma de lo que se movio.
 */
function movementsTable(input: ExportInput): ExportTable {
  const scale = decimalsOf(input.display);
  return {
    name: EXPORT_SET_NAMES.movements,
    head: [
      'Fecha',
      'Tipo',
      'Categoría',
      'Descripción',
      'Monto original',
      'Moneda',
      `Monto (${input.display})`,
    ],
    rows: [...input.movements]
      .sort((a, b) => a.occurredOn.localeCompare(b.occurredOn))
      .map((movement) => {
        const income = isIncome(movement.kind);
        const shown = roundDecimal(
          displayedAmount(movement.money, input.display, input.rates),
          scale,
        );
        return [
          textCell(movement.occurredOn),
          textCell(income ? 'Ingreso' : 'Gasto'),
          textCell(movement.category.name),
          textCell(movement.title),
          numberCell(income ? movement.money.amount : negateDecimal(movement.money.amount)),
          textCell(movement.money.currency),
          numberCell(income ? shown : negateDecimal(shown)),
        ];
      }),
  };
}

function loansTable(input: ExportInput): ExportTable {
  const scale = decimalsOf(input.display);
  return {
    name: EXPORT_SET_NAMES.loans,
    head: [
      'Fecha',
      'Persona',
      'Dirección',
      'Nota',
      'Monto original',
      'Moneda',
      `Monto (${input.display})`,
      `Abonado (${input.display})`,
      `Saldo pendiente (${input.display})`,
      'Estado',
    ],
    rows: input.loans.map((loan) => {
      const summary = summarize(loan, input.display, input.rates);
      return [
        textCell(loan.openedOn),
        textCell(loan.person),
        textCell(loan.direction === 'lent' ? 'Me debe' : 'Debo'),
        textCell(loan.note),
        numberCell(loan.money.amount),
        textCell(loan.money.currency),
        numberCell(roundDecimal(summary.total.amount, scale)),
        numberCell(roundDecimal(summary.paid.amount, scale)),
        numberCell(roundDecimal(summary.outstanding.amount, scale)),
        textCell(STATUS_NAMES[summary.status]),
      ];
    }),
  };
}

/** Los abonos de todos los prestamos, juntos y del mas antiguo al mas reciente. */
function paymentsTable(input: ExportInput): ExportTable {
  const scale = decimalsOf(input.display);
  const rows = input.loans.flatMap((loan) => loan.payments.map((payment) => ({ loan, payment })));
  return {
    name: EXPORT_SET_NAMES.payments,
    head: ['Fecha', 'Persona', 'Concepto', 'Monto original', 'Moneda', `Monto (${input.display})`],
    rows: rows
      .sort((a, b) => a.payment.paidOn.localeCompare(b.payment.paidOn))
      .map(({ loan, payment }) => [
        textCell(payment.paidOn),
        textCell(loan.person),
        textCell(payment.label ?? 'Abono'),
        numberCell(payment.money.amount),
        textCell(payment.money.currency),
        numberCell(roundDecimal(displayedAmount(payment.money, input.display, input.rates), scale)),
      ]),
  };
}

export function tableOf(set: ExportSet, input: ExportInput): ExportTable {
  if (set === 'movements') return movementsTable(input);
  return set === 'loans' ? loansTable(input) : paymentsTable(input);
}
