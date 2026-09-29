import type { ReactElement } from 'react';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import type { MovementKind } from '../../domain/ledger/category';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { useAccount } from '../account/account-context';
import { Notice } from '../auth/auth-chrome';
import { useToday } from '../clock/today-context';
import { messageOf } from '../common/blocks';
import { AmountField } from '../forms/amount-field';
import { CategoryGrid } from '../forms/category-grid';
import { FormField, FormLayout, SaveButton, SegmentedTabs } from '../forms/form-chrome';
import type { MovementPrefill } from '../forms/prefill';

import { useNewMovement, type NewMovementPorts, type NewMovementState } from './use-new-movement';

const KIND_TABS = [
  { value: 'in' as MovementKind, label: 'Ingreso', icon: 'south_west' },
  { value: 'out' as MovementKind, label: 'Gasto', icon: 'north_east' },
];

/**
 * Nuevo movimiento: tipo, importe con su moneda, categoria, fecha y nota.
 *
 * El boton de guardar esta apagado hasta que hay importe, equivalencia y categoria. Apagado y
 * no escondido: asi se ve que falta algo, que es lo que el diseno pide.
 */
export function NewMovementScreen({
  ports,
  onClose,
  onSaved,
  prefill,
}: {
  ports: NewMovementPorts;
  onClose: () => void;
  onSaved: (kind: MovementKind) => void;
  prefill?: MovementPrefill | undefined;
}): ReactElement {
  const today = useToday();
  const { settings, catalog, rates } = useAccount();
  const form = useNewMovement({
    ports,
    display: settings.displayCurrency,
    rates,
    today,
    onSaved,
    prefill,
  });
  return (
    <FormLayout
      action={
        <SaveButton
          disabled={!form.canSave}
          label={form.saving ? 'Guardando…' : 'Guardar movimiento'}
          onClick={form.save}
        />
      }
      onClose={onClose}
      title="Nuevo movimiento"
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
  form: NewMovementState;
  catalog: readonly CurrencyInfo[];
  rates: ExchangeRates;
}): ReactElement {
  return (
    <>
      <SegmentedTabs
        label="Tipo de movimiento"
        onSelect={form.setKind}
        options={KIND_TABS}
        selected={form.kind}
      />
      <AmountField
        catalog={catalog}
        onAmount={form.money.setAmount}
        onConversion={form.money.setConversion}
        onCurrency={form.money.setCurrency}
        rates={rates}
        state={form.money.state}
        tone={form.kind === 'in' ? 'income' : 'expense'}
        view={form.money.view}
      />
      <h2 className="g-form__section">Categoría</h2>
      <CategoryGrid
        categories={form.categories}
        kind={form.kind}
        onSelect={form.setCategory}
        selected={form.categoryId}
      />
      <Fields form={form} />
      {form.error === null ? null : (
        <Notice icon="error" tone="error">
          {messageOf(form.error)}
        </Notice>
      )}
    </>
  );
}

function Fields({ form }: { form: NewMovementState }): ReactElement {
  return (
    <div className="g-form__fields">
      <FormField label="Fecha">
        <input
          onChange={(event) => {
            form.setDate(event.target.value);
          }}
          type="date"
          value={form.occurredOn}
        />
      </FormField>
      <FormField hint="(opcional)" label="Nota">
        <input
          onChange={(event) => {
            form.setNote(event.target.value);
          }}
          placeholder="Ej. Mercado de la semana"
          value={form.note}
        />
      </FormField>
    </div>
  );
}
