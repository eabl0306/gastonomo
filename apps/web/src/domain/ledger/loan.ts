import type { MonetaryAmount } from '../money/monetary-amount';

/** Preste o me prestaron. Es el valor de `direction` en la API y el de las pills. */
export type LoanDirection = 'lent' | 'borrowed';

/**
 * Un abono de un prestamo.
 *
 * `label` es el concepto opcional, que la pantalla ensena como "Abono - {concepto}".
 */
export interface LoanPayment {
  readonly id: string;
  readonly loanId: string;
  readonly paidOn: string;
  readonly label: string | null;
  readonly money: MonetaryAmount;
}

/**
 * Un prestamo con sus abonos dentro.
 *
 * Vienen embebidos porque la tarjeta ya necesita el saldo, y el saldo es el prestamo menos
 * la suma de los abonos con las tasas de hoy.
 */
export interface Loan {
  readonly id: string;
  readonly person: string;
  readonly note: string;
  readonly direction: LoanDirection;
  readonly openedOn: string;
  readonly money: MonetaryAmount;
  readonly payments: readonly LoanPayment[];
}
