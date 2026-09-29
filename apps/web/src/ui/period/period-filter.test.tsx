import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { fixedClock } from '../../infrastructure/memory/in-memory-ledger';

import { PeriodProvider } from './period-context';
import { PeriodPicker } from './period-picker';
import { RangeSheet } from './range-sheet';

function renderFilter(): void {
  render(
    <PeriodProvider clock={fixedClock('2026-09-22')}>
      <PeriodPicker />
      <RangeSheet />
    </PeriodProvider>,
  );
}

function label(): string {
  return screen.getByText(/2026/u).textContent;
}

function openSheet(): Promise<void> {
  return userEvent.click(screen.getByRole('button', { name: 'Elegir un rango de fechas' }));
}

describe('filtro de periodo', () => {
  it('arranca en el mes de hoy', () => {
    renderFilter();
    expect(label()).toBe('Septiembre 2026');
  });

  it('retrocede y vuelve a avanzar', async () => {
    renderFilter();
    await userEvent.click(screen.getByRole('button', { name: 'Mes anterior' }));
    expect(label()).toBe('Agosto 2026');

    await userEvent.click(screen.getByRole('button', { name: 'Mes siguiente' }));
    expect(label()).toBe('Septiembre 2026');
  });

  /** No hay datos en el futuro, y un boton inerte que se sigue tabulando es un fallo. */
  it('en el mes de hoy no se puede avanzar', () => {
    renderFilter();
    expect(screen.getByRole('button', { name: 'Mes siguiente' })).toBeDisabled();
  });

  it('un preset rellena el borrador y solo Aplicar cambia el periodo', async () => {
    renderFilter();
    await openSheet();
    await userEvent.click(screen.getByRole('button', { name: 'Mes pasado' }));

    expect(label()).toBe('Septiembre 2026');
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar rango' }));
    expect(label()).toBe('1 ago – 31 ago 2026');
  });

  it('con un rango activo el boton de la derecha vuelve al mes de hoy', async () => {
    renderFilter();
    await openSheet();
    await userEvent.click(screen.getByRole('button', { name: 'Este año' }));
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar rango' }));

    expect(screen.getByRole('button', { name: 'Mes anterior' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Volver al mes actual' }));
    expect(label()).toBe('Septiembre 2026');
  });

  /** Ver otras fechas al abrir hace pensar que el filtro es otro. */
  it('el sheet se abre con el periodo vigente', async () => {
    renderFilter();
    await userEvent.click(screen.getByRole('button', { name: 'Mes anterior' }));
    await openSheet();

    expect(screen.getByLabelText('Desde')).toHaveValue('2026-08-01');
    expect(screen.getByLabelText('Hasta')).toHaveValue('2026-08-31');
  });

  it('teclear una fecha desmarca la pill', async () => {
    renderFilter();
    await openSheet();
    await userEvent.click(screen.getByRole('button', { name: 'Este mes' }));
    expect(screen.getByRole('button', { name: 'Este mes' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );

    await userEvent.clear(screen.getByLabelText('Desde'));
    await userEvent.type(screen.getByLabelText('Desde'), '2026-03-01');
    expect(screen.getByRole('button', { name: 'Este mes' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('cerrar sin aplicar no cambia el periodo, y el borrador se tira', async () => {
    renderFilter();
    await openSheet();
    await userEvent.click(screen.getByRole('button', { name: 'Mes pasado' }));
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(label()).toBe('Septiembre 2026');

    await openSheet();
    expect(screen.getByRole('button', { name: 'Mes pasado' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('Escape cierra el sheet', async () => {
    renderFilter();
    await openSheet();
    await userEvent.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  /** Un rango al reves no se puede aplicar: no es un rango peor, es uno que no se consulta. */
  it('con las fechas al reves el boton queda apagado', async () => {
    renderFilter();
    await openSheet();
    await userEvent.clear(screen.getByLabelText('Hasta'));
    await userEvent.type(screen.getByLabelText('Hasta'), '2026-01-01');

    expect(screen.getByRole('button', { name: 'Aplicar rango' })).toBeDisabled();
  });
});
