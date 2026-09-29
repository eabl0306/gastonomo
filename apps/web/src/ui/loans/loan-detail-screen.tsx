import type { ReactElement } from 'react';

import type { LoanPorts } from '../../application/use-cases/load-loan-list';
import type { LoanPayment } from '../../domain/ledger/loan';
import type { LoanRow } from '../../domain/ledger/loan-summary';
import { displayedMoney } from '../../domain/money/displayed-amount';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { isForeignTo } from '../../domain/money/monetary-amount';
import { Icon } from '../auth/auth-chrome';
import { Amount } from '../common/amount';
import { LoadingBlock, RetryBlock, messageOf } from '../common/blocks';
import { DOT_SEPARATOR, type GastonomoFormats } from '../format/formats';
import { useFormats } from '../format/formats-context';

import { STATUS_LABELS, directionLabel, headlineOf, initialOf } from './loan-view';
import { useLoans } from './use-loans';

export interface LoanDetailActions {
  readonly onBack: () => void;
  readonly onRegisterPayment: () => void;
}

/**
 * El detalle de un prestamo: quien, su monto, lo abonado, el estado y el historial de abonos.
 *
 * Se saca de la lista ya cargada en vez de pedir `GET /v1/loans/{id}`: la lista ya trae los
 * abonos dentro, asi que una peticion mas solo anadiria un fotograma en blanco.
 */
export function LoanDetailScreen({
  ports,
  loanId,
  actions,
}: {
  ports: LoanPorts;
  loanId: string;
  actions: LoanDetailActions;
}): ReactElement {
  const { data, loading, reload } = useLoans(ports);
  const row = data?.rows.find((candidate) => candidate.loan.id === loanId) ?? null;
  return (
    <div className="g-detail">
      <header className="g-detail__bar">
        <button
          aria-label="Volver"
          className="g-detail__back"
          onClick={actions.onBack}
          type="button"
        >
          <Icon name="arrow_back" />
        </button>
        <h1 className="g-detail__title">Detalle del préstamo</h1>
      </header>
      <Body
        error={data?.error ?? null}
        loading={loading}
        onRegisterPayment={actions.onRegisterPayment}
        onRetry={reload}
        rates={data?.rates ?? null}
        row={row}
      />
    </div>
  );
}

interface BodyProps {
  readonly row: LoanRow | null;
  readonly rates: ExchangeRates | null;
  readonly error: Parameters<typeof messageOf>[0] | null;
  readonly loading: boolean;
  readonly onRetry: () => void;
  readonly onRegisterPayment: () => void;
}

function Body(props: BodyProps): ReactElement {
  if (props.error !== null) {
    return <RetryBlock message={messageOf(props.error)} onRetry={props.onRetry} />;
  }
  if (props.loading) return <LoadingBlock />;
  if (props.row === null || props.rates === null) {
    return <RetryBlock message="No encontramos este préstamo." onRetry={props.onRetry} />;
  }
  return (
    <>
      <LoanPanel row={props.row} />
      {props.row.summary.status === 'paid' ? null : (
        <button className="g-detail__pay" onClick={props.onRegisterPayment} type="button">
          <Icon name="add_card" />
          Registrar abono
        </button>
      )}
      <h2 className="g-detail__section">Historial de abonos</h2>
      <Payments rates={props.rates} row={props.row} />
    </>
  );
}

function LoanPanel({ row }: { row: LoanRow }): ReactElement {
  const formats = useFormats();
  const headline = headlineOf(row);
  const summary = row.summary;
  return (
    <section className="g-detail__panel" data-direction={row.loan.direction}>
      <header className="g-detail__head">
        <span className="g-detail__avatar">{initialOf(row.loan.person)}</span>
        <span className="g-detail__who">
          <span className="g-detail__person">{row.loan.person}</span>
          <span className="g-detail__created">{createdOf(row, formats)}</span>
        </span>
        <span className="g-detail__direction">{directionLabel(row.loan.direction)}</span>
      </header>
      {row.loan.note === '' ? null : <p className="g-detail__note">{row.loan.note}</p>}
      <p className="g-detail__figure-label g-detail__headline-label">{headline.label}</p>
      <Amount className="g-detail__headline" text={formats.amountOf(headline.amount)} />
      <span
        aria-label={`${String(summary.percent)}% abonado`}
        className="g-bar-track g-bar-track--large"
        role="img"
      >
        <span className="g-bar-fill" style={{ width: `${String(summary.percent)}%` }} />
      </span>
      <div className="g-detail__columns">
        <Column label="Original" value={formats.amountOf(summary.total)} />
        <Column label="Abonado" value={formats.amountOf(summary.paid)} />
        <Column
          label="Estado"
          value={`${STATUS_LABELS[summary.status]} · ${String(summary.percent)}%`}
        />
      </div>
    </section>
  );
}

/** "Creado el 22 sept 2026" y, si el prestamo esta en otra moneda, su importe original. */
function createdOf(row: LoanRow, formats: GastonomoFormats): string {
  const created = `Creado el ${formats.dates.long(row.loan.openedOn)}`;
  if (!isForeignTo(row.loan.money, formats.displayCurrency)) return created;
  const original = formats.amount(row.loan.money.amount, row.loan.money.currency);
  return created + DOT_SEPARATOR + original;
}

function Column({ label, value }: { label: string; value: string }): ReactElement {
  return (
    <div className="g-detail__column">
      <p className="g-detail__figure-label">{label}</p>
      <Amount className="g-detail__column-value" text={value} />
    </div>
  );
}

function Payments({ row, rates }: { row: LoanRow; rates: ExchangeRates }): ReactElement {
  const sorted = [...row.loan.payments].sort((a, b) => b.paidOn.localeCompare(a.paidOn));
  return (
    <div className="g-card g-detail__payments">
      {sorted.length === 0 ? (
        <p className="g-card__empty">Aún no hay abonos.</p>
      ) : (
        sorted.map((payment) => <PaymentRow key={payment.id} payment={payment} rates={rates} />)
      )}
    </div>
  );
}

function PaymentRow({
  payment,
  rates,
}: {
  payment: LoanPayment;
  rates: ExchangeRates;
}): ReactElement {
  const formats = useFormats();
  const shown = displayedMoney(payment.money, formats.displayCurrency, rates);
  return (
    <div className="g-row">
      <span className="g-payment__check">
        <Icon name="check" />
      </span>
      <div className="g-row__body">
        <p className="g-row__title">
          {payment.label === null ? 'Abono' : `Abono · ${payment.label}`}
        </p>
        <p className="g-row__subtitle">{dateOf(payment, formats)}</p>
      </div>
      <Amount className="g-row__amount" text={formats.amountOf(shown)} />
    </div>
  );
}

function dateOf(payment: LoanPayment, formats: GastonomoFormats): string {
  const date = formats.dates.long(payment.paidOn);
  if (!isForeignTo(payment.money, formats.displayCurrency)) return date;
  return date + DOT_SEPARATOR + formats.amount(payment.money.amount, payment.money.currency);
}
