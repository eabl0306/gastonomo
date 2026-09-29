import { useCallback, useState } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import type { CategoryGateway } from '../../application/ports/category-gateway';
import type { MovementGateway } from '../../application/ports/movement-gateway';
import { monetaryAmountOf } from '../../application/state/money-field';
import type { Category, MovementKind } from '../../domain/ledger/category';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import type { MovementPrefill } from '../forms/prefill';
import { useCategories } from '../forms/use-categories';
import { useIdempotencyKey } from '../forms/use-idempotency-key';
import { useMoneyField, type MoneyField } from '../forms/use-money-field';
import { useSave } from '../forms/use-save';

export interface NewMovementPorts {
  readonly movements: MovementGateway;
  readonly categories: CategoryGateway;
}

export interface NewMovementState {
  readonly kind: MovementKind;
  readonly categories: readonly Category[];
  readonly categoryId: string | null;
  readonly occurredOn: string;
  readonly note: string;
  readonly money: MoneyField;
  readonly saving: boolean;
  readonly error: ApiError | null;
  readonly canSave: boolean;
  setKind: (kind: MovementKind) => void;
  setCategory: (id: string) => void;
  setDate: (day: string) => void;
  setNote: (note: string) => void;
  save: () => void;
}

export interface NewMovementInput {
  readonly ports: NewMovementPorts;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly today: string;
  readonly onSaved: (kind: MovementKind) => void;
  /** Con que arranca el formulario. Lo rellena "Editar" en el dictado. */
  readonly prefill?: MovementPrefill | undefined;
}

/**
 * El formulario de un movimiento.
 *
 * Cambiar de tipo **borra la categoria elegida**: las de gasto no valen para un ingreso, y
 * dejar seleccionada una que ya no esta en el grid es una forma silenciosa de guardar algo que
 * no se ve.
 */
export function useNewMovement(input: NewMovementInput): NewMovementState {
  const { prefill } = input;
  const start = startOf(prefill, input.today);
  const money = useMoneyField(input.display, input.rates, prefill);
  const idempotencyKey = useIdempotencyKey();
  const categories = useCategories(input.ports.categories);
  const [kind, setKindState] = useState<MovementKind>(start.kind);
  const [categoryId, setCategory] = useState<string | null>(start.categoryId);
  const [occurredOn, setDate] = useState(start.occurredOn);
  const [note, setNote] = useState(start.note);
  const { saving, error, save: send } = useSave(useSavedHandler(input.onSaved, kind));

  const setKind = useCallback((next: MovementKind) => {
    setKindState(next);
    setCategory(null);
  }, []);

  const canSave = money.view.valid && categoryId !== null && !saving;

  const save = useCallback(() => {
    if (categoryId === null || !canSave) return;
    send(() =>
      input.ports.movements.create(
        {
          occurredOn,
          kind,
          categoryId,
          note,
          money: monetaryAmountOf(money.view, input.display),
        },
        idempotencyKey,
      ),
    );
  }, [canSave, categoryId, send, input, occurredOn, kind, note, money.view, idempotencyKey]);

  return {
    kind,
    categories,
    categoryId,
    occurredOn,
    note,
    money,
    saving,
    error,
    canSave,
    setKind,
    setCategory,
    setDate,
    setNote,
    save,
  };
}

/** Con que arranca el formulario: vacio, o con lo que trajo el dictado. */
function startOf(
  prefill: MovementPrefill | undefined,
  today: string,
): { kind: MovementKind; categoryId: string | null; occurredOn: string; note: string } {
  if (prefill === undefined) {
    return { kind: 'out', categoryId: null, occurredOn: today, note: '' };
  }
  return {
    kind: prefill.kind,
    categoryId: prefill.categoryId,
    occurredOn: prefill.occurredOn,
    note: prefill.note,
  };
}

/** Avisa con el tipo que se guardo, que es lo que decide el mensaje de la lista. */
function useSavedHandler(onSaved: (kind: MovementKind) => void, kind: MovementKind): () => void {
  return useCallback(() => {
    onSaved(kind);
  }, [onSaved, kind]);
}
