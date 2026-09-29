import type { DictationDraft } from '../../domain/dictation/draft';
import type { MovementKind } from '../../domain/ledger/category';
import type { LoanDirection } from '../../domain/ledger/loan';
import type { CurrencyCode } from '../../domain/money/currency';
import { amountToRaw } from '../format/amount-input';

/**
 * Con que valores arranca un formulario cuando no arranca vacio.
 *
 * Existe por el dictado: "Editar" abre el formulario **relleno** con lo que la API entendio, en
 * vez de obligar a teclearlo otra vez. El importe va en la representacion raw del campo, no
 * como [Decimal], porque es lo que el campo sabe pintar.
 */
export interface MoneyPrefill {
  readonly amount: string;
  readonly currency: CurrencyCode;
}

export interface MovementPrefill extends MoneyPrefill {
  readonly kind: MovementKind;
  readonly categoryId: string;
  readonly note: string;
  readonly occurredOn: string;
}

export interface LoanPrefill extends MoneyPrefill {
  readonly direction: LoanDirection;
  readonly person: string;
  readonly note: string;
  readonly openedOn: string;
}

/** Lo que se lleva en el estado de la navegacion al ir del dictado a un formulario. */
export interface PrefillState {
  readonly movement?: MovementPrefill;
  readonly loan?: LoanPrefill;
}

/**
 * Convierte el borrador en lo que el formulario necesita.
 *
 * Un borrador sin importe deja el campo vacio, no un cero: un cero parece tecleado y el boton de
 * guardar se quedaria apagado sin decir por que.
 */
export function prefillOf(draft: DictationDraft): PrefillState {
  const amount = draft.amount === null ? '' : amountToRaw(draft.amount, draft.currency);
  if (draft.entry.kind === 'movement') {
    const movement = draft.entry.movement;
    return {
      movement: {
        kind: movement.kind,
        categoryId: movement.category.id,
        note: movement.note,
        occurredOn: draft.occurredOn,
        amount,
        currency: draft.currency,
      },
    };
  }
  const loan = draft.entry.loan;
  return {
    loan: {
      direction: loan.direction,
      person: loan.person,
      note: loan.note,
      openedOn: draft.occurredOn,
      amount,
      currency: draft.currency,
    },
  };
}
