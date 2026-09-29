import type { Day } from '../calendar/day';
import type { MovementKind } from '../ledger/category';
import type { LoanDirection } from '../ledger/loan';
import type { CurrencyCode } from '../money/currency';
import type { Decimal } from '../money/decimal';

/** La categoria que la API eligio. Siempre una que existe en la cuenta. */
interface DraftCategory {
  readonly id: string;
  readonly name: string;
}

/** Lo propio de un movimiento dictado. */
interface DictatedMovement {
  readonly kind: MovementKind;
  readonly category: DraftCategory;
  readonly note: string;
}

/** Lo propio de un prestamo dictado. */
interface DictatedLoan {
  readonly direction: LoanDirection;
  /** Vacio si no se entendio, y entonces `person` sale en `missing`. */
  readonly person: string;
  readonly note: string;
}

/**
 * El borrador que el usuario revisa antes de guardar.
 *
 * **No se ha creado nada.** Confirmarlo es una llamada aparte a `/v1/movements` o a
 * `/v1/loans`, con su clave de idempotencia: el historico es de solo anadir y no puede
 * depender de que un modelo acierte a la primera.
 */
export interface DictationDraft {
  readonly entry:
    | { readonly kind: 'movement'; readonly movement: DictatedMovement }
    | {
        readonly kind: 'loan';
        readonly loan: DictatedLoan;
      };
  /** Null si no se entendio el importe. Entonces `amount` sale en `missing`. */
  readonly amount: Decimal | null;
  readonly currency: CurrencyCode;
  readonly occurredOn: Day;
  /**
   * Lo que falta para poder guardar.
   *
   * Lo dice la API y no cada cliente: asi el movil y la web no pueden discrepar sobre cuando
   * un borrador esta completo.
   */
  readonly missing: readonly string[];
}

/** True si el borrador se puede guardar tal cual. */
export function isComplete(draft: DictationDraft): boolean {
  return draft.missing.length === 0;
}
