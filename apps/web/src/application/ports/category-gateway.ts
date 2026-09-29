import type { Category } from '../../domain/ledger/category';

import type { Outcome } from './api-error';

/** Lo que se manda al crear o editar una categoria. */
export interface CategoryDraft {
  readonly name: string;
  /** Crear solo ofrece `in` y `out`: `both` esta reservado a las predeterminadas. */
  readonly kind: 'in' | 'out';
  readonly icon: string;
  readonly color: string;
}

/**
 * Que paso al borrar.
 *
 * Una categoria con movimientos se **archiva**, no se destruye: reasignar sus movimientos
 * a "Otros" seria escribir en registros ya hechos, y el historico no se toca.
 */
export type RemovalOutcome = 'deleted' | 'archived';

export interface CategoryGateway {
  list: (includeArchived: boolean) => Promise<Outcome<readonly Category[]>>;
  create: (draft: CategoryDraft) => Promise<Outcome<Category>>;
  update: (id: string, draft: Partial<CategoryDraft>) => Promise<Outcome<Category>>;
  remove: (id: string) => Promise<Outcome<RemovalOutcome>>;
  restore: (id: string) => Promise<Outcome<Category>>;
}
