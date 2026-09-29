import { isIncome, type MovementKind } from '../../domain/ledger/category';
import type { Movement } from '../../domain/ledger/movement';

/** Las tres pills. Solo filtran lo que se ensena, no lo que se le pide a la API. */
export type MovementFilter = 'all' | 'in' | 'out';

export const MOVEMENT_FILTERS: readonly MovementFilter[] = ['all', 'in', 'out'];

export const FILTER_LABELS: Readonly<Record<MovementFilter, string>> = {
  all: 'Todos',
  in: 'Ingresos',
  out: 'Gastos',
};

function accepts(filter: MovementFilter, kind: MovementKind): boolean {
  if (filter === 'all') return true;
  return filter === 'in' ? isIncome(kind) : !isIncome(kind);
}

export function filterMovements(
  movements: readonly Movement[],
  filter: MovementFilter,
): readonly Movement[] {
  return filter === 'all' ? movements : movements.filter((m) => accepts(filter, m.kind));
}
