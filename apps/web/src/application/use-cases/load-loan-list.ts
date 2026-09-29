import type { LoanRow } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import type { ApiError } from '../ports/api-error';
import type { LoanGateway } from '../ports/loan-gateway';

import { loadLoans } from './load-loans';

/** Lo que necesitan la lista de Prestamos y su detalle. */
export interface LoanPorts {
  readonly loans: LoanGateway;
}

/** El resultado de cargar la lista una vez. */
export interface LoanListData {
  readonly rows: readonly LoanRow[];
  readonly error: ApiError | null;
}

/**
 * Trae los prestamos con su resumen ya calculado.
 *
 * Un prestamo en otra moneda sin tasa se convierte con la equivalencia guardada, que es la
 * rama de reserva de la regla de cuatro campos. Las pills filtran en local, asi que no se
 * pide por direccion.
 */
export async function loadLoanList(
  ports: LoanPorts,
  display: CurrencyCode,
  rates: ExchangeRates,
): Promise<LoanListData> {
  const rows = await loadLoans(ports.loans, display, rates);
  return {
    rows: rows.ok ? rows.value : [],
    error: rows.ok ? null : rows.error,
  };
}
