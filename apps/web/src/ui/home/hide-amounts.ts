import { useCallback, useState } from 'react';

const KEY = 'gastonomo.hide-amounts';

function readStored(): boolean {
  try {
    return globalThis.localStorage.getItem(KEY) === 'true';
  } catch {
    return false;
  }
}

/**
 * El ojo que oculta los montos.
 *
 * Es del dispositivo y no de la cuenta: quien tapa las cifras lo hace porque tiene a alguien
 * al lado, no porque quiera tener el movil y el computador tapados a la vez. Por eso va en
 * `localStorage` y no en `PATCH /v1/settings`, igual que en Android.
 */
export function useHideAmounts(): { hidden: boolean; toggle: () => void } {
  const [hidden, setHidden] = useState(readStored);
  const toggle = useCallback(() => {
    setHidden((previous) => {
      const next = !previous;
      try {
        globalThis.localStorage.setItem(KEY, String(next));
      } catch {
        // Sin almacenamiento el ojo sigue funcionando; solo no sobrevive a la recarga.
      }
      return next;
    });
  }, []);
  return { hidden, toggle };
}
