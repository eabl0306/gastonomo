import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { LoanGateway, PaymentDraft } from '../../application/ports/loan-gateway';
import { summarize } from '../../domain/ledger/loan-summary';
import { COP } from '../../domain/money/currency';
import {
  createMemoryCurrencyGateway,
  createMemoryLoanGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import {
  SAMPLE_LOANS,
  SAMPLE_RATES,
  SAMPLE_TODAY,
} from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';

import { PaymentSheet } from './payment-sheet';

const LOAN = SAMPLE_LOANS[0]!;
const ROW = { loan: LOAN, summary: summarize(LOAN, COP, SAMPLE_RATES) };

function renderSheet(sent: PaymentDraft[] = [], onSaved = vi.fn()): void {
  const gateway: LoanGateway = {
    ...createMemoryLoanGateway(),
    addPayment: (_loanId, draft) => {
      sent.push(draft);
      return Promise.resolve({ ok: true, value: LOAN });
    },
  };
  render(
    <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
      <AccountProvider
        currencies={createMemoryCurrencyGateway()}
        settings={createMemorySettingsGateway()}
      >
        <PaymentSheet gateway={gateway} onClose={vi.fn()} onSaved={onSaved} row={ROW} />
      </AccountProvider>
    </TodayProvider>,
  );
}

describe('Registrar abono', () => {
  it('dice de quien es y cuanto queda', () => {
    renderSheet();
    expect(screen.getByText('Ana · saldo $ 250.000')).toBeInTheDocument();
  });

  it('los chips rellenan el saldo entero y la mitad', async () => {
    renderSheet();
    const amount = screen.getByRole('textbox', { name: 'Importe' });

    await userEvent.click(screen.getByRole('button', { name: 'Todo el saldo' }));
    expect(amount).toHaveValue('250.000');

    await userEvent.click(screen.getByRole('button', { name: 'La mitad' }));
    expect(amount).toHaveValue('125.000');
  });

  it('sin importe no se puede guardar', () => {
    renderSheet();
    expect(screen.getByRole('button', { name: 'Guardar abono' })).toBeDisabled();
  });

  it('manda la fecha y los cuatro campos', async () => {
    const sent: PaymentDraft[] = [];
    const onSaved = vi.fn();
    renderSheet(sent, onSaved);
    await userEvent.click(screen.getByRole('button', { name: 'Todo el saldo' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar abono' }));

    expect(sent).toHaveLength(1);
    expect(sent[0]?.paidOn).toBe(SAMPLE_TODAY);
    expect(sent[0]?.money.currency).toBe('COP');
    expect(onSaved).toHaveBeenCalled();
  });
});
