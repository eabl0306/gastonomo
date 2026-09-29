import { useCallback } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import type { LoanGateway } from '../../application/ports/loan-gateway';
import type { MovementGateway } from '../../application/ports/movement-gateway';
import type { DictationDraft } from '../../domain/dictation/draft';
import type { CurrencyCode } from '../../domain/money/currency';
import { ZERO } from '../../domain/money/decimal';
import { convert, type ExchangeRates } from '../../domain/money/exchange-rates';
import { amountOf, singleAmount, type MonetaryAmount } from '../../domain/money/monetary-amount';
import { moneyOf, roundedMoney } from '../../domain/money/money';
import { useIdempotencyKey } from '../forms/use-idempotency-key';
import { useSave } from '../forms/use-save';

export interface ConfirmPorts {
  readonly movements: MovementGateway;
  readonly loans: LoanGateway;
}

export interface ConfirmDraft {
  readonly saving: boolean;
  readonly error: ApiError | null;
  readonly confirm: (draft: DictationDraft) => void;
}

export interface ConfirmInputs {
  readonly ports: ConfirmPorts;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly onSaved: () => void;
}

/**
 * Guarda el borrador.
 *
 * **El dictado no crea nada**: esto es una llamada aparte a `/v1/movements` o a `/v1/loans`,
 * con su clave de idempotencia. El historico es de solo anadir y no puede depender de que un
 * modelo acierte a la primera.
 */
export function useConfirmDraft({ ports, display, rates, onSaved }: ConfirmInputs): ConfirmDraft {
  const idempotencyKey = useIdempotencyKey();
  const { saving, error, save } = useSave(onSaved);

  const confirm = useCallback(
    (draft: DictationDraft) => {
      const money = moneyOfDraft(draft, display, rates);
      if (draft.entry.kind === 'movement') {
        const movement = draft.entry.movement;
        save(() =>
          ports.movements.create(
            {
              occurredOn: draft.occurredOn,
              kind: movement.kind,
              categoryId: movement.category.id,
              note: movement.note,
              money,
            },
            idempotencyKey,
          ),
        );
        return;
      }
      const loan = draft.entry.loan;
      save(() =>
        ports.loans.create(
          {
            person: loan.person,
            note: loan.note,
            direction: loan.direction,
            openedOn: draft.occurredOn,
            money,
          },
          idempotencyKey,
        ),
      );
    },
    [ports, display, rates, save, idempotencyKey],
  );

  return { saving, error, confirm };
}

/**
 * Los cuatro campos del borrador.
 *
 * La equivalencia se calcula aqui con las tasas de hoy: la API manda el importe y su moneda,
 * pero no la equivalencia, porque convertir para mostrar y para guardar es del cliente.
 */
function moneyOfDraft(
  draft: DictationDraft,
  display: CurrencyCode,
  rates: ExchangeRates,
): MonetaryAmount {
  const amount = draft.amount ?? ZERO;
  if (draft.currency === display) return singleAmount(amount, display);
  const base = convert(rates, amount, draft.currency, display) ?? amount;
  return amountOf(moneyOf(amount, draft.currency), roundedMoney(moneyOf(base, display)));
}
