import type { ReactElement } from 'react';

import type { HomeContent, HomePorts } from '../../application/use-cases/load-home';
import type { CurrencyCode } from '../../domain/money/currency';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { useAccount } from '../account/account-context';
import { LoadingBlock, NoteLine, RetryBlock, messageOf } from '../common/blocks';
import { usePeriod } from '../period/period-context';
import { PeriodPicker } from '../period/period-picker';

import { BalanceCard } from './balance-card';
import { ExpensesByCategory } from './expenses-by-category';
import { useHideAmounts } from './hide-amounts';
import { HomeHeader } from './home-header';
import { LoanTotalCards } from './loan-totals';
import { QuickActions } from './quick-actions';
import { RecentMovements } from './recent-movements';
import { useHome } from './use-home';

/** Lo que Inicio no decide: a donde llevan sus botones. */
export interface HomeActions {
  readonly onNewMovement: (kind: 'in' | 'out') => void;
  readonly onNewLoan: () => void;
  readonly onOpenLoans: () => void;
  readonly onOpenMovements: () => void;
  readonly onOpenSettings: () => void;
}

/**
 * Inicio: saludo, filtro de periodo, balance con su tendencia, acciones rapidas, prestamos,
 * gastos por categoria y ultimos movimientos.
 *
 * El encabezado y el filtro se pintan **siempre**, tambien mientras carga y en un fallo: lo
 * unico que se sustituye es el bloque de contenido.
 */
export function HomeScreen({
  ports,
  actions,
}: {
  ports: HomePorts;
  actions: HomeActions;
}): ReactElement {
  const { today } = usePeriod();
  const { settings } = useAccount();
  const { data, loading, everLoaded, reload } = useHome(ports);
  const { hidden, toggle } = useHideAmounts();
  return (
    <div className="g-home">
      <HomeHeader onOpenSettings={actions.onOpenSettings} today={today} user={data?.user ?? null} />
      <PeriodPicker />
      {data?.ratesUnavailable === true ? (
        <NoteLine>Las tasas de cambio aún no están listas.</NoteLine>
      ) : null}
      <Body
        actions={actions}
        data={data}
        display={settings.displayCurrency}
        everLoaded={everLoaded}
        hidden={hidden}
        loading={loading}
        onReload={reload}
        onToggleHidden={toggle}
      />
    </div>
  );
}

interface BodyProps {
  readonly data: ReturnType<typeof useHome>['data'];
  readonly display: CurrencyCode;
  readonly loading: boolean;
  readonly everLoaded: boolean;
  readonly hidden: boolean;
  readonly actions: HomeActions;
  readonly onReload: () => void;
  readonly onToggleHidden: () => void;
}

function Body(props: BodyProps): ReactElement | null {
  const error = props.data?.error ?? null;
  if (error !== null) return <RetryBlock message={messageOf(error)} onRetry={props.onReload} />;
  if (props.loading && !props.everLoaded) return <LoadingBlock />;
  const content = props.data?.content ?? null;
  if (content === null || props.data === null) return null;
  return (
    <Columns
      actions={props.actions}
      content={content}
      display={props.display}
      hidden={props.hidden}
      onToggleHidden={props.onToggleHidden}
      rates={props.data.rates}
    />
  );
}

interface ColumnsProps {
  readonly content: HomeContent;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly hidden: boolean;
  readonly actions: HomeActions;
  readonly onToggleHidden: () => void;
}

/**
 * Las dos columnas.
 *
 * El corte no es el de 900px del armazon: es el ancho disponible, porque el grid cae a una
 * columna cuando no caben dos de 400. Por eso lo decide `auto-fit` y no una media query.
 */
function Columns(props: ColumnsProps): ReactElement {
  const { state } = usePeriod();
  return (
    <div className="g-home__grid">
      <div className="g-home__column">
        <BalanceCard
          display={props.display}
          hidden={props.hidden}
          onToggleHidden={props.onToggleHidden}
          period={state.selection.period}
          summary={props.content.summary}
          trend={props.content.trend}
        />
        <QuickActions
          onNewLoan={props.actions.onNewLoan}
          onNewMovement={props.actions.onNewMovement}
          onOpenLoans={props.actions.onOpenLoans}
        />
        <LoanTotalCards
          display={props.display}
          hidden={props.hidden}
          iOwe={props.content.iOwe}
          onOpenLoans={props.actions.onOpenLoans}
          owedToMe={props.content.owedToMe}
        />
      </div>
      <div className="g-home__column">
        <ExpensesByCategory
          breakdown={props.content.breakdown}
          display={props.display}
          hidden={props.hidden}
        />
        <RecentMovements
          display={props.display}
          movements={props.content.recent}
          onSeeAll={props.actions.onOpenMovements}
          rates={props.rates}
        />
      </div>
    </div>
  );
}
