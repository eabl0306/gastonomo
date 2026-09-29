import { periodFrom, periodTo, type Period } from '../../domain/calendar/period';
import type { Movement } from '../../domain/ledger/movement';
import { success, type Outcome } from '../ports/api-error';
import type { MovementGateway } from '../ports/movement-gateway';

/** Lo que se pide por pagina. Es el tope de la API. */
const PAGE_SIZE = 100;

/**
 * Cuantas paginas se piden como mucho.
 *
 * Un rango de un ano en una cuenta activa no cabe en una pagina, y de esta lista salen el
 * balance, la tendencia y los gastos por categoria: con una sola pagina, un periodo largo
 * ensenaria un balance que **parece** correcto y no lo es. El tope existe para que un
 * periodo absurdo no encadene cien peticiones, no para recortar un periodo normal.
 */
const MAX_PAGES = 20;

/**
 * Todos los movimientos del periodo, siguiendo el cursor.
 *
 * Devuelve lo que haya podido traer solo si no fallo nada: un fallo a mitad se propaga, en
 * vez de devolver media lista que daria un balance menor que el de verdad.
 */
export async function loadPeriodMovements(
  gateway: MovementGateway,
  period: Period,
): Promise<Outcome<readonly Movement[]>> {
  const from = periodFrom(period);
  const to = periodTo(period);
  const movements: Movement[] = [];
  let cursor: string | undefined;
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const outcome = await gateway.list({
      from,
      to,
      limit: PAGE_SIZE,
      ...(cursor === undefined ? {} : { cursor }),
    });
    if (!outcome.ok) return outcome;
    movements.push(...outcome.value.items);
    if (outcome.value.nextCursor === null) break;
    cursor = outcome.value.nextCursor;
  }
  return success(movements);
}

/** Cuantos movimientos ensena "Ultimos movimientos" en escritorio. En movil se cortan a 4. */
export const RECENT_LIMIT = 7;

/** Los mas recientes del periodo, que es una llamada aparte con su propio limite. */
export async function loadRecentMovements(
  gateway: MovementGateway,
  period: Period,
): Promise<Outcome<readonly Movement[]>> {
  const outcome = await gateway.list({
    from: periodFrom(period),
    to: periodTo(period),
    limit: RECENT_LIMIT,
  });
  return outcome.ok ? success(outcome.value.items) : outcome;
}
