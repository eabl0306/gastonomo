import { useState, type ReactElement } from 'react';

import type { CategoryGateway } from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';
import { Icon } from '../auth/auth-chrome';
import { LoadingBlock, RetryBlock, messageOf } from '../common/blocks';
import { FilterPills } from '../common/filter-pills';

import { CategoryRow } from './category-row';
import { CategorySheet } from './category-sheet';
import { useCategoriesScreen, type CategoriesState } from './use-categories-screen';

type Kind = 'out' | 'in';

const KIND_LABELS: Readonly<Record<Kind, string>> = { out: 'Gastos', in: 'Ingresos' };
const KINDS: readonly Kind[] = ['out', 'in'];

/**
 * Categorias: las propias, que se editan y se borran, y las predeterminadas, que no.
 *
 * Borrar una categoria con movimientos la **archiva**: reasignar sus movimientos a "Otros"
 * seria escribir en registros ya hechos, y el historico no se toca. Solo se borra de verdad la
 * que no usa nadie.
 */
export function CategoriesScreen({
  gateway,
  onBack,
}: {
  gateway: CategoryGateway;
  onBack: () => void;
}): ReactElement {
  const state = useCategoriesScreen(gateway);
  const [kind, setKind] = useState<Kind>('out');
  const [editing, setEditing] = useState<Category | null>(null);
  const [open, setOpen] = useState(false);
  const edit = (category: Category | null): void => {
    setEditing(category);
    setOpen(true);
  };
  const close = (): void => {
    setOpen(false);
  };
  return (
    <div className="g-cats-screen">
      <TopBar onBack={onBack} title="Categorías" />
      <FilterPills
        label="Tipo de categoría"
        labels={KIND_LABELS}
        onSelect={setKind}
        options={KINDS}
        selected={kind}
      />
      <Body kind={kind} onEdit={edit} state={state} />
      <button
        className="g-cats-screen__new"
        onClick={() => {
          edit(null);
        }}
        type="button"
      >
        <Icon name="add" />
        Nueva categoría
      </button>
      {open ? <Editor editing={editing} onClose={close} state={state} /> : null}
    </div>
  );
}

/** El sheet de crear o editar. Solo se cierra si la API acepto: si no, el borrador se pierde. */
function Editor({
  editing,
  state,
  onClose,
}: {
  editing: Category | null;
  state: CategoriesState;
  onClose: () => void;
}): ReactElement {
  return (
    <CategorySheet
      editing={editing}
      onClose={onClose}
      onSave={(draft) => {
        void state.save(draft, editing?.id ?? null).then((saved) => {
          if (saved) onClose();
        });
      }}
    />
  );
}

/** La barra con la flecha de volver. La comparten las pantallas que cuelgan de Ajustes. */
function TopBar({ title, onBack }: { title: string; onBack: () => void }): ReactElement {
  return (
    <header className="g-detail__bar">
      <button aria-label="Volver" className="g-detail__back" onClick={onBack} type="button">
        <Icon name="arrow_back" />
      </button>
      <h1 className="g-detail__title">{title}</h1>
    </header>
  );
}

/** True si la categoria sale en el grid de este tipo. Las `both` salen en los dos. */
function inKind(category: Category, kind: Kind): boolean {
  return category.kind === kind || category.kind === 'both';
}

function Body({
  state,
  kind,
  onEdit,
}: {
  state: CategoriesState;
  kind: Kind;
  onEdit: (category: Category) => void;
}): ReactElement {
  if (state.error !== null) {
    return <RetryBlock message={messageOf(state.error)} onRetry={state.reload} />;
  }
  if (state.loading) return <LoadingBlock />;
  const visible = state.categories.filter((category) => inKind(category, kind));
  const own = visible.filter((category) => !category.isDefault);
  const builtin = visible.filter((category) => category.isDefault);
  return (
    <>
      {own.length === 0 ? null : (
        <Group title="Mis categorías">
          {own.map((category) => (
            <CategoryRow
              actions={{
                onEdit: () => {
                  onEdit(category);
                },
                onRemove: () => {
                  void state.remove(category.id);
                },
                onRestore: () => {
                  void state.restore(category.id);
                },
              }}
              category={category}
              key={category.id}
            />
          ))}
        </Group>
      )}
      <Group title="Predeterminadas">
        {builtin.map((category) => (
          <CategoryRow actions={null} category={category} key={category.id} />
        ))}
      </Group>
    </>
  );
}

function Group({ title, children }: { title: string; children: React.ReactNode }): ReactElement {
  return (
    <section className="g-cats-screen__group">
      <h2 className="g-settings__title">{title}</h2>
      <div className="g-settings__card">{children}</div>
    </section>
  );
}
