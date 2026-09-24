import { useCallback, useEffect, useState } from 'react';

import { checkApiHealth, type HealthProbe } from '../../application/use-cases/check-api-health';

import { useHealthGateway } from './health-gateway-context';

export type HealthViewState =
  { readonly kind: 'loading' } | { readonly kind: 'settled'; readonly probe: HealthProbe };

export interface HealthReportHook {
  readonly state: HealthViewState;
  readonly refresh: () => void;
}

interface Attempted {
  readonly attempt: number;
  readonly probe: HealthProbe;
}

/**
 * Ejecuta el caso de uso y lo traduce a estado de pantalla. El intento se
 * guarda junto al resultado: asi un reintento vuelve a "cargando" sin tener
 * que escribir estado dentro del efecto.
 */
export function useHealthReport(): HealthReportHook {
  const gateway = useHealthGateway();
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<Attempted | null>(null);

  useEffect(() => {
    let active = true;
    void checkApiHealth(gateway).then((probe) => {
      if (active) setSettled({ attempt, probe });
    });
    return () => {
      active = false;
    };
  }, [gateway, attempt]);

  const refresh = useCallback(() => {
    setAttempt((current) => current + 1);
  }, []);

  const state: HealthViewState =
    settled !== null && settled.attempt === attempt
      ? { kind: 'settled', probe: settled.probe }
      : { kind: 'loading' };

  return { state, refresh };
}
