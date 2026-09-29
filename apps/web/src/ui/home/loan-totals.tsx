import type { ReactElement } from 'react';

import type { LoanTotals } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import { Icon } from '../auth/auth-chrome';
import { Amount } from '../common/amount';
import { useFormats } from '../format/formats-context';

export interface LoanTotalsProps {
  readonly owedToMe: LoanTotals;
  readonly iOwe: LoanTotals;
  readonly display: CurrencyCode;
  readonly hidden: boolean;
  readonly onOpenLoans: () => void;
}

/**
 * "Me deben" y "Debo": el saldo pendiente total y cuantos prestamos siguen abiertos.
 *
 * **No dependen del periodo**, a diferencia de todo lo que hay encima: son saldos de hoy. Un
 * prestamo pagado no aporta ni saldo ni conteo. Las dos tarjetas llevan a Prestamos.
 */
export function LoanTotalCards(props: LoanTotalsProps): ReactElement {
  return (
    <div className="g-loan-totals">
      <Card
        display={props.display}
        hidden={props.hidden}
        label="Me deben"
        onClick={props.onOpenLoans}
        tone="lent"
        totals={props.owedToMe}
      />
      <Card
        display={props.display}
        hidden={props.hidden}
        label="Debo"
        onClick={props.onOpenLoans}
        tone="borrowed"
        totals={props.iOwe}
      />
    </div>
  );
}

/** "Nada pendiente", "1 préstamo" o "N préstamos". El cero no es un plural, es otra frase. */
function loanCountLabel(count: number): string {
  if (count === 0) return 'Nada pendiente';
  return count === 1 ? '1 préstamo' : `${String(count)} préstamos`;
}

interface CardProps {
  readonly label: string;
  readonly totals: LoanTotals;
  readonly tone: 'lent' | 'borrowed';
  readonly display: CurrencyCode;
  readonly hidden: boolean;
  readonly onClick: () => void;
}

function Card({ label, totals, tone, display, hidden, onClick }: CardProps): ReactElement {
  const formats = useFormats();
  return (
    <button className="g-loan-total" data-tone={tone} onClick={onClick} type="button">
      <span className="g-loan-total__head">
        {label}
        <Icon name="arrow_outward" />
      </span>
      <Amount
        className="g-loan-total__amount"
        text={hidden ? formats.masked(display) : formats.amountOf(totals.outstanding)}
      />
      <span className="g-loan-total__count">{loanCountLabel(totals.openCount)}</span>
    </button>
  );
}
