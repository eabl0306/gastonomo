import type { ReactElement } from 'react';

import type { LoanRow } from '../../domain/ledger/loan-summary';
import { Icon } from '../auth/auth-chrome';
import { Amount } from '../common/amount';
import { DOT_SEPARATOR } from '../format/formats';
import { useFormats } from '../format/formats-context';

import { STATUS_LABELS, directionLabel, headlineOf, initialOf } from './loan-view';

export interface LoanCardProps {
  readonly row: LoanRow;
  readonly onOpen: () => void;
  readonly onRegisterPayment: () => void;
}

/**
 * La tarjeta de un prestamo: quien, su estado, la cifra que importa, la barra de progreso y
 * el boton de abonar cuando queda saldo.
 *
 * La tarjeta entera abre el detalle, asi que el boton de abonar tiene que parar el clic o
 * pulsarlo abriria las dos cosas.
 */
export function LoanCard({ row, onOpen, onRegisterPayment }: LoanCardProps): ReactElement {
  const formats = useFormats();
  const headline = headlineOf(row);
  const summary = row.summary;
  return (
    <article className="g-loan" data-direction={row.loan.direction}>
      <button
        aria-label={`Ver el préstamo de ${row.loan.person}`}
        className="g-loan__open"
        onClick={onOpen}
        type="button"
      >
        <header className="g-loan__head">
          <span className="g-loan__avatar">{initialOf(row.loan.person)}</span>
          <span className="g-loan__who">
            <span className="g-loan__person">{row.loan.person}</span>
            <span className="g-loan__sub">{subtitleOf(row, formats.dates.short)}</span>
          </span>
          <span className="g-loan__status" data-status={summary.status}>
            {STATUS_LABELS[summary.status]}
          </span>
        </header>
        <div className="g-loan__figures">
          <span>
            <span className="g-loan__figure-label">{headline.label}</span>
            <Amount className="g-loan__headline" text={formats.amountOf(headline.amount)} />
          </span>
          <span className="g-loan__of">
            <span className="g-loan__figure-label">{`de ${formats.amountOf(summary.total)}`}</span>
            <span className="g-loan__percent">{`${String(summary.percent)}%`}</span>
          </span>
        </div>
        <span aria-label={`${String(summary.percent)}% abonado`} className="g-bar-track" role="img">
          <span className="g-bar-fill" style={{ width: `${String(summary.percent)}%` }} />
        </span>
      </button>
      {summary.status === 'paid' ? null : (
        <button className="g-loan__pay" onClick={onRegisterPayment} type="button">
          <Icon name="add_card" />
          Registrar abono
        </button>
      )}
    </article>
  );
}

/** "Me debe · {nota} · {fecha}". La nota se salta si no la escribio. */
function subtitleOf(row: LoanRow, short: (day: string) => string): string {
  const note = row.loan.note === '' ? '' : DOT_SEPARATOR + row.loan.note;
  return directionLabel(row.loan.direction) + note + DOT_SEPARATOR + short(row.loan.openedOn);
}
