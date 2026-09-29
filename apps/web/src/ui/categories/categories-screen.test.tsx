import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { CategoryDraft, CategoryGateway } from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';
import { SAMPLE_CATEGORIES } from '../../infrastructure/memory/sample-ledger';

import { CategoriesScreen } from './categories-screen';

const ARCHIVED: Category = {
  id: 'c9',
  name: 'Suscripciones',
  kind: 'out',
  icon: 'subscriptions',
  color: '#7a52c7',
  isDefault: false,
  movementCount: 4,
  archived: true,
};

const OWN: Category = { ...ARCHIVED, id: 'c8', name: 'Mascotas', icon: 'pets', archived: false };

function gatewayOf(calls: string[]): CategoryGateway {
  return {
    list: () => Promise.resolve({ ok: true, value: [...SAMPLE_CATEGORIES, OWN, ARCHIVED] }),
    create: (draft: CategoryDraft) => {
      calls.push(`create:${draft.name}:${draft.kind}:${draft.icon}`);
      return Promise.resolve({ ok: true, value: OWN });
    },
    update: (id) => {
      calls.push(`update:${id}`);
      return Promise.resolve({ ok: true, value: OWN });
    },
    remove: (id) => {
      calls.push(`remove:${id}`);
      return Promise.resolve({ ok: true, value: 'archived' as const });
    },
    restore: (id) => {
      calls.push(`restore:${id}`);
      return Promise.resolve({ ok: true, value: OWN });
    },
  };
}

function renderScreen(calls: string[] = []): void {
  render(<CategoriesScreen gateway={gatewayOf(calls)} onBack={vi.fn()} />);
}

function rowOf(name: string): HTMLElement {
  return screen.getByText(name).closest<HTMLElement>('.g-cat-row')!;
}

describe('Categorías', () => {
  it('separa las propias de las predeterminadas', async () => {
    renderScreen();
    expect(await screen.findByText('Mis categorías')).toBeInTheDocument();
    expect(screen.getByText('Predeterminadas')).toBeInTheDocument();
  });

  /** Una predeterminada no se puede tocar: se marca con un candado. */
  it('una predeterminada no trae acciones', async () => {
    renderScreen();
    await screen.findByText('Alimentación');
    const row = within(rowOf('Alimentación'));

    expect(row.queryByRole('button')).not.toBeInTheDocument();
    expect(row.getByText('2 movimientos')).toBeInTheDocument();
  });

  /** Una archivada ya no se ofrece al registrar; lo unico que se puede es devolverla. */
  it('una archivada solo ofrece restaurar', async () => {
    const calls: string[] = [];
    renderScreen(calls);
    await screen.findByText('Suscripciones');
    const row = within(rowOf('Suscripciones'));

    expect(row.getByText('Archivada')).toBeInTheDocument();
    expect(row.queryByRole('button', { name: /Borrar/u })).not.toBeInTheDocument();

    await userEvent.click(row.getByRole('button', { name: 'Restaurar Suscripciones' }));
    expect(calls).toContain('restore:c9');
  });

  it('borrar una propia la manda a la API', async () => {
    const calls: string[] = [];
    renderScreen(calls);
    await screen.findByText('Mascotas');

    await userEvent.click(
      within(rowOf('Mascotas')).getByRole('button', { name: 'Borrar Mascotas' }),
    );
    expect(calls).toContain('remove:c8');
  });

  it('la pill de Ingresos cambia la lista', async () => {
    renderScreen();
    await screen.findByText('Alimentación');

    await userEvent.click(screen.getByRole('tab', { name: 'Ingresos' }));
    expect(screen.queryByText('Alimentación')).not.toBeInTheDocument();
    expect(screen.getByText('Freelance')).toBeInTheDocument();
  });

  it('crear pide nombre y manda lo elegido', async () => {
    const calls: string[] = [];
    renderScreen(calls);
    await screen.findByText('Alimentación');

    await userEvent.click(screen.getByRole('button', { name: /Nueva categoría/u }));
    const create = screen.getByRole('button', { name: 'Crear categoría' });
    expect(create).toBeDisabled();

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre de la categoría' }), 'Café');
    await userEvent.click(screen.getByRole('radio', { name: 'local_cafe' }));
    await userEvent.click(create);

    expect(calls).toContain('create:Café:out:local_cafe');
  });

  it('editar una propia la manda con su identificador', async () => {
    const calls: string[] = [];
    renderScreen(calls);
    await screen.findByText('Mascotas');

    await userEvent.click(
      within(rowOf('Mascotas')).getByRole('button', { name: 'Editar Mascotas' }),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(calls).toContain('update:c8');
  });
});
