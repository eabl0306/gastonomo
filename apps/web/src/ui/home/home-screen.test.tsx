import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { CurrencyGateway } from '../../application/ports/currency-gateway';
import type { HomePorts } from '../../application/use-cases/load-home';
import { createMemoryAuthGateway } from '../../infrastructure/memory/in-memory-auth';
import {
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { PeriodProvider } from '../period/period-context';

import { HomeScreen } from './home-screen';

const NOOP_ACTIONS = {
  onNewMovement: () => undefined,
  onNewLoan: () => undefined,
  onOpenLoans: () => undefined,
  onOpenMovements: () => undefined,
  onOpenSettings: () => undefined,
};

function renderHome(
  overrides: Partial<HomePorts> = {},
  actions = NOOP_ACTIONS,
  currencies: CurrencyGateway = createMemoryCurrencyGateway(),
): ReactElement {
  const ports: HomePorts = {
    movements: createMemoryMovementGateway(),
    loans: createMemoryLoanGateway(),
    auth: createMemoryAuthGateway(),
    ...overrides,
  };
  const tree = (
    <AccountProvider currencies={currencies} settings={createMemorySettingsGateway()}>
      <PeriodProvider clock={fixedClock(SAMPLE_TODAY)}>
        <HomeScreen actions={actions} ports={ports} />
      </PeriodProvider>
    </AccountProvider>
  );
  render(tree);
  return tree;
}

describe('Inicio', () => {
  // El ojo se recuerda en el dispositivo, asi que sobrevive de un test al siguiente.
  beforeEach(() => {
    globalThis.localStorage.clear();
  });

  it('saluda con el nombre y con la fecha de hoy', async () => {
    renderHome();
    expect(await screen.findByText('Hola, Camilo')).toBeInTheDocument();
    expect(screen.getByText('Martes, 22 sept')).toBeInTheDocument();
  });

  /** 560.000 de ingreso menos 186.400 + 32.000 + 18.500 de gastos. */
  it('ensena el balance del periodo con sus ingresos y sus gastos', async () => {
    renderHome();
    expect(await screen.findByLabelText('$ 323.100')).toBeInTheDocument();
    expect(screen.getByText('Balance de septiembre')).toBeInTheDocument();
    expect(screen.getByLabelText('$ 560.000')).toBeInTheDocument();
    // Sale dos veces: en la tarjeta de gastos y como total de "Gastos por categoría".
    expect(screen.getAllByLabelText('$ 236.900')).toHaveLength(2);
  });

  /**
   * El ojo tapa las cifras agregadas, no el detalle de una fila: los importes de "Ultimos
   * movimientos" se siguen viendo, igual que en el movil.
   */
  it('el ojo oculta el balance pero no los ultimos movimientos', async () => {
    renderHome();
    await userEvent.click(await screen.findByRole('button', { name: 'Ocultar los montos' }));

    expect(screen.queryByLabelText('$ 323.100')).not.toBeInTheDocument();
    expect(screen.getAllByLabelText('Importe oculto').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('más $ 560.000')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mostrar los montos' })).toBeInTheDocument();
  });

  it('el subtitulo de un registro en otra moneda ensena el importe original', async () => {
    renderHome();
    const rows = await screen.findByText('Últimos movimientos');
    const card = rows.closest('section');
    expect(within(card!).getByText('Freelance · 20 sept · US$ 140,00')).toBeInTheDocument();
  });

  it('las dos tarjetas de prestamos no dependen del periodo', async () => {
    renderHome();
    expect(await screen.findByLabelText('$ 250.000')).toBeInTheDocument();
    expect(screen.getByLabelText('$ 220.000')).toBeInTheDocument();
    expect(screen.getAllByText('1 préstamo')).toHaveLength(2);
  });

  it('los gastos por categoria salen de mayor a menor', async () => {
    renderHome();
    expect(await screen.findByText('Alimentación')).toBeInTheDocument();
    expect(screen.getByText('92%')).toBeInTheDocument();
    expect(screen.getByText('8%')).toBeInTheDocument();
  });

  it('un fallo de la lista deja la pantalla con su reintento, y el reintento recarga', async () => {
    let fail = true;
    const list = vi.fn(() =>
      fail
        ? Promise.resolve({ ok: false as const, error: { kind: 'network' as const } })
        : createMemoryMovementGateway().list({}),
    );
    renderHome({ movements: { list, create: () => Promise.reject(new Error('no se usa')) } });

    expect(await screen.findByText('No hay conexión. Inténtalo de nuevo.')).toBeInTheDocument();
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Balance de septiembre')).toBeInTheDocument();
  });

  /** Un 503 de tasas no es un fallo de la pantalla: lo de la moneda principal se ensena igual. */
  it('sin tasas se avisa con una linea y el contenido sigue', async () => {
    renderHome({}, NOOP_ACTIONS, {
      catalog: () => Promise.resolve({ ok: false, error: { kind: 'service-unavailable' } }),
      rates: () => Promise.resolve({ ok: false, error: { kind: 'service-unavailable' } }),
    });

    expect(await screen.findByText('Balance de septiembre')).toBeInTheDocument();
    expect(screen.getByText('Las tasas de cambio aún no están listas.')).toBeInTheDocument();
  });

  it('las acciones rapidas y Ver todos llevan a donde toca', async () => {
    const actions = {
      onNewMovement: vi.fn(),
      onNewLoan: vi.fn(),
      onOpenLoans: vi.fn(),
      onOpenMovements: vi.fn(),
      onOpenSettings: vi.fn(),
    };
    renderHome({}, actions);

    await userEvent.click(await screen.findByRole('button', { name: /Gasto/u }));
    expect(actions.onNewMovement).toHaveBeenCalledWith('out');

    // Un abono no existe sin prestamo: la cuarta accion lleva a elegir cual.
    await userEvent.click(screen.getByRole('button', { name: /Abono/u }));
    expect(actions.onOpenLoans).toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Ver todos' }));
    expect(actions.onOpenMovements).toHaveBeenCalled();
  });
});
