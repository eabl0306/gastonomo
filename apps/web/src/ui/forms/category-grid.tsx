import type { ReactElement } from 'react';

import type { Category, MovementKind } from '../../domain/ledger/category';
import { Icon } from '../auth/auth-chrome';

/**
 * El grid de categorias del formulario de movimiento.
 *
 * Solo salen las del tipo elegido y las que no estan archivadas: una archivada ya no se
 * ofrece, aunque sus movimientos la sigan usando. Las de tipo `both` salen en los dos grids.
 */
export function CategoryGrid({
  categories,
  kind,
  selected,
  onSelect,
}: {
  categories: readonly Category[];
  kind: MovementKind;
  selected: string | null;
  onSelect: (id: string) => void;
}): ReactElement {
  const visible = categories.filter(
    (category) => !category.archived && (category.kind === kind || category.kind === 'both'),
  );
  return (
    <div aria-label="Categoría" className="g-cats" role="radiogroup">
      {visible.map((category) => (
        <button
          aria-checked={category.id === selected}
          className="g-cat"
          data-active={category.id === selected}
          key={category.id}
          onClick={() => {
            onSelect(category.id);
          }}
          role="radio"
          style={category.id === selected ? { borderColor: category.color } : undefined}
          type="button"
        >
          <span
            className="g-cat__tile"
            style={{ background: `${category.color}18`, color: category.color }}
          >
            <Icon name={category.icon} />
          </span>
          <span className="g-cat__name">{category.name}</span>
        </button>
      ))}
    </div>
  );
}
