import { useCallback, useState } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import type { LoanGateway } from '../../application/ports/loan-gateway';
import { monetaryAmountOf } from '../../application/state/money-field';
import type { LoanDirection } from '../../domain/ledger/loan';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { useIdempotencyKey } from '../forms/use-idempotency-key';
import { useMoneyField, type MoneyField } from '../forms/use-money-field';
import { useSave } from '../forms/use-save';

export interface NewLoanState {
  readonly direction: LoanDirection;
  readonly person: string;
  readonly openedOn: string;
  readonly note: string;
  readonly money: MoneyField;
  readonly saving: boolean;
  readonly error: ApiError | null;
  readonly canSave: boolean;
  setDirection: (direction: LoanDirection) => void;
  setPerson: (person: string) => void;
  setDate: (day: string) => void;
  setNote: (note: string) => void;
  save: () => void;
}

export interface NewLoanInput {
  readonly gateway: LoanGateway;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly today: string;
  readonly onSaved: () => void;
}

/**
 * El formulario de un prestamo.
 *
 * Hace falta el nombre de la persona: un prestamo sin persona no se puede cobrar ni devolver,
 * y la API lo rechazaria de todas formas.
 */
export function useNewLoan(input: NewLoanInput): NewLoanState {
  const money = useMoneyField(input.display, input.rates);
  const idempotencyKey = useIdempotencyKey();
  const [direction, setDirection] = useState<LoanDirection>('lent');
  const [person, setPerson] = useState('');
  const [openedOn, setDate] = useState(input.today);
  const [note, setNote] = useState('');
  const { saving, error, save: send } = useSave(input.onSaved);

  const canSave = money.view.valid && person.trim() !== '' && !saving;

  const save = useCallback(() => {
    if (!canSave) return;
    send(() =>
      input.gateway.create(
        {
          person: person.trim(),
          note,
          direction,
          openedOn,
          money: monetaryAmountOf(money.view, input.display),
        },
        idempotencyKey,
      ),
    );
  }, [canSave, send, input, person, note, direction, openedOn, money.view, idempotencyKey]);

  return {
    direction,
    person,
    openedOn,
    note,
    money,
    saving,
    error,
    canSave,
    setDirection,
    setPerson,
    setDate,
    setNote,
    save,
  };
}
