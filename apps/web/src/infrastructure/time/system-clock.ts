import type { Clock } from '../../application/ports/clock';
import type { Day } from '../../domain/calendar/day';

const ISO_DAY_LENGTH = 10;

/**
 * El dia de hoy **en la zona del navegador**, no en UTC.
 *
 * `toISOString()` daria el dia de UTC, y en Bogota eso significa que desde las 19:00 la
 * aplicacion ya estaria ensenando el mes siguiente. Se compone a mano con las partes locales.
 */
function todayOf(now: Date): Day {
  const year = now.getFullYear().toString().padStart(4, '0');
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const day = now.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`.slice(0, ISO_DAY_LENGTH);
}

export const systemClock: Clock = { today: () => todayOf(new Date()) };
