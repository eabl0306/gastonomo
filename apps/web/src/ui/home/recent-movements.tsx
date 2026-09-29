import type { ReactElement } from 'react';

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

export interface RecentProps {
  readonly movements: readonly Movement[];
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
  readonly onSeeAll: () => void;
}

/**
 * "Ultimos movimientos", con "Ver todos" al lado del titulo.
 *
 * **Estos importes no se ocultan con el ojo**, igual que en el movil: el ojo tapa las cifras
 * agregadas, no el detalle de una fila.
 */
export function RecentMovements(props: RecentProps): ReactElement {
  return (
    <section className="g-recent">
      <header className="g-recent__head">
        <h2 className="g-card__title">Últimos movimientos</h2>
        <button className="g-recent__all" onClick={props.onSeeAll} type="button">
          Ver todos
        </button>
      </header>
      <div className="g-card g-recent__list">
        {props.movements.length === 0 ? (
          <p className="g-card__empty">Sin movimientos en este periodo.</p>
        ) : (
          props.movements.map((movement) => (
            <Row
              display={props.display}
              key={movement.id}
              movement={movement}
              rates={props.rates}
            />
          ))
        )}
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
  const income = isIncome(movement.kind);
  const shown = displayedAmount(movement.money, display, rates);
  return (
    <div className="g-row">
      <CategoryTile color={movement.category.color} icon={movement.category.icon} size="large" />
      <div className="g-row__body">
        <p className="g-row__title">{movement.title}</p>
        <p className="g-row__subtitle">{subtitleOf(movement, display, formats)}</p>
      </div>
      <Amount className="g-row__amount" text={formats.signed(shown, income, display)} />
    </div>
  );
}

/** "{categoria} · {fecha}" y, si el registro esta en otra moneda, su importe original. */
function subtitleOf(movement: Movement, display: CurrencyCode, formats: GastonomoFormats): string {
  const head = movement.category.name + DOT_SEPARATOR + formats.dates.short(movement.occurredOn);
  if (!isForeignTo(movement.money, display)) return head;
  return head + DOT_SEPARATOR + formats.amount(movement.money.amount, movement.money.currency);
}
