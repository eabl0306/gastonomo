import type { ReactElement } from 'react';

import type { CategoryBreakdown } from '../../domain/analysis/category-breakdown';
import type { CurrencyCode } from '../../domain/money/currency';
import { Amount } from '../common/amount';
import { CategoryTile } from '../common/tile';
import { MASK_SHORT } from '../format/formats';
import { useFormats } from '../format/formats-context';

export interface BreakdownProps {
  readonly breakdown: CategoryBreakdown;
  readonly display: CurrencyCode;
  readonly hidden: boolean;
}

/** Una porcion nunca desaparece del todo: sin esto un 0,2 por ciento no se veria. */
const MIN_FRACTION = 0.02;

/**
 * Gastos por categoria: la barra apilada de las seis categorias con mas gasto y una fila por
 * cada una.
 *
 * El divisor es el **total de gastos del periodo entero**, no la suma de las seis porciones,
 * asi que con mas de seis categorias la barra no llega al final. El diseno deja ese hueco a
 * proposito en vez de repartir el resto.
 *
 * Con el ojo cerrado el importe de la fila se sustituye por cuatro puntos y **sin simbolo**,
 * que no es la misma mascara que la de la tarjeta de balance. La barra y el porcentaje se
 * siguen viendo, porque son proporciones y no cifras.
 */
export function ExpensesByCategory(props: BreakdownProps): ReactElement {
  const formats = useFormats();
  return (
    <section className="g-card g-breakdown">
      <header className="g-breakdown__head">
        <h2 className="g-card__title">Gastos por categoría</h2>
        <Amount
          className="g-breakdown__total"
          text={
            props.hidden ? formats.masked(props.display) : formats.amountOf(props.breakdown.total)
          }
        />
      </header>
      {props.breakdown.slices.length === 0 ? (
        <p className="g-card__empty">Sin gastos en este periodo.</p>
      ) : (
        <>
          <StackedBar breakdown={props.breakdown} />
          {props.breakdown.slices.map((slice) => (
            <div className="g-breakdown__row" key={slice.category.id}>
              <CategoryTile color={slice.category.color} icon={slice.category.icon} size="small" />
              <span className="g-breakdown__name">{slice.category.name}</span>
              <span className="g-breakdown__percent">{`${String(slice.percent)}%`}</span>
              <Amount
                className="g-breakdown__amount"
                text={props.hidden ? MASK_SHORT : formats.amountOf(slice.total)}
              />
            </div>
          ))}
        </>
      )}
    </section>
  );
}

function StackedBar({ breakdown }: { breakdown: CategoryBreakdown }): ReactElement {
  return (
    <div className="g-bar">
      {breakdown.slices.map((slice) => (
        <span
          className="g-bar__slice"
          key={slice.category.id}
          style={{
            flexGrow: Math.max(slice.fraction, MIN_FRACTION),
            background: slice.category.color,
          }}
        />
      ))}
      <Rest breakdown={breakdown} />
    </div>
  );
}

/** El hueco que queda cuando las seis porciones no llegan al total del periodo. */
function Rest({ breakdown }: { breakdown: CategoryBreakdown }): ReactElement | null {
  const covered = breakdown.slices.reduce(
    (total, slice) => total + Math.max(slice.fraction, MIN_FRACTION),
    0,
  );
  const rest = 1 - covered;
  return rest > MIN_FRACTION ? <span style={{ flexGrow: rest }} /> : null;
}
