import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { LoanDraft, LoanGateway } from '../../application/ports/loan-gateway';
import {
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_LOANS, SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';

import { NewLoanScreen } from './new-loan-screen';

function renderForm(sent: LoanDraft[] = [], onSaved = vi.fn()): void {
  const gateway: LoanGateway = {
    ...createMemoryLoanGateway(),
    create: (draft) => {
      sent.push(draft);
      return Promise.resolve({ ok: true, value: SAMPLE_LOANS[0]! });
    },
  };
  render(
    <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
      <AccountProvider
        currencies={createMemoryCurrencyGateway()}
        settings={createMemorySettingsGateway()}
      >
        <NewLoanScreen gateway={gateway} onClose={vi.fn()} onSaved={onSaved} />
      </AccountProvider>
    </TodayProvider>,
  );
}

describe('Nuevo préstamo', () => {
  /** Un prestamo sin persona no se puede cobrar ni devolver, y la API lo rechazaria. */
  it('no se puede crear sin importe ni persona', async () => {
    renderForm();
    const save = screen.getByRole('button', { name: 'Crear préstamo' });
    expect(save).toBeDisabled();

    await userEvent.type(screen.getByRole('textbox', { name: 'Importe' }), '250000');
    expect(save).toBeDisabled();

    await userEvent.type(screen.getByRole('textbox', { name: /A quién/u }), 'Rosa');
    expect(save).toBeEnabled();
  });

  it('el aviso dice lo que pasa en el historico, y cambia con la direccion', async () => {
    renderForm();
    expect(screen.getByText(/Se registrará como gasto/u)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: 'Me prestaron' }));
    expect(screen.getByText(/Se registrará como ingreso/u)).toBeInTheDocument();
    expect(screen.getByText('¿Quién te prestó?')).toBeInTheDocument();
  });

  it('manda la direccion, la persona recortada y los cuatro campos', async () => {
    const sent: LoanDraft[] = [];
    renderForm(sent);
    await userEvent.type(screen.getByRole('textbox', { name: 'Importe' }), '250000');
    await userEvent.type(screen.getByRole('textbox', { name: /A quién/u }), '  Rosa  ');
    await userEvent.click(screen.getByRole('button', { name: 'Crear préstamo' }));

    expect(sent).toHaveLength(1);
    expect(sent[0]).toMatchObject({
      person: 'Rosa',
      direction: 'lent',
      openedOn: SAMPLE_TODAY,
      note: '',
    });
    expect(sent[0]?.money.currency).toBe('COP');
  });
});
