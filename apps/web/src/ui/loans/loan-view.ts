import type { LoanDirection } from '../../domain/ledger/loan';
import type { LoanRow, LoanStatus } from '../../domain/ledger/loan-summary';
import { isZeroMoney } from '../../domain/money/money';

/** Las tres pills de la lista. Filtran en local, como las de Movimientos. */
export type LoanFilter = 'all' | 'lent' | 'borrowed';

export const LOAN_FILTERS: readonly LoanFilter[] = ['all', 'lent', 'borrowed'];

export const LOAN_FILTER_LABELS: Readonly<Record<LoanFilter, string>> = {
  all: 'Todos',
  lent: 'Me deben',
  borrowed: 'Debo',
};

export function filterLoans(rows: readonly LoanRow[], filter: LoanFilter): readonly LoanRow[] {
  return filter === 'all' ? rows : rows.filter((row) => row.loan.direction === filter);
}

export const STATUS_LABELS: Readonly<Record<LoanStatus, string>> = {
  paid: 'Pagado',
  partial: 'Parcial',
  pending: 'Pendiente',
};

/** "Me debe" o "Debo", que es como la tarjeta nombra la direccion. */
export function directionLabel(direction: LoanDirection): string {
  return direction === 'lent' ? 'Me debe' : 'Debo';
}

/**
 * La cifra grande de la tarjeta y lo que dice encima.
 *
 * Un prestamo pagado no ensena un cero: ensena lo que se presto, o los intereses si lo
 * abonado se paso del monto. Un cero ahi se leeria como "no hay nada", que es otra cosa.
 */
export interface Headline {
  readonly label: string;
  readonly amount: LoanRow['summary']['total'];
  readonly tone: 'lent' | 'borrowed' | 'paid' | 'interest';
}

export function headlineOf(row: LoanRow): Headline {
  const summary = row.summary;
  if (summary.status !== 'paid') {
    return {
      label: 'Saldo pendiente',
      amount: summary.outstanding,
      tone: row.loan.direction,
    };
  }
  if (!isZeroMoney(summary.interest)) {
    return { label: 'Intereses ganados', amount: summary.interest, tone: 'interest' };
  }
  return { label: 'Pagado completo', amount: summary.total, tone: 'paid' };
}

/** La inicial del avatar. Vacia si el nombre lo esta, que la API no deja. */
export function initialOf(person: string): string {
  return person.trim().slice(0, 1).toUpperCase();
}
