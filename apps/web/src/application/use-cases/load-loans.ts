import { summarize, type LoanRow } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { success, type Outcome } from '../ports/api-error';
import type { LoanGateway } from '../ports/loan-gateway';

/**
 * Los prestamos con su resumen ya calculado.
 *
 * El resumen es del cliente: la API no guarda saldo, ni estado, ni porcentaje. Se calcula
 * aqui y no en la pantalla para que Inicio, Prestamos y el detalle den el mismo numero.
 */
export async function loadLoans(
  gateway: LoanGateway,
  display: CurrencyCode,
  rates: ExchangeRates,
): Promise<Outcome<readonly LoanRow[]>> {
  const outcome = await gateway.list();
  if (!outcome.ok) return outcome;
  return success(outcome.value.map((loan) => ({ loan, summary: summarize(loan, display, rates) })));
}
