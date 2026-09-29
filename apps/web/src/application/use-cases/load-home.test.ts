import { describe, expect, it } from 'vitest';

import { DEFAULT_SETTINGS } from '../../domain/account/settings';
import { monthPeriod } from '../../domain/calendar/period';
import { decimalToString } from '../../domain/money/decimal';
import { createMemoryAuthGateway } from '../../infrastructure/memory/in-memory-auth';
import {
  createMemoryLoanGateway,
  createMemoryMovementGateway,
} from '../../infrastructure/memory/in-memory-ledger';
import {
  SAMPLE_MOVEMENTS,
  SAMPLE_RATES,
  SAMPLE_TODAY,
} from '../../infrastructure/memory/sample-ledger';
import type { ApiError } from '../ports/api-error';
import type { LoanGateway } from '../ports/loan-gateway';
import type { MovementGateway } from '../ports/movement-gateway';

import { loadHome, type HomePorts } from './load-home';

const PERIOD = monthPeriod('2026-09');
const NETWORK: ApiError = { kind: 'network' };

function ports(overrides: Partial<HomePorts> = {}): HomePorts {
  return {
    movements: createMemoryMovementGateway(),
    loans: createMemoryLoanGateway(),
    auth: createMemoryAuthGateway(),
    ...overrides,
  };
}

function request(): Parameters<typeof loadHome>[1] {
  return {
    period: PERIOD,
    today: SAMPLE_TODAY,
    display: DEFAULT_SETTINGS.displayCurrency,
    rates: SAMPLE_RATES,
  };
}

const FAILING_MOVEMENTS: MovementGateway = {
  list: () => Promise.resolve({ ok: false, error: NETWORK }),
  create: () => Promise.resolve({ ok: false, error: NETWORK }),
};

const FAILING_LOANS: LoanGateway = {
  list: () => Promise.resolve({ ok: false, error: NETWORK }),
  byId: () => Promise.resolve({ ok: false, error: NETWORK }),
  create: () => Promise.resolve({ ok: false, error: NETWORK }),
  addPayment: () => Promise.resolve({ ok: false, error: NETWORK }),
};

describe('cargar Inicio', () => {
  it('resume el periodo, la tendencia, los gastos y los prestamos', async () => {
    const data = await loadHome(ports(), request());

    // 560.000 de ingreso menos 236.900 de gastos.
    expect(decimalToString(data.content?.summary.net.amount ?? { units: 0n, scale: 0 })).toBe(
      '323100.0000',
    );
    expect(data.content?.breakdown.slices).toHaveLength(2);
    expect(data.content?.recent).toHaveLength(SAMPLE_MOVEMENTS.length);
    expect(data.content?.owedToMe.openCount).toBe(1);
    expect(data.content?.iOwe.openCount).toBe(1);
    expect(data.error).toBeNull();
  });

  /** De la lista del periodo salen balance, tendencia, categorias y ultimos movimientos. */
  it('si falla la lista del periodo, la pantalla no tiene nada que ensenar', async () => {
    const data = await loadHome(ports({ movements: FAILING_MOVEMENTS }), request());

    expect(data.content).toBeNull();
    expect(data.error).toEqual(NETWORK);
  });

  it('sin prestamos las dos tarjetas quedan a cero y el resto sigue', async () => {
    const data = await loadHome(ports({ loans: FAILING_LOANS }), request());

    expect(data.content?.owedToMe.openCount).toBe(0);
    expect(data.content?.summary.income.amount.units).toBeGreaterThan(0n);
  });

  it('los prestamos no dependen del periodo: son saldos de hoy', async () => {
    const outside = await loadHome(ports(), {
      period: monthPeriod('2026-01'),
      today: SAMPLE_TODAY,
      display: DEFAULT_SETTINGS.displayCurrency,
      rates: SAMPLE_RATES,
    });

    // Uno de los tres esta pagado, asi que no cuenta: la tarjeta solo suma los abiertos.
    expect(outside.content?.recent).toHaveLength(0);
    expect(outside.content?.owedToMe.openCount).toBe(1);
  });
});
