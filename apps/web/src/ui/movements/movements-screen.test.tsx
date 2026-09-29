import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { MovementsPorts } from '../../application/use-cases/load-movements';
import {
  createMemoryCurrencyGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { PeriodProvider } from '../period/period-context';

import { MovementsScreen } from './movements-screen';

function renderMovements(overrides: Partial<MovementsPorts> = {}): void {
  const currencies = createMemoryCurrencyGateway();
  const ports: MovementsPorts = {
    movements: createMemoryMovementGateway(),
    currencies,
    ...overrides,
  };
  render(
    <AccountProvider currencies={currencies} settings={createMemorySettingsGateway()}>
      <PeriodProvider clock={fixedClock(SAMPLE_TODAY)}>
        <MovementsScreen ports={ports} />
      </PeriodProvider>
    </AccountProvider>,
  );
}

function headOf(group: HTMLElement): string {
  return group.querySelector('.g-day__head')?.textContent ?? '';
}

describe('Movimientos', () => {
  it('agrupa por dia, del mas reciente al mas antiguo', async () => {
    renderMovements();
    const groups = await screen.findAllByRole('region');

    expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual([
      'Domingo 20 sept',
      'Viernes 18 sept',
      'Martes 15 sept',
      'Sábado 12 sept',
    ]);
  });

  it('el encabezado de cada dia lleva su neto, con su signo', async () => {
    renderMovements();
    const groups = await screen.findAllByRole('region');

    expect(headOf(groups[0]!)).toBe('Domingo 20 sept+$ 560.000');
    expect(headOf(groups[1]!)).toBe('Viernes 18 sept−$ 186.400');
  });

  /** Si se pidiera la lista con kind=out no se podria pintar la columna de Ingresos. */
  it('el resumen no cambia al filtrar: sale del periodo entero', async () => {
    renderMovements();
    await screen.findAllByRole('region');

    const summary = screen.getByText('Neto').closest('.g-summary')!;
    const before = summary.textContent;

    await userEvent.click(screen.getByRole('tab', { name: 'Gastos' }));
    expect(summary.textContent).toBe(before);
  });

  it('la pill de Gastos deja fuera los ingresos', async () => {
    renderMovements();
    expect(await screen.findByText('Diseño web')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: 'Gastos' }));
    expect(screen.queryByText('Diseño web')).not.toBeInTheDocument();
    expect(screen.getByText('Mercado del mes')).toBeInTheDocument();
  });

  it('el subtitulo es la categoria, sin la fecha, y con el importe original si es de otra moneda', async () => {
    renderMovements();
    expect(await screen.findByText('Freelance · US$ 140,00')).toBeInTheDocument();
    expect(screen.getByText('Transporte')).toBeInTheDocument();
  });

  it('un periodo sin movimientos ensena su hueco', async () => {
    renderMovements();
    await userEvent.click(await screen.findByRole('button', { name: 'Mes anterior' }));

    expect(await screen.findByText('No hay movimientos en este periodo.')).toBeInTheDocument();
  });

  it('un fallo deja la pantalla con su reintento', async () => {
    renderMovements({
      movements: {
        list: () => Promise.resolve({ ok: false, error: { kind: 'network' } }),
        create: () => Promise.reject(new Error('no se usa')),
      },
    });

    expect(await screen.findByText('No hay conexión. Inténtalo de nuevo.')).toBeInTheDocument();
  });
});
