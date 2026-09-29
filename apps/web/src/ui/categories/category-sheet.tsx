import { useState, type ReactElement } from 'react';

import type { CategoryDraft } from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';
import { Icon } from '../auth/auth-chrome';
import { Sheet } from '../common/sheet';
import { SegmentedTabs } from '../forms/form-chrome';

import {
  CATEGORY_COLORS,
  CATEGORY_ICONS,
  DEFAULT_COLOR,
  DEFAULT_ICON,
  MAX_CATEGORY_NAME,
} from './category-palette';

const KIND_TABS = [
  { value: 'out' as const, label: 'Gasto' },
  { value: 'in' as const, label: 'Ingreso' },
];

/**
 * Crear o editar una categoria propia.
 *
 * Solo se ofrecen `in` y `out`: `both` esta reservado a las tres predeterminadas que salen en
 * los dos grids, y una propia de los dos tipos haria que el mismo nombre apareciera dos veces.
 */
export function CategorySheet({
  editing,
  onClose,
  onSave,
}: {
  editing: Category | null;
  onClose: () => void;
  onSave: (draft: CategoryDraft) => void;
}): ReactElement {
  const [draft, setDraft] = useState(() => draftOf(editing));
  const patch = (changes: Partial<CategoryDraft>): void => {
    setDraft((previous) => ({ ...previous, ...changes }));
  };
  const trimmed = draft.name.trim();
  return (
    <Sheet onClose={onClose} title={editing === null ? 'Nueva categoría' : 'Editar categoría'}>
      <Preview color={draft.color} icon={draft.icon} name={trimmed} />
      <Editor draft={draft} onPatch={patch} />
      <button
        className="g-sheet__apply"
        disabled={trimmed === ''}
        onClick={() => {
          onSave({ ...draft, name: trimmed });
        }}
        type="button"
      >
        {editing === null ? 'Crear categoría' : 'Guardar cambios'}
      </button>
    </Sheet>
  );
}

/** Una categoria propia editada, o una nueva con el icono y el color de arranque. */
function draftOf(editing: Category | null): CategoryDraft {
  if (editing === null) {
    return { name: '', kind: 'out', icon: DEFAULT_ICON, color: DEFAULT_COLOR };
  }
  return {
    name: editing.name,
    // `both` no se ofrece: esta reservado a las predeterminadas.
    kind: editing.kind === 'in' ? 'in' : 'out',
    icon: editing.icon,
    color: editing.color,
  };
}

function Editor({
  draft,
  onPatch,
}: {
  draft: CategoryDraft;
  onPatch: (changes: Partial<CategoryDraft>) => void;
}): ReactElement {
  return (
    <>
      <input
        aria-label="Nombre de la categoría"
        className="g-cat-form__input"
        maxLength={MAX_CATEGORY_NAME}
        onChange={(event) => {
          onPatch({ name: event.target.value });
        }}
        placeholder="Nombre de la categoría"
        value={draft.name}
      />
      <SegmentedTabs
        label="Tipo de categoría"
        onSelect={(kind) => {
          onPatch({ kind });
        }}
        options={KIND_TABS}
        selected={draft.kind}
      />
      <Picker
        chosen={draft.icon}
        color={draft.color}
        onChoose={(icon) => {
          onPatch({ icon });
        }}
      />
      <Colors
        chosen={draft.color}
        onChoose={(color) => {
          onPatch({ color });
        }}
      />
    </>
  );
}

function Preview({
  icon,
  color,
  name,
}: {
  icon: string;
  color: string;
  name: string;
}): ReactElement {
  return (
    <div className="g-cat-form__head">
      <span className="g-cat-form__preview" style={{ background: color }}>
        <Icon name={icon} />
      </span>
      <p className="g-cat-form__name">{name === '' ? 'Sin nombre' : name}</p>
    </div>
  );
}

function Picker({
  chosen,
  color,
  onChoose,
}: {
  chosen: string;
  color: string;
  onChoose: (icon: string) => void;
}): ReactElement {
  return (
    <>
      <h3 className="g-cat-form__label">Ícono</h3>
      <div aria-label="Ícono" className="g-cat-form__icons" role="radiogroup">
        {CATEGORY_ICONS.map((icon) => (
          <button
            aria-checked={icon === chosen}
            aria-label={icon}
            className="g-cat-form__icon"
            key={icon}
            onClick={() => {
              onChoose(icon);
            }}
            role="radio"
            style={icon === chosen ? { background: color, borderColor: color } : undefined}
            type="button"
          >
            <Icon name={icon} />
          </button>
        ))}
      </div>
    </>
  );
}

function Colors({
  chosen,
  onChoose,
}: {
  chosen: string;
  onChoose: (color: string) => void;
}): ReactElement {
  return (
    <>
      <h3 className="g-cat-form__label">Color</h3>
      <div aria-label="Color" className="g-cat-form__colors" role="radiogroup">
        {CATEGORY_COLORS.map((color) => (
          <button
            aria-checked={color === chosen}
            aria-label={color}
            className="g-cat-form__color"
            data-active={color === chosen}
            key={color}
            onClick={() => {
              onChoose(color);
            }}
            role="radio"
            style={{ background: color }}
            type="button"
          >
            {color === chosen ? <Icon name="check" /> : null}
          </button>
        ))}
      </div>
    </>
  );
}
