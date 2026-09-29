import type { User } from '../../domain/account/user';
import { balanceTrend, type BalanceTrend } from '../../domain/analysis/balance-trend';
import {
  breakdownOfExpenses,
  type CategoryBreakdown,
} from '../../domain/analysis/category-breakdown';
import { summarizePeriod, type PeriodSummary } from '../../domain/analysis/period-summary';
import type { Day } from '../../domain/calendar/day';
import type { Period } from '../../domain/calendar/period';
import { loanTotals, type LoanRow, type LoanTotals } from '../../domain/ledger/loan-summary';
import type { Movement } from '../../domain/ledger/movement';
import type { CurrencyCode } from '../../domain/money/currency';
import { EMPTY_RATES, type ExchangeRates } from '../../domain/money/exchange-rates';
import type { ApiError, Outcome } from '../ports/api-error';
import type { AuthGateway } from '../ports/auth-gateway';
import type { CurrencyGateway } from '../ports/currency-gateway';
import type { LoanGateway } from '../ports/loan-gateway';
import type { MovementGateway } from '../ports/movement-gateway';

import { loadLoans } from './load-loans';
import { loadPeriodMovements, loadRecentMovements } from './load-period-movements';

/** Los gateways que hacen falta para llenar Inicio. */
export interface HomePorts {
  readonly movements: MovementGateway;
  readonly loans: LoanGateway;
  readonly currencies: CurrencyGateway;
  readonly auth: AuthGateway;
}

/** Lo que se pinta debajo del filtro cuando la carga salio bien. */
export interface HomeContent {
  readonly summary: PeriodSummary;
  readonly trend: BalanceTrend;
  readonly breakdown: CategoryBreakdown;
  readonly recent: readonly Movement[];
  readonly owedToMe: LoanTotals;
  readonly iOwe: LoanTotals;
}

/** El resultado de cargar Inicio una vez: lo que se pudo traer y lo que fallo. */
export interface HomeData {
  readonly rates: ExchangeRates;
  readonly user: User | null;
  /** Null solo cuando fallo la lista del periodo, que es lo unico que rompe la pantalla. */
  readonly content: HomeContent | null;
  readonly error: ApiError | null;
  /**
   * `GET /v1/exchange-rates` respondio 503 porque la tarea de refresco todavia no consiguio
   * una pasada. No es un fallo de la pantalla: lo que esta en la moneda principal se ensena
   * igual y se avisa con una linea.
   */
  readonly ratesUnavailable: boolean;
}

function valueOr<T>(outcome: Outcome<T>, fallback: T): T {
  return outcome.ok ? outcome.value : fallback;
}

/** Lo que hace falta para una carga: el periodo, el dia de hoy y la moneda principal. */
export interface HomeRequest {
  readonly period: Period;
  readonly today: Day;
  /**
   * La moneda con la que se convierte todo. Llega ya cargada y no se pide aqui.
   *
   * Es el codigo y no los ajustes enteros a proposito: el idioma no cambia lo que se pide, y
   * pasar el objeto hacia recargar Inicio cada vez que llegaba una copia nueva de los ajustes.
   */
  readonly display: CurrencyCode;
}

/**
 * Trae de la API todo lo que ensena Inicio y lo convierte en lo que pinta la pantalla.
 *
 * Son cinco llamadas: cuatro en paralelo y la de prestamos despues, porque necesita las
 * tasas para calcular los saldos.
 *
 * **Lo unico que rompe la pantalla es que falle la lista de movimientos del periodo**: de
 * ella salen el balance, la tendencia, los gastos por categoria y los ultimos movimientos.
 * Si solo fallan el perfil, los prestamos o las tasas, se ensena lo que hay.
 */
export async function loadHome(ports: HomePorts, request: HomeRequest): Promise<HomeData> {
  const { period, today, display } = request;
  const [rates, movements, recent, profile] = await Promise.all([
    ports.currencies.rates(),
    loadPeriodMovements(ports.movements, period),
    loadRecentMovements(ports.movements, period),
    ports.auth.profile(),
  ]);
  const table = valueOr(rates, EMPTY_RATES);
  const head = {
    rates: table,
    user: valueOr<User | null>(profile, null),
    ratesUnavailable: !rates.ok,
  };
  if (!movements.ok) return { ...head, content: null, error: movements.error };
  const loans = await loadLoans(ports.loans, display, table);
  return {
    ...head,
    content: contentOf({
      movements: movements.value,
      recent: valueOr<readonly Movement[]>(recent, []),
      rows: valueOr(loans, []),
      display,
      rates: table,
      period,
      today,
    }),
    error: null,
  };
}

interface ContentInput {
  readonly movements: readonly Movement[];
  readonly recent: readonly Movement[];
  readonly rows: readonly LoanRow[];
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly period: Period;
  readonly today: Day;
}

function contentOf(input: ContentInput): HomeContent {
  return {
    summary: summarizePeriod(input.movements, input.display, input.rates),
    trend: balanceTrend({
      movements: input.movements,
      period: input.period,
      today: input.today,
      display: input.display,
      rates: input.rates,
    }),
    breakdown: breakdownOfExpenses(input.movements, input.display, input.rates),
    recent: input.recent,
    owedToMe: loanTotals(input.rows, 'lent', input.display),
    iOwe: loanTotals(input.rows, 'borrowed', input.display),
  };
}
