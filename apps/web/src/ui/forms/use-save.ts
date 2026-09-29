import { useCallback, useState } from 'react';

import type { ApiError, Outcome } from '../../application/ports/api-error';

export interface SaveState<T> {
  readonly saving: boolean;
  readonly error: ApiError | null;
  /** Manda el alta. Un segundo clic mientras esta en vuelo no hace nada. */
  readonly save: (send: () => Promise<Outcome<T>>) => void;
}

/**
 * El estado de un alta.
 *
 * Bloquear el segundo clic no es cosmetico: el historico no se puede borrar, asi que dos
 * llamadas en vuelo con la misma clave son, en el mejor caso, un 409 y, sin clave, un gasto
 * duplicado para siempre.
 */
export function useSave<T>(onSaved: (value: T) => void): SaveState<T> {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const save = useCallback(
    (send: () => Promise<Outcome<T>>) => {
      if (saving) return;
      setSaving(true);
      setError(null);
      void send().then((outcome) => {
        setSaving(false);
        if (outcome.ok) onSaved(outcome.value);
        else setError(outcome.error);
      });
    },
    [saving, onSaved],
  );
  return { saving, error, save };
}
