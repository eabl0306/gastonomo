import { describe, expect, it } from 'vitest';

import { COP } from '../../domain/money/currency';
import { decimalToString } from '../../domain/money/decimal';
import { createMemoryLoanGateway } from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_RATES } from '../../infrastructure/memory/sample-ledger';
import type { LoanGateway } from '../ports/loan-gateway';

import { loadLoans } from './load-loans';

describe('cargar prestamos', () => {
  /** El saldo, el estado y el porcentaje los calcula el cliente: la API no los guarda. */
  it('adjunta el resumen de cada prestamo', async () => {
    const outcome = await loadLoans(createMemoryLoanGateway(), COP, SAMPLE_RATES);

    expect(outcome.ok).toBe(true);
    if (!outcome.ok) return;
    const [first] = outcome.value;
    expect(decimalToString(first?.summary.outstanding.amount ?? { units: 0n, scale: 0 })).toBe(
      '250000.0000',
    );
    expect(first?.summary.status).toBe('partial');
  });

  it('un fallo se propaga tal cual', async () => {
    const failing: LoanGateway = {
      list: () => Promise.resolve({ ok: false, error: { kind: 'network' } }),
      byId: () => Promise.reject(new Error('no se usa')),
      create: () => Promise.reject(new Error('no se usa')),
      addPayment: () => Promise.reject(new Error('no se usa')),
    };

    const outcome = await loadLoans(failing, COP, SAMPLE_RATES);
    expect(outcome.ok).toBe(false);
  });
});
