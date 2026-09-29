import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { LoanPorts } from '../../application/use-cases/load-loan-list';
import {
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemorySettingsGateway,
} from '../../infrastructure/memory/in-memory-ledger';
import { AccountProvider } from '../account/account-context';

import { LoanDetailScreen, type LoanDetailActions } from './loan-detail-screen';

const NOOP: LoanDetailActions = {
  onBack: () => undefined,
  onRegisterPayment: () => undefined,
};

function renderDetail(loanId: string, actions: LoanDetailActions = NOOP): void {
  const currencies = createMemoryCurrencyGateway();
  const ports: LoanPorts = { loans: createMemoryLoanGateway(), currencies };
  render(
    <AccountProvider currencies={currencies} settings={createMemorySettingsGateway()}>
      <LoanDetailScreen actions={actions} loanId={loanId} ports={ports} />
    </AccountProvider>,
  );
}

describe('Detalle del préstamo', () => {
  it('ensena quien, el saldo, lo abonado y el estado', async () => {
    renderDetail('l1');
    expect(await screen.findByText('Ana')).toBeInTheDocument();

    expect(screen.getByText('Creado el 10 ago 2026')).toBeInTheDocument();
    expect(screen.getByText('Me debe')).toBeInTheDocument();
    expect(screen.getByText('Viaje')).toBeInTheDocument();
    expect(screen.getByText('Saldo pendiente')).toBeInTheDocument();
    expect(screen.getByLabelText('$ 250.000')).toBeInTheDocument();
    expect(screen.getByLabelText('$ 400.000')).toBeInTheDocument();
    expect(screen.getByLabelText('Parcial · 38%')).toBeInTheDocument();
    // 150.000 sale dos veces: como abonado y como el unico abono del historial.
    expect(screen.getAllByLabelText('$ 150.000')).toHaveLength(2);
  });

  it('el historial va del abono mas reciente al mas antiguo', async () => {
    renderDetail('l3');
    expect(await screen.findByText('Marta')).toBeInTheDocument();

    const history = within(screen.getByText('Historial de abonos').parentElement!);
    expect(history.getByText('Abono · Todo de una')).toBeInTheDocument();
    expect(history.getByText('20 jun 2026')).toBeInTheDocument();
  });

  it('un prestamo pagado no ofrece abonar', async () => {
    renderDetail('l3');
    expect(await screen.findByText('Marta')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Registrar abono/u })).not.toBeInTheDocument();
  });

  it('sin abonos lo dice', async () => {
    renderDetail('l2');
    expect(await screen.findByText('Julián')).toBeInTheDocument();
    expect(screen.getByText('Aún no hay abonos.')).toBeInTheDocument();
  });

  /** Un prestamo de otra cuenta responde 404, y aqui se ve igual que uno que no existe. */
  it('un identificador que no esta en la lista lo dice', async () => {
    renderDetail('no-existe');
    expect(await screen.findByText('No encontramos este préstamo.')).toBeInTheDocument();
  });

  it('el boton de volver avisa', async () => {
    const actions = { onBack: vi.fn(), onRegisterPayment: vi.fn() };
    renderDetail('l1', actions);
    await screen.findByText('Ana');

    await userEvent.click(screen.getByRole('button', { name: 'Volver' }));
    expect(actions.onBack).toHaveBeenCalled();
  });
});
