import { success, type Outcome } from '../../application/ports/api-error';
import type { LoanDraft, LoanGateway, PaymentDraft } from '../../application/ports/loan-gateway';
import type { Loan, LoanDirection, LoanPayment } from '../../domain/ledger/loan';

import type { ApiClient } from './api-client';
import { readLoan, readLoanList, readPayment } from './ledger-dto';
import { malformed } from './malformed';
import { writeMoney } from './money-dto';

function loanFrom(body: unknown): Outcome<Loan> {
  const loan = readLoan(body);
  return loan === null ? malformed<Loan>() : success(loan);
}

/** Prestamos y abonos contra la API. */
export function createHttpLoanGateway(client: ApiClient): LoanGateway {
  return {
    list: async (direction?: LoanDirection) => {
      const outcome = await client.request('loans', { query: { direction } });
      if (!outcome.ok) return outcome;
      const loans = readLoanList(outcome.value.body);
      return loans === null ? malformed<readonly Loan[]>() : success(loans);
    },

    byId: async (id: string) => {
      const outcome = await client.request(`loans/${encodeURIComponent(id)}`);
      return outcome.ok ? loanFrom(outcome.value.body) : outcome;
    },

    create: async (draft: LoanDraft, idempotencyKey: string) => {
      const outcome = await client.request('loans', {
        method: 'POST',
        idempotencyKey,
        body: {
          person: draft.person,
          note: draft.note,
          direction: draft.direction,
          openedOn: draft.openedOn,
          ...writeMoney(draft.money),
        },
      });
      return outcome.ok ? loanFrom(outcome.value.body) : outcome;
    },

    addPayment: async (loanId: string, draft: PaymentDraft, idempotencyKey: string) => {
      const outcome = await client.request(`loans/${encodeURIComponent(loanId)}/payments`, {
        method: 'POST',
        idempotencyKey,
        body: {
          paidOn: draft.paidOn,
          ...(draft.label === '' ? {} : { label: draft.label }),
          ...writeMoney(draft.money),
        },
      });
      if (!outcome.ok) return outcome;
      const payment = readPayment(outcome.value.body);
      return payment === null ? malformed<LoanPayment>() : success(payment);
    },
  };
}
