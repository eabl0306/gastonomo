import { describe, expect, it } from 'vitest';

import type { DictationDraft } from '../../domain/dictation/draft';
import { COP, USD } from '../../domain/money/currency';
import { decimalOf } from '../../domain/money/decimal';

import { prefillOf } from './prefill';

const MOVEMENT: DictationDraft = {
  entry: {
    kind: 'movement',
    movement: { kind: 'out', category: { id: 'c1', name: 'Alimentación' }, note: 'Almuerzo' },
  },
  amount: decimalOf('45000.0000'),
  currency: COP,
  occurredOn: '2026-09-22',
  missing: [],
};

const LOAN: DictationDraft = {
  entry: { kind: 'loan', loan: { direction: 'lent', person: 'Rosa', note: 'Viaje' } },
  amount: decimalOf('300000.0000'),
  currency: COP,
  occurredOn: '2026-09-20',
  missing: [],
};

describe('rellenar un formulario desde un borrador', () => {
  it('un movimiento lleva tipo, categoria, nota y fecha', () => {
    const prefill = prefillOf(MOVEMENT);

    expect(prefill.loan).toBeUndefined();
    expect(prefill.movement).toEqual({
      kind: 'out',
      categoryId: 'c1',
      note: 'Almuerzo',
      occurredOn: '2026-09-22',
      amount: '45000',
      currency: 'COP',
    });
  });

  it('un prestamo lleva direccion, persona, nota y fecha', () => {
    const prefill = prefillOf(LOAN);

    expect(prefill.movement).toBeUndefined();
    expect(prefill.loan).toEqual({
      direction: 'lent',
      person: 'Rosa',
      note: 'Viaje',
      openedOn: '2026-09-20',
      amount: '300000',
      currency: 'COP',
    });
  });

  /** Un cero parece tecleado, y el boton de guardar se quedaria apagado sin decir por que. */
  it('sin importe el campo arranca vacio, no en cero', () => {
    const prefill = prefillOf({ ...MOVEMENT, amount: null, missing: ['amount'] });
    expect(prefill.movement?.amount).toBe('');
  });

  /** El raw se recorta a los decimales de SU moneda, no a los de la principal. */
  it('el importe sale en la representacion del campo, con los decimales de su moneda', () => {
    const prefill = prefillOf({ ...MOVEMENT, amount: decimalOf('140.5000'), currency: USD });

    expect(prefill.movement?.amount).toBe('140.5');
    expect(prefill.movement?.currency).toBe('USD');
  });
});
