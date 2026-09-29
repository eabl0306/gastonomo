import type { Period } from '../../domain/calendar/period';
import type { Movement } from '../../domain/ledger/movement';
import { EMPTY_RATES, type ExchangeRates } from '../../domain/money/exchange-rates';
import type { ApiError } from '../ports/api-error';
import type { CurrencyGateway } from '../ports/currency-gateway';
import type { MovementGateway } from '../ports/movement-gateway';

import { loadPeriodMovements } from './load-period-movements';

/** Lo que necesita la lista de Movimientos. */
export interface MovementsPorts {
  readonly movements: MovementGateway;
  readonly currencies: CurrencyGateway;
}

/** El resultado de cargar la lista una vez. */
export interface MovementsData {
  readonly movements: readonly Movement[];
  readonly rates: ExchangeRates;
  readonly error: ApiError | null;
  readonly ratesUnavailable: boolean;
}

/**
 * Trae los movimientos del periodo y las tasas con las que se convierten.
 *
 * Se pide la lista **sin filtrar por tipo**, aunque haya una pill activa: el resumen de tres
 * columnas se calcula sobre el periodo entero, asi que pidiendo `kind=out` no se podria pintar
 * la columna de Ingresos. Las pills filtran en local.
 */
export async function loadMovements(ports: MovementsPorts, period: Period): Promise<MovementsData> {
  const [rates, movements] = await Promise.all([
    ports.currencies.rates(),
    loadPeriodMovements(ports.movements, period),
  ]);
  return {
    movements: movements.ok ? movements.value : [],
    rates: rates.ok ? rates.value : EMPTY_RATES,
    error: movements.ok ? null : movements.error,
    ratesUnavailable: !rates.ok,
  };
}
