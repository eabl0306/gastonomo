import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { MovementDraft } from '../../application/ports/movement-gateway';
import {
  createMemoryCategoryGateway,
  createMemoryCurrencyGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_MOVEMENTS, SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';
import { PeriodProvider } from '../period/period-context';

import { NewMovementScreen } from './new-movement-screen';
import type { NewMovementPorts } from './use-new-movement';

interface Sent {
  readonly draft: MovementDraft;
  readonly key: string;
}

function renderForm(sent: Sent[] = [], onSaved = vi.fn()): void {
  const currencies = createMemoryCurrencyGateway();
  const first = SAMPLE_MOVEMENTS[0]!;
  const ports: NewMovementPorts = {
    movements: {
      ...createMemoryMovementGateway(),
      create: (draft, key) => {
        sent.push({ draft, key });
        return Promise.resolve({ ok: true, value: first });
      },
    },
    categories: createMemoryCategoryGateway(),
  };
  render(
    <AccountProvider currencies={currencies} settings={createMemorySettingsGateway()}>
      <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
        <PeriodProvider>
          <NewMovementScreen onClose={vi.fn()} onSaved={onSaved} ports={ports} />
        </PeriodProvider>
      </TodayProvider>
    </AccountProvider>,
  );
}

async function type(amount: string): Promise<void> {
  await userEvent.type(screen.getByRole('textbox', { name: 'Importe' }), amount);
}

describe('Nuevo movimiento', () => {
  it('no se puede guardar sin importe ni categoria', async () => {
    renderForm();
    const save = await screen.findByRole('button', { name: 'Guardar movimiento' });
    expect(save).toBeDisabled();

    await type('45000');
    expect(save).toBeDisabled();

    await userEvent.click(await screen.findByRole('radio', { name: /Alimentación/u }));
    expect(save).toBeEnabled();
  });

  /** Las de gasto no valen para un ingreso: dejar una elegida guardaria algo que no se ve. */
  it('cambiar de tipo borra la categoria elegida', async () => {
    renderForm();
    await type('45000');
    await userEvent.click(await screen.findByRole('radio', { name: /Alimentación/u }));

    await userEvent.click(screen.getByRole('tab', { name: /Ingreso/u }));
    expect(screen.getByRole('button', { name: 'Guardar movimiento' })).toBeDisabled();
    expect(screen.queryByRole('radio', { name: /Alimentación/u })).not.toBeInTheDocument();
  });

  it('manda los cuatro campos y la clave de idempotencia', async () => {
    const sent: Sent[] = [];
    renderForm(sent);
    await type('45000');
    await userEvent.click(await screen.findByRole('radio', { name: /Alimentación/u }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar movimiento' }));

    expect(sent).toHaveLength(1);
    const { draft, key } = sent[0]!;
    expect(draft.kind).toBe('out');
    expect(draft.occurredOn).toBe(SAMPLE_TODAY);
    expect(draft.money.currency).toBe('COP');
    expect(draft.money.baseCurrency).toBe('COP');
    expect(key).not.toBe('');
  });

  it('el importe se pinta con la agrupacion del idioma', async () => {
    renderForm();
    await type('1250000');
    expect(screen.getByRole('textbox', { name: 'Importe' })).toHaveValue('1.250.000');
  });

  /** La caja solo sale cuando el registro no esta en la moneda principal. */
  it('en otra moneda aparece la caja de equivalencia', async () => {
    renderForm();
    await type('140');
    expect(screen.queryByText(/Equivale a/u)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /COP/u }));
    await userEvent.click(await screen.findByRole('button', { name: /US Dollar/u }));

    expect(screen.getByText('Equivale a (COP)')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Equivalencia en COP' })).toHaveValue('560.000');
    expect(screen.getByText(/Tasa sugerida: 1 USD = 4.000 COP/u)).toBeInTheDocument();
  });

  it('avisa al guardar con el tipo que se guardo', async () => {
    const onSaved = vi.fn();
    renderForm([], onSaved);
    await type('45000');
    await userEvent.click(await screen.findByRole('radio', { name: /Alimentación/u }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar movimiento' }));

    expect(onSaved).toHaveBeenCalledWith('out');
  });
});
