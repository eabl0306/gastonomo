import { success, type Outcome } from '../../application/ports/api-error';
import type { CategoryGateway } from '../../application/ports/category-gateway';
import type { Clock } from '../../application/ports/clock';
import type { CurrencyGateway } from '../../application/ports/currency-gateway';
import type { LoanGateway } from '../../application/ports/loan-gateway';
import type { MovementGateway } from '../../application/ports/movement-gateway';
import type { SettingsGateway } from '../../application/ports/settings-gateway';
import { DEFAULT_SETTINGS, type UserSettings } from '../../domain/account/settings';
import { isAfter, isBefore } from '../../domain/calendar/day';
import type { Loan } from '../../domain/ledger/loan';
import type { Movement } from '../../domain/ledger/movement';

import {
  SAMPLE_CATEGORIES,
  SAMPLE_CURRENCIES,
  SAMPLE_LOANS,
  SAMPLE_MOVEMENTS,
  SAMPLE_RATES,
  SAMPLE_TODAY,
} from './sample-ledger';

/** Un reloj parado. Con el reloj de verdad, un test solo pasaria el dia que se escribio. */
export function fixedClock(today: string = SAMPLE_TODAY): Clock {
  return { today: () => today };
}

function inPeriod(movement: Movement, from?: string, to?: string): boolean {
  if (from !== undefined && isBefore(movement.occurredOn, from)) return false;
  return !(to !== undefined && isAfter(movement.occurredOn, to));
}

/** Movimientos en memoria, ya ordenados de mas reciente a mas antiguo como hace la API. */
export function createMemoryMovementGateway(
  movements: readonly Movement[] = SAMPLE_MOVEMENTS,
): MovementGateway {
  return {
    list: (query) => {
      const matching = movements
        .filter((movement) => inPeriod(movement, query.from, query.to))
        .filter((movement) => query.kind === undefined || movement.kind === query.kind);
      const items = query.limit === undefined ? matching : matching.slice(0, query.limit);
      return Promise.resolve(success({ items, nextCursor: null }));
    },
    create: () => {
      const first = movements[0];
      if (first === undefined) throw new Error('No hay movimiento de muestra que devolver');
      return Promise.resolve(success(first));
    },
  };
}

export function createMemoryLoanGateway(loans: readonly Loan[] = SAMPLE_LOANS): LoanGateway {
  const find = (id: string): Outcome<Loan> => {
    const loan = loans.find((candidate) => candidate.id === id);
    return loan === undefined ? { ok: false, error: { kind: 'not-found' } } : success(loan);
  };
  return {
    list: (direction) =>
      Promise.resolve(
        success(
          direction === undefined ? loans : loans.filter((loan) => loan.direction === direction),
        ),
      ),
    byId: (id) => Promise.resolve(find(id)),
    create: () => Promise.resolve(find(loans[0]?.id ?? '')),
    addPayment: (loanId) => Promise.resolve(find(loanId)),
  };
}

export function createMemorySettingsGateway(
  initial: UserSettings = DEFAULT_SETTINGS,
): SettingsGateway {
  let current = initial;
  return {
    read: () => Promise.resolve(success(current)),
    update: (patch) => {
      current = {
        displayCurrency: patch.displayCurrency ?? current.displayCurrency,
        language: patch.language ?? current.language,
      };
      return Promise.resolve(success(current));
    },
  };
}

export function createMemoryCurrencyGateway(): CurrencyGateway {
  return {
    catalog: () => Promise.resolve(success(SAMPLE_CURRENCIES)),
    rates: () => Promise.resolve(success(SAMPLE_RATES)),
  };
}

export function createMemoryCategoryGateway(): CategoryGateway {
  const first = SAMPLE_CATEGORIES[0];
  if (first === undefined) throw new Error('No hay categorias de muestra');
  return {
    list: () => Promise.resolve(success(SAMPLE_CATEGORIES)),
    create: () => Promise.resolve(success(first)),
    update: () => Promise.resolve(success(first)),
    remove: () => Promise.resolve(success('archived' as const)),
    restore: () => Promise.resolve(success(first)),
  };
}
