import { useCallback, useEffect, useState } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import type { CategoryDraft, CategoryGateway } from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';

export interface CategoriesState {
  readonly categories: readonly Category[];
  readonly loading: boolean;
  readonly error: ApiError | null;
  readonly reload: () => void;
  readonly save: (draft: CategoryDraft, id: string | null) => Promise<boolean>;
  readonly remove: (id: string) => Promise<void>;
  readonly restore: (id: string) => Promise<void>;
}

/**
 * La lista de categorias, **con las archivadas**.
 *
 * Aqui si se ven: una archivada sigue usandose en los movimientos ya registrados, y esta es la
 * unica pantalla desde la que se puede devolver a la lista.
 */
export function useCategoriesScreen(gateway: CategoryGateway): CategoriesState {
  const [categories, setCategories] = useState<readonly Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    void gateway.list(true).then((outcome) => {
      if (!live) return;
      setLoading(false);
      if (outcome.ok) setCategories(outcome.value);
      else setError(outcome.error);
    });
    return () => {
      live = false;
    };
  }, [gateway, attempt]);

  const reload = useCallback(() => {
    setAttempt((previous) => previous + 1);
  }, []);

  const save = useCallback(
    async (draft: CategoryDraft, id: string | null): Promise<boolean> => {
      const outcome = id === null ? await gateway.create(draft) : await gateway.update(id, draft);
      if (outcome.ok) reload();
      else setError(outcome.error);
      return outcome.ok;
    },
    [gateway, reload],
  );

  const remove = useCallback(
    async (id: string) => {
      const outcome = await gateway.remove(id);
      if (outcome.ok) reload();
      else setError(outcome.error);
    },
    [gateway, reload],
  );

  const restore = useCallback(
    async (id: string) => {
      const outcome = await gateway.restore(id);
      if (outcome.ok) reload();
      else setError(outcome.error);
    },
    [gateway, reload],
  );

  return { categories, loading, error, reload, save, remove, restore };
}
