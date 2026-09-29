import { useCallback, useEffect, useState } from 'react';

import { loadHome, type HomeData, type HomePorts } from '../../application/use-cases/load-home';
import { periodFrom, periodTo, type Period } from '../../domain/calendar/period';
import { useAccount } from '../account/account-context';
import { usePeriod } from '../period/period-context';

/** Lo que la pantalla necesita saber, mas la bandera que evita el parpadeo al recargar. */
export interface HomeState {
  readonly data: HomeData | null;
  readonly loading: boolean;
  /**
   * Si alguna vez llego contenido.
   *
   * Separa "todavia no hay nada que ensenar" de "estoy recargando lo que ya se ve". Sin esto,
   * cambiar de mes cambia la pantalla entera por el bloque de carga y la devuelve.
   */
  readonly everLoaded: boolean;
  readonly reload: () => void;
}

/** Lo ultimo que llego, con la peticion de la que salio. */
interface Loaded {
  readonly key: string;
  readonly data: HomeData;
}

/**
 * Carga Inicio y la recarga cuando cambia el periodo, la moneda o cuando se pide a mano.
 *
 * `loading` **se deriva** de comparar la peticion en curso con la de lo ultimo que llego, en
 * vez de guardarse en su propio estado. Asi el efecto no toca el estado salvo para guardar la
 * respuesta, que es lo unico que de verdad viene de fuera.
 */
export function useHome(ports: HomePorts): HomeState {
  const { state, today } = usePeriod();
  const { settings, ready } = useAccount();
  const period: Period = state.selection.period;
  const display = settings.displayCurrency;
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [attempt, setAttempt] = useState(0);
  const key = keyOf(period, today, display, attempt);

  useEffect(() => {
    if (!ready) return undefined;
    let live = true;
    void loadHome(ports, { period, today, display }).then((data) => {
      // Una respuesta que llega tarde no puede pisar a la del periodo que se esta viendo.
      if (live) setLoaded({ key, data });
    });
    return () => {
      live = false;
    };
  }, [ports, period, today, display, ready, key]);

  const reload = useCallback(() => {
    setAttempt((previous) => previous + 1);
  }, []);

  return {
    // Se devuelve lo anterior mientras llega lo nuevo: es lo que deja la pantalla puesta.
    data: loaded?.data ?? null,
    loading: loaded?.key !== key,
    everLoaded: loaded?.data.content != null,
    reload,
  };
}

function keyOf(period: Period, today: string, display: string, attempt: number): string {
  return [periodFrom(period), periodTo(period), today, display, attempt].join('|');
}
