import type { ReactElement } from 'react';

import type { Category } from '../../domain/ledger/category';
import { Icon } from '../auth/auth-chrome';

import { usesLabel } from './category-palette';

/** Lo que se puede hacer con una categoria propia. Null en una predeterminada. */
export interface CategoryActions {
  readonly onEdit: () => void;
  readonly onRemove: () => void;
  readonly onRestore: () => void;
}

/**
 * Una fila de la lista.
 *
 * Una predeterminada no trae acciones: se marca con un candado. Una archivada sale con el
 * boton de devolverla, que es lo unico que se puede hacer con ella.
 */
export function CategoryRow({
  category,
  actions,
}: {
  category: Category;
  actions: CategoryActions | null;
}): ReactElement {
  return (
    <div className="g-cat-row" data-archived={category.archived}>
      <span
        className="g-cat-row__tile"
        style={{ background: `${category.color}18`, color: category.color }}
      >
        <Icon name={category.icon} />
      </span>
      <span className="g-cat-row__body">
        <span className="g-cat-row__name">{category.name}</span>
        <span className="g-cat-row__uses">
          {category.archived ? 'Archivada' : usesLabel(category.movementCount)}
        </span>
      </span>
      <Actions actions={actions} archived={category.archived} name={category.name} />
    </div>
  );
}

function Actions({
  name,
  archived,
  actions,
}: {
  name: string;
  archived: boolean;
  actions: CategoryActions | null;
}): ReactElement {
  if (actions === null) return <Icon name="lock" />;
  if (archived) {
    return (
      <button
        aria-label={`Restaurar ${name}`}
        className="g-cat-row__action"
        onClick={actions.onRestore}
        type="button"
      >
        <Icon name="unarchive" />
      </button>
    );
  }
  return (
    <>
      <button
        aria-label={`Editar ${name}`}
        className="g-cat-row__action"
        onClick={actions.onEdit}
        type="button"
      >
        <Icon name="edit" />
      </button>
      <button
        aria-label={`Borrar ${name}`}
        className="g-cat-row__action"
        data-danger=""
        onClick={actions.onRemove}
        type="button"
      >
        <Icon name="delete" />
      </button>
    </>
  );
}
