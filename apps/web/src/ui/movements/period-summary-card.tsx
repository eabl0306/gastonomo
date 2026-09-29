import type { ReactElement } from 'react';

import type { PeriodSummary } from '../../domain/analysis/period-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import { Amount } from '../common/amount';
import { useFormats } from '../format/formats-context';

/**
 * El resumen de tres columnas: Ingresos en verde, Gastos en rojo y el Neto en ink.
 *
 * Se calcula sobre el periodo **sin filtrar**: cambiar de pill no cambia estas tres cifras.
 * El Neto tampoco se enmascara con el ojo, igual que en el movil: ocultar es de Inicio.
 */
export function PeriodSummaryCard({
  summary,
  display,
}: {
  summary: PeriodSummary;
  display: CurrencyCode;
}): ReactElement {
  const formats = useFormats();
  return (
    <div className="g-summary">
      <Column label="Ingresos" text={formats.amount(summary.income.amount, display)} tone="in" />
      <Column label="Gastos" text={formats.amount(summary.expense.amount, display)} tone="out" />
      <Column label="Neto" text={formats.net(summary.net.amount, display)} tone="net" />
    </div>
  );
}

function Column({
  label,
  text,
  tone,
}: {
  label: string;
  text: string;
  tone: 'in' | 'out' | 'net';
}): ReactElement {
  return (
    <div className="g-summary__column" data-tone={tone}>
      <p className="g-summary__label">{label}</p>
      <Amount className="g-summary__amount" text={text} />
    </div>
  );
}
