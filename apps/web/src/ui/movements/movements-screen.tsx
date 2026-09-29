import { useMemo, useState, type ReactElement } from 'react';

import type { MovementsData, MovementsPorts } from '../../application/use-cases/load-movements';
import { groupByDay } from '../../domain/analysis/day-group';
import { summarizePeriod } from '../../domain/analysis/period-summary';
import type { Day } from '../../domain/calendar/day';
import type { CurrencyCode } from '../../domain/money/currency';
import { EMPTY_RATES } from '../../domain/money/exchange-rates';
import { useAccount } from '../account/account-context';
import { LoadingBlock, NoteLine, RetryBlock, messageOf } from '../common/blocks';
import { EmptyState } from '../common/empty-state';
import { FilterPills } from '../common/filter-pills';
import { usePeriod } from '../period/period-context';
import { PeriodPicker } from '../period/period-picker';

import { DayGroupCard } from './day-group-card';
import {
  FILTER_LABELS,
  MOVEMENT_FILTERS,
  filterMovements,
  type MovementFilter,
} from './movement-filter';
import { PeriodSummaryCard } from './period-summary-card';
import { useMovements } from './use-movements';

/**
 * Movimientos: el resumen del periodo, las tres pills y la lista agrupada por dia, del dia
 * mas reciente al mas antiguo.
 *
 * El resumen sale del periodo entero y las pills solo filtran la lista de abajo, asi que
 * cambiar de pill no mueve las tres cifras de arriba.
 */
export function MovementsScreen({ ports }: { ports: MovementsPorts }): ReactElement {
  const { today } = usePeriod();
  const { settings } = useAccount();
  const { data, loading, everLoaded, reload } = useMovements(ports);
  const [filter, setFilter] = useState<MovementFilter>('all');
  const display = settings.displayCurrency;
  const rates = data?.rates ?? EMPTY_RATES;
  return (
    <div className="g-movements">
      <h1 className="g-screen__title">Movimientos</h1>
      <PeriodPicker />
      {data?.ratesUnavailable === true ? (
        <NoteLine>Las tasas de cambio aún no están listas.</NoteLine>
      ) : null}
      <PeriodSummaryCard
        display={display}
        summary={summarizePeriod(data?.movements ?? [], display, rates)}
      />
      <FilterPills
        label="Tipo de movimiento"
        labels={FILTER_LABELS}
        onSelect={setFilter}
        options={MOVEMENT_FILTERS}
        selected={filter}
      />
      <Body
        data={data}
        display={display}
        everLoaded={everLoaded}
        filter={filter}
        loading={loading}
        onRetry={reload}
        today={today}
      />
    </div>
  );
}

interface BodyProps {
  readonly data: MovementsData | null;
  readonly filter: MovementFilter;
  readonly display: CurrencyCode;
  readonly today: Day;
  readonly loading: boolean;
  readonly everLoaded: boolean;
  readonly onRetry: () => void;
}

function Body(props: BodyProps): ReactElement | null {
  const rates = props.data?.rates ?? EMPTY_RATES;
  const groups = useMemo(
    () =>
      groupByDay(filterMovements(props.data?.movements ?? [], props.filter), props.display, rates),
    [props.data, props.filter, props.display, rates],
  );
  const error = props.data?.error ?? null;
  if (error !== null) return <RetryBlock message={messageOf(error)} onRetry={props.onRetry} />;
  if (props.loading && !props.everLoaded) return <LoadingBlock />;
  if (props.data === null) return null;
  if (groups.length === 0) {
    return <EmptyState icon="receipt_long" message="No hay movimientos en este periodo." />;
  }
  return (
    <div className="g-groups">
      {groups.map((group) => (
        <DayGroupCard
          display={props.display}
          group={group}
          key={group.date}
          rates={rates}
          today={props.today}
        />
      ))}
    </div>
  );
}
