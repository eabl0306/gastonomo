import { describe, expect, it } from 'vitest';

import { monthPeriod } from '../../domain/calendar/period';
import type { Movement } from '../../domain/ledger/movement';
import { SAMPLE_MOVEMENTS } from '../../infrastructure/memory/sample-ledger';
import type { MovementGateway, MovementQuery } from '../ports/movement-gateway';

import { RECENT_LIMIT, loadPeriodMovements, loadRecentMovements } from './load-period-movements';

const PERIOD = monthPeriod('2026-09');

/** Un gateway que parte la lista en paginas de uno, para ver que se siguen los cursores. */
function paginated(items: readonly Movement[], calls: MovementQuery[]): MovementGateway {
  return {
    list: (query) => {
      calls.push(query);
      const start = query.cursor === undefined ? 0 : Number(query.cursor);
      const next = start + 1;
      return Promise.resolve({
        ok: true,
        value: {
          items: items.slice(start, next),
          nextCursor: next < items.length ? String(next) : null,
        },
      });
    },
    create: () => Promise.reject(new Error('no se usa')),
  };
}

describe('movimientos del periodo', () => {
  /**
   * Un periodo largo no cabe en una pagina, y de esta lista salen el balance, la tendencia
   * y los gastos por categoria: quedarse en la primera daria un balance que parece correcto.
   */
  it('sigue el cursor hasta el final', async () => {
    const calls: MovementQuery[] = [];
    const outcome = await loadPeriodMovements(paginated(SAMPLE_MOVEMENTS, calls), PERIOD);

    expect(outcome.ok && outcome.value).toHaveLength(SAMPLE_MOVEMENTS.length);
    expect(calls).toHaveLength(SAMPLE_MOVEMENTS.length);
    expect(calls[0]).toMatchObject({ from: '2026-09-01', to: '2026-09-30' });
    expect(calls[1]?.cursor).toBe('1');
  });

  /** Media lista daria un balance menor que el de verdad, y nada avisaria. */
  it('un fallo a mitad se propaga en vez de devolver media lista', async () => {
    let page = 0;
    const gateway: MovementGateway = {
      list: () => {
        page += 1;
        if (page === 1) {
          return Promise.resolve({ ok: true, value: { items: [], nextCursor: '1' } });
        }
        return Promise.resolve({ ok: false, error: { kind: 'network' } });
      },
      create: () => Promise.reject(new Error('no se usa')),
    };

    const outcome = await loadPeriodMovements(gateway, PERIOD);
    expect(outcome.ok).toBe(false);
  });

  it('los ultimos movimientos van en una sola llamada con su limite', async () => {
    const calls: MovementQuery[] = [];
    const outcome = await loadRecentMovements(paginated(SAMPLE_MOVEMENTS, calls), PERIOD);

    expect(calls).toHaveLength(1);
    expect(calls[0]?.limit).toBe(RECENT_LIMIT);
    expect(outcome.ok && outcome.value).toHaveLength(1);
  });
});
