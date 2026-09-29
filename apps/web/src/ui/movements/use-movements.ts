import { useCallback, useEffect, useState } from 'react';

import {
  loadMovements,
  type MovementsData,
  type MovementsPorts,
} from '../../application/use-cases/load-movements';
import { periodFrom, periodTo, type Period } from '../../domain/calendar/period';
import { useAccount } from '../account/account-context';
import { usePeriod } from '../period/period-context';

interface Loaded {
  readonly key: string;
  readonly data: MovementsData;
}

export interface MovementsState {
  readonly data: MovementsData | null;
  readonly loading: boolean;
  /** Si alguna vez llego la lista. Es lo que evita que cambiar de mes parpadee. */
  readonly everLoaded: boolean;
  readonly reload: () => void;
}

/**
 * Carga la lista del periodo.
 *
 * No depende de la pill activa: la lista se pide entera y las pills filtran en local, porque
 * el resumen de tres columnas se calcula sobre el periodo sin filtrar.
 */
export function useMovements(ports: MovementsPorts): MovementsState {
  const { state } = usePeriod();
  const { ready } = useAccount();
  const period: Period = state.selection.period;
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [attempt, setAttempt] = useState(0);
  const key = [periodFrom(period), periodTo(period), attempt].join('|');

  useEffect(() => {
    if (!ready) return undefined;
    let live = true;
    void loadMovements(ports, period).then((data) => {
      if (live) setLoaded({ key, data });
    });
    return () => {
      live = false;
    };
  }, [ports, period, ready, key]);

  const reload = useCallback(() => {
    setAttempt((previous) => previous + 1);
  }, []);

  return {
    data: loaded?.data ?? null,
    loading: loaded?.key !== key,
    everLoaded: loaded !== null && loaded.data.error === null,
    reload,
  };
}
