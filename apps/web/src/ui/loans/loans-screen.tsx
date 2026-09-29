import { useMemo, useState, type ReactElement, type ReactNode } from 'react';

import type { LoanPorts } from '../../application/use-cases/load-loan-list';
import type { LoanRow, LoanTotals } from '../../domain/ledger/loan-summary';
import { loanTotals, sortedLoans } from '../../domain/ledger/loan-summary';
import type { CurrencyCode } from '../../domain/money/currency';
import { useAccount } from '../account/account-context';
import { Amount } from '../common/amount';
import { LoadingBlock, RetryBlock, messageOf } from '../common/blocks';
import { EmptyState } from '../common/empty-state';
import { FilterPills } from '../common/filter-pills';
import { useFormats } from '../format/formats-context';

import { LoanCard } from './loan-card';
import { LOAN_FILTERS, LOAN_FILTER_LABELS, filterLoans, type LoanFilter } from './loan-view';
import { PaymentSheet } from './payment-sheet';
import { useLoans } from './use-loans';

/** Lo que Prestamos no decide: a donde lleva abrir un prestamo. */
export interface LoansActions {
  readonly onOpenLoan: (id: string) => void;
}

/**
 * Prestamos: las dos tarjetas de totales, las tres pills y las tarjetas de cada prestamo.
 *
 * Nada de aqui depende del periodo: son saldos de hoy, y por eso esta pantalla no lleva el
 * selector de mes.
 */
export function LoansScreen({
  ports,
  actions,
}: {
  ports: LoanPorts;
  actions: LoansActions;
}): ReactElement {
  const { settings } = useAccount();
  const { data, loading, everLoaded, reload } = useLoans(ports);
  const [filter, setFilter] = useState<LoanFilter>('all');
  const [paying, setPaying] = useState<string | null>(null);
  const display = settings.displayCurrency;
  // Las dos tarjetas de arriba no se pintan a cero mientras carga: un "Nada pendiente" que
  // luego cambia se lee como un dato, no como que todavia no ha llegado.
  if (data === null) return <Screen>{<LoadingBlock />}</Screen>;
  const rows = data.rows;
  const paid = rows.find((row) => row.loan.id === paying) ?? null;
  return (
    <Screen>
      <Totals display={display} rows={rows} />
      <FilterPills
        label="Dirección del préstamo"
        labels={LOAN_FILTER_LABELS}
        onSelect={setFilter}
        options={LOAN_FILTERS}
        selected={filter}
      />
      <Body
        actions={actions}
        error={data.error}
        everLoaded={everLoaded}
        filter={filter}
        loading={loading}
        onRegisterPayment={setPaying}
        onRetry={reload}
        rows={rows}
      />
      <Payment
        gateway={ports.loans}
        onDone={() => {
          setPaying(null);
          reload();
        }}
        row={paid}
      />
    </Screen>
  );
}

function Screen({ children }: { children: ReactNode }): ReactElement {
  return (
    <div className="g-loans">
      <h1 className="g-screen__title">Préstamos</h1>
      {children}
    </div>
  );
}

/**
 * El sheet de abono, si hay un prestamo elegido.
 *
 * Al guardar se recarga la lista: el saldo y el estado de la tarjeta cambian, y dejarlos como
 * estaban haria pensar que el abono no se guardo.
 */
function Payment({
  row,
  gateway,
  onDone,
}: {
  row: LoanRow | null;
  gateway: LoanPorts['loans'];
  onDone: () => void;
}): ReactElement | null {
  if (row === null) return null;
  return <PaymentSheet gateway={gateway} onClose={onDone} onSaved={onDone} row={row} />;
}

function Totals({
  rows,
  display,
}: {
  rows: readonly LoanRow[];
  display: CurrencyCode;
}): ReactElement {
  const owedToMe = loanTotals(rows, 'lent', display);
  const iOwe = loanTotals(rows, 'borrowed', display);
  return (
    <div className="g-loan-heads">
      <TotalCard label="Te deben" tone="lent" totals={owedToMe} />
      <TotalCard label="Debes" tone="borrowed" totals={iOwe} />
    </div>
  );
}

/** "Nada pendiente", "1 préstamo" o "N préstamos". El cero no es un plural, es otra frase. */
function countLabel(count: number): string {
  if (count === 0) return 'Nada pendiente';
  return count === 1 ? '1 préstamo' : `${String(count)} préstamos`;
}

function TotalCard({
  label,
  totals,
  tone,
}: {
  label: string;
  totals: LoanTotals;
  tone: 'lent' | 'borrowed';
}): ReactElement {
  const formats = useFormats();
  return (
    <div className="g-loan-head" data-tone={tone}>
      <p className="g-loan-head__label">{label}</p>
      <Amount className="g-loan-head__amount" text={formats.amountOf(totals.outstanding)} />
      <p className="g-loan-head__count">{countLabel(totals.openCount)}</p>
    </div>
  );
}

interface BodyProps {
  readonly rows: readonly LoanRow[];
  readonly filter: LoanFilter;
  readonly error: Parameters<typeof messageOf>[0] | null;
  readonly loading: boolean;
  readonly everLoaded: boolean;
  readonly onRetry: () => void;
  readonly onRegisterPayment: (id: string) => void;
  readonly actions: LoansActions;
}

function Body(props: BodyProps): ReactElement {
  const visible = useMemo(
    () => sortedLoans(filterLoans(props.rows, props.filter)),
    [props.rows, props.filter],
  );
  if (props.error !== null) {
    return <RetryBlock message={messageOf(props.error)} onRetry={props.onRetry} />;
  }
  if (props.loading && !props.everLoaded) return <LoadingBlock />;
  if (visible.length === 0) {
    return <EmptyState icon="handshake" message="No tienes préstamos en esta lista." />;
  }
  return (
    <div className="g-loan-grid">
      {visible.map((row) => (
        <LoanCard
          key={row.loan.id}
          onOpen={() => {
            props.actions.onOpenLoan(row.loan.id);
          }}
          onRegisterPayment={() => {
            props.onRegisterPayment(row.loan.id);
          }}
          row={row}
        />
      ))}
    </div>
  );
}
