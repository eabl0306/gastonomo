import type { LoanRow } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import { EMPTY_RATES, type ExchangeRates } from '../../domain/money/exchange-rates';
import type { ApiError } from '../ports/api-error';
import type { CurrencyGateway } from '../ports/currency-gateway';
import type { LoanGateway } from '../ports/loan-gateway';

import { loadLoans } from './load-loans';

/** Lo que necesitan la lista de Prestamos y su detalle. */
export interface LoanPorts {
  readonly loans: LoanGateway;
  readonly currencies: CurrencyGateway;
}

/** El resultado de cargar la lista una vez. */
export interface LoanListData {
  readonly rows: readonly LoanRow[];
  readonly rates: ExchangeRates;
  readonly error: ApiError | null;
}

/**
 * Trae los prestamos con su resumen ya calculado.
 *
 * Las tasas van primero porque de ellas depende el saldo: un prestamo en otra moneda sin tasa
 * se convierte con la equivalencia guardada, que es la rama de reserva de la regla de cuatro
 * campos. Las pills filtran en local, asi que no se pide por direccion.
 */
export async function loadLoanList(ports: LoanPorts, display: CurrencyCode): Promise<LoanListData> {
  const rates = await ports.currencies.rates();
  const table = rates.ok ? rates.value : EMPTY_RATES;
  const rows = await loadLoans(ports.loans, display, table);
  return {
    rows: rows.ok ? rows.value : [],
    rates: table,
    error: rows.ok ? null : rows.error,
  };
}
