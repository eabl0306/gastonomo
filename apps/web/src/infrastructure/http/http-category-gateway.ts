import { success, type Outcome } from '../../application/ports/api-error';
import type {
  CategoryDraft,
  CategoryGateway,
  RemovalOutcome,
} from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';

import type { ApiClient } from './api-client';
import { readCategoryBody, readCategoryList } from './ledger-dto';
import { malformed } from './malformed';

function categoryFrom(body: unknown): Outcome<Category> {
  const category = readCategoryBody(body);
  return category === null ? malformed<Category>() : success(category);
}

/**
 * Lee el `outcome` del borrado.
 *
 * Cualquier cosa que no sea `deleted` se lee como archivada, que es el caso conservador: si
 * la respuesta cambiara, la pantalla diria que la categoria sigue ahi en vez de dar por
 * destruido algo que no lo esta.
 */
function removalFrom(body: unknown): RemovalOutcome {
  const outcome = (body as { outcome?: unknown } | null)?.outcome;
  return outcome === 'deleted' ? 'deleted' : 'archived';
}

/** Categorias contra la API. */
export function createHttpCategoryGateway(client: ApiClient): CategoryGateway {
  return {
    list: async (includeArchived: boolean) => {
      const outcome = await client.request('categories', {
        query: { includeArchived: includeArchived ? 'true' : undefined },
      });
      if (!outcome.ok) return outcome;
      const categories = readCategoryList(outcome.value.body);
      return categories === null ? malformed<readonly Category[]>() : success(categories);
    },

    create: async (draft: CategoryDraft) => {
      const outcome = await client.request('categories', { method: 'POST', body: draft });
      return outcome.ok ? categoryFrom(outcome.value.body) : outcome;
    },

    update: async (id: string, draft: Partial<CategoryDraft>) => {
      const outcome = await client.request(`categories/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: draft,
      });
      return outcome.ok ? categoryFrom(outcome.value.body) : outcome;
    },

    remove: async (id: string) => {
      const outcome = await client.request(`categories/${encodeURIComponent(id)}`, {
        method: 'DELETE',
      });
      return outcome.ok ? success(removalFrom(outcome.value.body)) : outcome;
    },

    restore: async (id: string) => {
      const outcome = await client.request(`categories/${encodeURIComponent(id)}/restore`, {
        method: 'POST',
      });
      return outcome.ok ? categoryFrom(outcome.value.body) : outcome;
    },
  };
}
