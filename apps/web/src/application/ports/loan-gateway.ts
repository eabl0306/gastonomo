import type { Loan, LoanDirection } from '../../domain/ledger/loan';
import type { MonetaryAmount } from '../../domain/money/monetary-amount';

import type { Outcome } from './api-error';

/** Lo que se manda a `POST /v1/loans`. */
export interface LoanDraft {
  readonly person: string;
  readonly note: string;
  readonly direction: LoanDirection;
  readonly openedOn: string;
  readonly money: MonetaryAmount;
}

/** Lo que se manda a `POST /v1/loans/{id}/payments`. */
export interface PaymentDraft {
  readonly paidOn: string;
  /** El concepto, opcional. La pantalla lo ensena como "Abono - {concepto}". */
  readonly label: string;
  readonly money: MonetaryAmount;
}

export interface LoanGateway {
  /** Sin [direction], los dos sentidos. */
  list: (direction?: LoanDirection) => Promise<Outcome<readonly Loan[]>>;
  /** Un prestamo de otra cuenta responde 404, no 403. */
  byId: (id: string) => Promise<Outcome<Loan>>;
  create: (draft: LoanDraft, idempotencyKey: string) => Promise<Outcome<Loan>>;
  /**
   * Registra un abono y devuelve el **prestamo entero**, ya con el abono dentro.
   *
   * Es lo que responde la API, y tiene sentido: quien abona necesita el saldo nuevo para
   * repintar la tarjeta, y el abono suelto no lo dice.
   */
  addPayment: (
    loanId: string,
    draft: PaymentDraft,
    idempotencyKey: string,
  ) => Promise<Outcome<Loan>>;
}
