import { success } from '../../application/ports/api-error';
import type { DictationGateway } from '../../application/ports/dictation-gateway';
import type { DictationDraft } from '../../domain/dictation/draft';

import type { ApiClient } from './api-client';
import { readDraft } from './dictation-dto';
import { malformed } from './malformed';

/**
 * El dictado contra la API.
 *
 * **No lleva clave de idempotencia**: no crea nada, solo interpreta. Lo que crea es la llamada
 * de confirmar, que si la lleva.
 */
export function createHttpDictationGateway(client: ApiClient): DictationGateway {
  return {
    parse: async (text, today) => {
      const outcome = await client.request('dictation/parse', {
        method: 'POST',
        body: { text, today },
      });
      if (!outcome.ok) return outcome;
      const draft = readDraft(outcome.value.body);
      return draft === null ? malformed<DictationDraft>() : success(draft);
    },
  };
}
