import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { LoanPorts } from '../../application/use-cases/load-loan-list';
import {
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';

import { LoansScreen, type LoansActions } from './loans-screen';

const NOOP: LoansActions = { onOpenLoan: () => undefined };

function renderLoans(overrides: Partial<LoanPorts> = {}, actions: LoansActions = NOOP): void {
  const currencies = createMemoryCurrencyGateway();
  const ports: LoanPorts = { loans: createMemoryLoanGateway(), ...overrides };
  render(
    <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
      <AccountProvider currencies={currencies} settings={createMemorySettingsGateway()}>
        <LoansScreen actions={actions} ports={ports} />
      </AccountProvider>
    </TodayProvider>,
  );
}

function cardOf(person: string): HTMLElement {
  return closestOf(screen.getByText(person), 'article');
}

function closestOf(from: HTMLElement, selector: string): HTMLElement {
  return from.closest<HTMLElement>(selector)!;
}

describe('Préstamos', () => {
  it('suma solo los prestamos abiertos en las dos tarjetas de arriba', async () => {
    renderLoans();
    await screen.findByText('Ana');

    // Ana debe 250.000 de 400.000; Marta ya pago, asi que no suma ni cuenta.
    const owed = closestOf(screen.getByText('Te deben'), '.g-loan-head');
    expect(within(owed).getByLabelText('$ 250.000')).toBeInTheDocument();
    expect(within(owed).getByText('1 préstamo')).toBeInTheDocument();

    const owe = closestOf(screen.getByText('Debes'), '.g-loan-head');
    expect(within(owe).getByLabelText('$ 220.000')).toBeInTheDocument();
  });

  it('cada tarjeta dice su estado, su saldo y su porcentaje', async () => {
    renderLoans();
    const ana = within(await screen.findByText('Ana').then(() => cardOf('Ana')));

    expect(ana.getByText('Parcial')).toBeInTheDocument();
    expect(ana.getByText('Saldo pendiente')).toBeInTheDocument();
    expect(ana.getByLabelText('$ 250.000')).toBeInTheDocument();
    expect(ana.getByText('de $ 400.000')).toBeInTheDocument();
    expect(ana.getByText('38%')).toBeInTheDocument();
  });

  /** Un prestamo pagado no ensena un cero: un cero se leeria como "no hay nada". */
  it('uno pagado ensena lo que se presto y no ofrece abonar', async () => {
    renderLoans();
    const marta = within(await screen.findByText('Marta').then(() => cardOf('Marta')));

    expect(marta.getByText('Pagado')).toBeInTheDocument();
    expect(marta.getByText('Pagado completo')).toBeInTheDocument();
    expect(marta.getByLabelText('$ 90.000')).toBeInTheDocument();
    expect(marta.queryByRole('button', { name: /Registrar abono/u })).not.toBeInTheDocument();
  });

  it('los abiertos van antes que los pagados', async () => {
    renderLoans();
    await screen.findByText('Ana');

    const people = screen.getAllByRole('article').map((card) => within(card).getAllByText(/./u)[0]);
    expect(people.map((node) => node?.textContent)).toEqual(['A', 'J', 'M']);
  });

  it('la pill de Debo deja fuera los que me deben', async () => {
    renderLoans();
    await screen.findByText('Ana');

    await userEvent.click(screen.getByRole('tab', { name: 'Debo' }));
    expect(screen.queryByText('Ana')).not.toBeInTheDocument();
    expect(screen.getByText('Julián')).toBeInTheDocument();
  });

  it('una lista vacia ensena su hueco', async () => {
    renderLoans({
      loans: {
        list: () => Promise.resolve({ ok: true, value: [] }),
        byId: () => Promise.reject(new Error('no se usa')),
        create: () => Promise.reject(new Error('no se usa')),
        addPayment: () => Promise.reject(new Error('no se usa')),
      },
    });

    expect(await screen.findByText('No tienes préstamos en esta lista.')).toBeInTheDocument();
  });

  /** La tarjeta entera abre el detalle, asi que el boton de abonar tiene que parar el clic. */
  it('el boton de abonar abre el sheet y no el detalle', async () => {
    const actions = { onOpenLoan: vi.fn() };
    renderLoans({}, actions);
    await screen.findByText('Ana');

    await userEvent.click(within(cardOf('Ana')).getByRole('button', { name: /Registrar abono/u }));
    expect(screen.getByRole('dialog', { name: 'Registrar abono' })).toBeInTheDocument();
    expect(actions.onOpenLoan).not.toHaveBeenCalled();
  });

  it('la tarjeta abre el detalle', async () => {
    const actions = { onOpenLoan: vi.fn() };
    renderLoans({}, actions);
    await screen.findByText('Ana');

    await userEvent.click(screen.getByRole('button', { name: 'Ver el préstamo de Ana' }));
    expect(actions.onOpenLoan).toHaveBeenCalledWith('l1');
  });

  it('un fallo deja la pantalla con su reintento', async () => {
    renderLoans({
      loans: {
        list: () => Promise.resolve({ ok: false, error: { kind: 'network' } }),
        byId: () => Promise.reject(new Error('no se usa')),
        create: () => Promise.reject(new Error('no se usa')),
        addPayment: () => Promise.reject(new Error('no se usa')),
      },
    });

    expect(await screen.findByText('No hay conexión. Inténtalo de nuevo.')).toBeInTheDocument();
  });
});
