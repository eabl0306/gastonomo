import type { ReactElement } from 'react';

import type { DayGroup } from '../../domain/analysis/day-group';
import type { Day } from '../../domain/calendar/day';
import { isIncome } from '../../domain/ledger/category';
import type { Movement } from '../../domain/ledger/movement';
import type { CurrencyCode } from '../../domain/money/currency';
import { displayedAmount } from '../../domain/money/displayed-amount';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { isForeignTo } from '../../domain/money/monetary-amount';
import { Amount } from '../common/amount';
import { CategoryTile } from '../common/tile';
import { DOT_SEPARATOR, type GastonomoFormats } from '../format/formats';
import { useFormats } from '../format/formats-context';

const DAY_LABELS = { today: 'Hoy', yesterday: 'Ayer' };

export interface DayGroupProps {
  readonly group: DayGroup;
  readonly today: Day;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
}

/**
 * Un grupo de dia: el encabezado con "Hoy", "Ayer" o el dia de la semana y el neto del dia a
 * la derecha, y debajo una tarjeta blanca con sus movimientos.
 */
export function DayGroupCard(props: DayGroupProps): ReactElement {
  const formats = useFormats();
  const label = formats.dates.dayGroup(props.group.date, props.today, DAY_LABELS);
  return (
    <section aria-label={label} className="g-day">
      <header className="g-day__head">
        <span>{label}</span>
        <Amount text={formats.net(props.group.net.amount, props.display)} />
      </header>
      <div className="g-card g-day__list">
        {props.group.movements.map((movement) => (
          <Row display={props.display} key={movement.id} movement={movement} rates={props.rates} />
        ))}
      </div>
    </section>
  );
}

function Row({
  movement,
  display,
  rates,
}: {
  movement: Movement;
  display: CurrencyCode;
  rates: ExchangeRates;
}): ReactElement {
  const formats = useFormats();
  const shown = displayedAmount(movement.money, display, rates);
  return (
    <div className="g-row">
      <CategoryTile color={movement.category.color} icon={movement.category.icon} size="large" />
      <div className="g-row__body">
        <p className="g-row__title">{movement.title}</p>
        <p className="g-row__subtitle">{subtitleOf(movement, display, formats)}</p>
      </div>
      <Amount
        className="g-row__amount"
        text={formats.signed(shown, isIncome(movement.kind), display)}
      />
    </div>
  );
}

/**
 * Solo la categoria, y su importe original si el registro esta en otra moneda.
 *
 * Aqui no va la fecha, a diferencia de Inicio: ya la dice el encabezado del grupo.
 */
function subtitleOf(movement: Movement, display: CurrencyCode, formats: GastonomoFormats): string {
  const name = movement.category.name;
  if (!isForeignTo(movement.money, display)) return name;
  return name + DOT_SEPARATOR + formats.amount(movement.money.amount, movement.money.currency);
}
