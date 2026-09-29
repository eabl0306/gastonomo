import { useCallback, useEffect, useState } from 'react';

import {
  loadLoanList,
  type LoanListData,
  type LoanPorts,
} from '../../application/use-cases/load-loan-list';
import { useAccount } from '../account/account-context';

interface Loaded {
  readonly key: string;
  readonly data: LoanListData;
}

export interface LoansState {
  readonly data: LoanListData | null;
  readonly loading: boolean;
  readonly everLoaded: boolean;
  readonly reload: () => void;
}

/**
 * Carga los prestamos.
 *
 * **No dependen del periodo**: son saldos de hoy, asi que cambiar de mes en Inicio o en
 * Movimientos no los toca. Lo que si los recarga es cambiar la moneda principal, porque el
 * saldo se convierte al pintar.
 */
export function useLoans(ports: LoanPorts): LoansState {
  const { settings, ready } = useAccount();
  const display = settings.displayCurrency;
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [attempt, setAttempt] = useState(0);
  const key = `${display}|${String(attempt)}`;

  useEffect(() => {
    if (!ready) return undefined;
    let live = true;
    void loadLoanList(ports, display).then((data) => {
      if (live) setLoaded({ key, data });
    });
    return () => {
      live = false;
    };
  }, [ports, display, ready, key]);

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
