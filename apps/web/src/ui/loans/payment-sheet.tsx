import { useState, type ReactElement } from 'react';

import type { LoanGateway } from '../../application/ports/loan-gateway';
import { monetaryAmountOf } from '../../application/state/money-field';
import type { LoanRow } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import { decimalOf, divideDecimals, type Decimal } from '../../domain/money/decimal';
import { convert } from '../../domain/money/exchange-rates';
import { useAccount } from '../account/account-context';
import { Notice } from '../auth/auth-chrome';
import { useToday } from '../clock/today-context';
import { messageOf } from '../common/blocks';
import { Sheet } from '../common/sheet';
import { amountToRaw } from '../format/amount-input';
import { useFormats } from '../format/formats-context';
import { AmountField } from '../forms/amount-field';
import { useIdempotencyKey } from '../forms/use-idempotency-key';
import { useMoneyField } from '../forms/use-money-field';
import { useSave } from '../forms/use-save';

/** Para el chip de "La mitad". */
const TWO = decimalOf('2');

export interface PaymentSheetProps {
  readonly gateway: LoanGateway;
  readonly row: LoanRow;
  readonly onClose: () => void;
  readonly onSaved: () => void;
}

/**
 * El sheet de abonar un prestamo.
 *
 * Los dos chips rellenan el importe **en la moneda elegida**, no en la principal: el saldo se
 * lleva en la moneda que se ensena, asi que abonar "todo el saldo" en otra moneda hay que
 * convertirlo o el importe seria de otra magnitud.
 */
export function PaymentSheet(props: PaymentSheetProps): ReactElement {
  const today = useToday();
  const { settings, catalog, rates } = useAccount();
  const formats = useFormats();
  const display = settings.displayCurrency;
  const money = useMoneyField(display, rates);
  const idempotencyKey = useIdempotencyKey();
  const [paidOn, setPaidOn] = useState(today);
  const { saving, error, save: send } = useSave(props.onSaved);
  const outstanding = props.row.summary.outstanding.amount;

  const canSave = money.view.valid && !saving;
  const submit = (): void => {
    if (!canSave) return;
    send(() =>
      props.gateway.addPayment(
        props.row.loan.id,
        { paidOn, label: '', money: monetaryAmountOf(money.view, display) },
        idempotencyKey,
      ),
    );
  };
  return (
    <Sheet
      onClose={props.onClose}
      subtitle={`${props.row.loan.person} · saldo ${formats.amountOf(props.row.summary.outstanding)}`}
      title="Registrar abono"
    >
      <AmountField
        catalog={catalog}
        onAmount={money.setAmount}
        onConversion={money.setConversion}
        onCurrency={money.setCurrency}
        rates={rates}
        state={money.state}
        tone="neutral"
        view={money.view}
      />
      <FillChips currency={money.state.currency} onFill={money.setAmount} total={outstanding} />
      <PaidOnField onChange={setPaidOn} value={paidOn} />
      {error === null ? null : (
        <Notice icon="error" tone="error">
          {messageOf(error)}
        </Notice>
      )}
      <button className="g-sheet__apply" disabled={!canSave} onClick={submit} type="button">
        {saving ? 'Guardando…' : 'Guardar abono'}
      </button>
    </Sheet>
  );
}

/**
 * Los dos chips que rellenan el importe.
 *
 * Rellenan **en la moneda elegida**: si el registro va en dolares, "todo el saldo" tiene que
 * ser el saldo convertido, no la cifra en pesos.
 */
function FillChips({
  total,
  currency,
  onFill,
}: {
  total: Decimal;
  currency: CurrencyCode;
  onFill: (raw: string) => void;
}): ReactElement {
  const formats = useFormats();
  const rates = useAccount().rates;
  const inCurrency = convert(rates, total, formats.displayCurrency, currency) ?? total;
  const half = divideDecimals(inCurrency, TWO) ?? inCurrency;
  return (
    <div className="g-payment__chips">
      <button
        className="g-chip"
        onClick={() => {
          onFill(amountToRaw(inCurrency, currency));
        }}
        type="button"
      >
        Todo el saldo
      </button>
      <button
        className="g-chip"
        onClick={() => {
          onFill(amountToRaw(half, currency));
        }}
        type="button"
      >
        La mitad
      </button>
    </div>
  );
}

function PaidOnField({
  value,
  onChange,
}: {
  value: string;
  onChange: (day: string) => void;
}): ReactElement {
  return (
    <label className="g-sheet__field">
      <span className="g-sheet__field-label">Fecha</span>
      <input
        onChange={(event) => {
          onChange(event.target.value);
        }}
        type="date"
        value={value}
      />
    </label>
  );
}
