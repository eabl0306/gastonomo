import type { ReactElement } from 'react';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import type { LoanGateway } from '../../application/ports/loan-gateway';
import type { LoanDirection } from '../../domain/ledger/loan';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { useAccount } from '../account/account-context';
import { Icon, Notice } from '../auth/auth-chrome';
import { useToday } from '../clock/today-context';
import { messageOf } from '../common/blocks';
import { AmountField } from '../forms/amount-field';
import { FormField, FormLayout, SaveButton, SegmentedTabs } from '../forms/form-chrome';

import { useNewLoan, type NewLoanState } from './use-new-loan';

const DIRECTION_TABS = [
  { value: 'lent' as LoanDirection, label: 'Presté' },
  { value: 'borrowed' as LoanDirection, label: 'Me prestaron' },
];

/**
 * Nuevo prestamo: direccion, monto con su moneda, persona, fecha y nota.
 *
 * El aviso de abajo no es un adorno: un prestamo **crea un movimiento** en el historico, y
 * quien no lo sepa vera un gasto que no recuerda haber registrado.
 */
export function NewLoanScreen({
  gateway,
  onClose,
  onSaved,
}: {
  gateway: LoanGateway;
  onClose: () => void;
  onSaved: () => void;
}): ReactElement {
  const today = useToday();
  const { settings, catalog, rates } = useAccount();
  const form = useNewLoan({
    gateway,
    display: settings.displayCurrency,
    rates,
    today,
    onSaved,
  });
  return (
    <FormLayout
      action={
        <SaveButton
          disabled={!form.canSave}
          label={form.saving ? 'Creando…' : 'Crear préstamo'}
          onClick={form.save}
        />
      }
      onClose={onClose}
      title="Nuevo préstamo"
    >
      <Body catalog={catalog} form={form} rates={rates} />
    </FormLayout>
  );
}

function Body({
  form,
  catalog,
  rates,
}: {
  form: NewLoanState;
  catalog: readonly CurrencyInfo[];
  rates: ExchangeRates;
}): ReactElement {
  return (
    <>
      <SegmentedTabs
        label="Dirección del préstamo"
        onSelect={form.setDirection}
        options={DIRECTION_TABS}
        selected={form.direction}
      />
      <AmountField
        caption="Monto total"
        catalog={catalog}
        onAmount={form.money.setAmount}
        onConversion={form.money.setConversion}
        onCurrency={form.money.setCurrency}
        rates={rates}
        state={form.money.state}
        tone={form.direction}
        view={form.money.view}
      />
      <Fields form={form} />
      <p className="g-hint">
        <Icon name="info" />
        <span>{hintOf(form.direction)}</span>
      </p>
      {form.error === null ? null : (
        <Notice icon="error" tone="error">
          {messageOf(form.error)}
        </Notice>
      )}
    </>
  );
}

/** Lo que pasa en el historico al crear el prestamo, segun la direccion. */
function hintOf(direction: LoanDirection): string {
  return direction === 'lent'
    ? 'Se registrará como gasto en tus movimientos. Cada abono que recibas contará como ingreso.'
    : 'Se registrará como ingreso en tus movimientos. Cada abono que pagues contará como gasto.';
}

function Fields({ form }: { form: NewLoanState }): ReactElement {
  return (
    <div className="g-form__fields">
      <FormField label={form.direction === 'lent' ? '¿A quién le prestaste?' : '¿Quién te prestó?'}>
        <input
          onChange={(event) => {
            form.setPerson(event.target.value);
          }}
          placeholder="Nombre de la persona"
          value={form.person}
        />
      </FormField>
      <FormField label="Fecha">
        <input
          onChange={(event) => {
            form.setDate(event.target.value);
          }}
          type="date"
          value={form.openedOn}
        />
      </FormField>
      <FormField hint="(opcional)" label="Nota">
        <input
          onChange={(event) => {
            form.setNote(event.target.value);
          }}
          placeholder="Ej. Teléfono"
          value={form.note}
        />
      </FormField>
    </div>
  );
}
