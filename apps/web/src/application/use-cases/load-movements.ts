import type { Period } from '../../domain/calendar/period';
import type { Movement } from '../../domain/ledger/movement';
import type { ApiError } from '../ports/api-error';
import type { MovementGateway } from '../ports/movement-gateway';

import { loadPeriodMovements } from './load-period-movements';

/** Lo que necesita la lista de Movimientos. */
export interface MovementsPorts {
  readonly movements: MovementGateway;
}

/** El resultado de cargar la lista una vez. */
export interface MovementsData {
  readonly movements: readonly Movement[];
  readonly error: ApiError | null;
}

/**
 * Trae los movimientos del periodo.
 *
 * Se pide la lista **sin filtrar por tipo**, aunque haya una pill activa: el resumen de tres
 * columnas se calcula sobre el periodo entero, asi que pidiendo `kind=out` no se podria pintar
 * la columna de Ingresos. Las pills filtran en local.
 */
export async function loadMovements(ports: MovementsPorts, period: Period): Promise<MovementsData> {
  const movements = await loadPeriodMovements(ports.movements, period);
  return {
    movements: movements.ok ? movements.value : [],
    error: movements.ok ? null : movements.error,
  };
}
