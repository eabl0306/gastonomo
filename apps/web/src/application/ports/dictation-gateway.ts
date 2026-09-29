import type { Day } from '../../domain/calendar/day';
import type { DictationDraft } from '../../domain/dictation/draft';

import type { Outcome } from './api-error';

/**
 * Convierte una frase dictada en un borrador.
 *
 * Solo lo pueden usar las cuentas aprobadas: al resto la API le responde 404, no 403, para no
 * confirmar que el endpoint existe. Aqui eso llega como `not-found`.
 */
export interface DictationGateway {
  parse: (text: string, today: Day) => Promise<Outcome<DictationDraft>>;
}
