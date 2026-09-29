import type { MonetaryAmount } from '../money/monetary-amount';

import type { Category, MovementKind } from './category';

/**
 * Un ingreso o un gasto.
 *
 * La fecha es un dia sin hora ni zona, en `YYYY-MM-DD`: un gasto del 22 de septiembre es
 * del 22 en Bogota y en Madrid.
 */
export interface Movement {
  readonly id: string;
  readonly occurredOn: string;
  readonly kind: MovementKind;
  /** La nota del usuario o, si no la escribio, el nombre de la categoria. */
  readonly title: string;
  readonly money: MonetaryAmount;
  readonly category: Category;
  /** Lo rellena la API cuando el movimiento lo genero un prestamo. */
  readonly loanId: string | null;
  /** Lo rellena la API cuando lo genero un abono. */
  readonly loanPaymentId: string | null;
}
