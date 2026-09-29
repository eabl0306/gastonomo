import type { ReactElement } from 'react';

import { isEmptyTrend } from '../../domain/analysis/balance-trend';
import type { BalanceTrend } from '../../domain/analysis/balance-trend';
import type { PeriodSummary } from '../../domain/analysis/period-summary';
import { periodFrom, type Period } from '../../domain/calendar/period';
import type { CurrencyCode } from '../../domain/money/currency';
import { Icon } from '../auth/auth-chrome';
import { Amount } from '../common/amount';
import { useFormats } from '../format/formats-context';

import { TrendChart } from './trend-chart';

export interface BalanceCardProps {
  readonly summary: PeriodSummary;
  readonly trend: BalanceTrend;
  readonly period: Period;
  readonly display: CurrencyCode;
  readonly hidden: boolean;
  readonly onToggleHidden: () => void;
}

/**
 * La tarjeta de balance: la unica superficie oscura del diseno.
 *
 * El ojo no oculta la tarjeta, solo sustituye cada importe por "{simbolo} ••••••", y la
 * tendencia se sigue dibujando: es una silueta, no una cifra.
 */
export function BalanceCard(props: BalanceCardProps): ReactElement {
  const formats = useFormats();
  const shown = (value: PeriodSummary['income']): string =>
    props.hidden ? formats.masked(props.display) : formats.amountOf(value);
  return (
    <section className="g-balance">
      <header className="g-balance__head">
        <span className="g-balance__label">{headingOf(props.period, formats.dates.monthName)}</span>
        <button
          aria-label={props.hidden ? 'Mostrar los montos' : 'Ocultar los montos'}
          className="g-balance__eye"
          onClick={props.onToggleHidden}
          type="button"
        >
          <Icon name={props.hidden ? 'visibility_off' : 'visibility'} />
        </button>
      </header>
      <Amount
        className="g-balance__amount"
        text={
          props.hidden
            ? formats.masked(props.display)
            : formats.balance(props.summary.net.amount, props.display)
        }
      />
      <Trend period={props.period} trend={props.trend} />
      <div className="g-balance__subs">
        <SubCard
          amount={shown(props.summary.income)}
          icon="arrow_downward"
          label="Ingresos"
          tone="income"
        />
        <SubCard
          amount={shown(props.summary.expense)}
          icon="arrow_upward"
          label="Gastos"
          tone="expense"
        />
      </div>
    </section>
  );
}

/** "Balance de septiembre" con un mes, "Balance del rango" con un rango. */
function headingOf(period: Period, monthName: (month: string) => string): string {
  return period.kind === 'month' ? `Balance de ${monthName(period.month)}` : 'Balance del rango';
}

function Trend({ trend, period }: { trend: BalanceTrend; period: Period }): ReactElement | null {
  const formats = useFormats();
  if (isEmptyTrend(trend)) return null;
  const first = trend.points[0];
  const last = trend.points[trend.points.length - 1];
  if (first === undefined || last === undefined) return null;
  return (
    <div className="g-balance__trend">
      <TrendChart trend={trend} />
      <div className="g-balance__trend-labels">
        <span>{formats.dates.short(first.date)}</span>
        <span>{formats.dates.short(last.date)}</span>
      </div>
      <span className="g-visually-hidden">
        {`Tendencia del balance desde el ${formats.dates.short(periodFrom(period))}.`}
      </span>
    </div>
  );
}

function SubCard({
  label,
  icon,
  tone,
  amount,
}: {
  label: string;
  icon: string;
  tone: 'income' | 'expense';
  amount: string;
}): ReactElement {
  return (
    <div className="g-balance__sub">
      <p className="g-balance__sub-label" data-tone={tone}>
        <Icon name={icon} />
        {label}
      </p>
      <Amount className="g-balance__sub-amount" text={amount} />
    </div>
  );
}
