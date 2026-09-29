import { success } from '../../application/ports/api-error';
import type {
  MovementDraft,
  MovementGateway,
  MovementPage,
  MovementQuery,
} from '../../application/ports/movement-gateway';
import type { Movement } from '../../domain/ledger/movement';

import type { ApiClient } from './api-client';
import { readMovement, readMovementPage } from './ledger-dto';
import { malformed } from './malformed';
import { writeMoney } from './money-dto';

/** Movimientos contra la API. */
export function createHttpMovementGateway(client: ApiClient): MovementGateway {
  return {
    list: async (query: MovementQuery) => {
      const outcome = await client.request('movements', {
        query: {
          from: query.from,
          to: query.to,
          kind: query.kind,
          cursor: query.cursor,
          limit: query.limit,
        },
      });
      if (!outcome.ok) return outcome;
      const page = readMovementPage(outcome.value.body);
      return page === null ? malformed<MovementPage>() : success(page);
    },

    create: async (draft: MovementDraft, idempotencyKey: string) => {
      const outcome = await client.request('movements', {
        method: 'POST',
        idempotencyKey,
        body: {
          occurredOn: draft.occurredOn,
          kind: draft.kind,
          categoryId: draft.categoryId,
          note: draft.note,
          ...writeMoney(draft.money),
        },
      });
      if (!outcome.ok) return outcome;
      const movement = readMovement(outcome.value.body);
      return movement === null ? malformed<Movement>() : success(movement);
    },
  };
}
