import type { ReactElement } from 'react';

import { isComplete, type DictationDraft } from '../../domain/dictation/draft';
import { isIncome } from '../../domain/ledger/category';
import type { CurrencyCode } from '../../domain/money/currency';
import { convert, type ExchangeRates } from '../../domain/money/exchange-rates';
import { Icon } from '../auth/auth-chrome';
import { Amount } from '../common/amount';
import { useFormats } from '../format/formats-context';

/** El icono y el titulo de la tarjeta, segun lo que se dicto. */
function headOf(draft: DictationDraft): { icon: string; title: string; subtitle: string } {
  if (draft.entry.kind === 'movement') {
    const movement = draft.entry.movement;
    return {
      icon: isIncome(movement.kind) ? 'south_west' : 'north_east',
      title: movement.note === '' ? movement.category.name : movement.note,
      subtitle: movement.category.name,
    };
  }
  const loan = draft.entry.loan;
  return {
    icon: 'handshake',
    title: loan.person === '' ? 'Sin persona' : loan.person,
    subtitle: loan.direction === 'lent' ? 'Presté' : 'Me prestaron',
  };
}

/** Lo que falta, dicho en castellano. Es lo unico que apaga el boton de guardar. */
const MISSING_LABELS: Readonly<Record<string, string>> = {
  amount: 'Falta el monto.',
  person: 'Falta con quién es el préstamo.',
};

function missingText(missing: readonly string[]): string {
  const known = missing.map((field) => MISSING_LABELS[field] ?? `Falta ${field}.`);
  return known.join(' ');
}

/**
 * La tarjeta de revision.
 *
 * Ensena lo que la API entendio, incluido lo que **sustituyo**: una categoria que no existe
 * acaba en "Otros" y una fecha futura en hoy, y eso tiene que verse antes de guardar.
 */
export function DraftCard({
  draft,
  display,
  rates,
}: {
  draft: DictationDraft;
  display: CurrencyCode;
  rates: ExchangeRates;
}): ReactElement {
  const formats = useFormats();
  const head = headOf(draft);
  const income = draft.entry.kind === 'movement' && isIncome(draft.entry.movement.kind);
  return (
    <div className="g-draft">
      <header className="g-draft__head">
        <span className="g-draft__icon" data-tone={income ? 'income' : 'expense'}>
          <Icon name={head.icon} />
        </span>
        <span className="g-draft__who">
          <span className="g-draft__title">{head.title}</span>
          <span className="g-draft__subtitle">{head.subtitle}</span>
        </span>
      </header>
      <Amount
        className="g-draft__amount"
        text={draft.amount === null ? 'Sin monto' : formats.amount(draft.amount, draft.currency)}
      />
      <Equivalent display={display} draft={draft} rates={rates} />
      <dl className="g-draft__rows">
        <Row label="Fecha" value={formats.dates.long(draft.occurredOn)} />
        <Row label="Moneda" value={draft.currency} />
      </dl>
      {isComplete(draft) ? null : (
        <p className="g-draft__missing">
          <Icon name="error" />
          <span>{missingText(draft.missing)}</span>
        </p>
      )}
    </div>
  );
}

/** La equivalencia, solo si el registro no esta en la moneda principal. */
function Equivalent({
  draft,
  display,
  rates,
}: {
  draft: DictationDraft;
  display: CurrencyCode;
  rates: ExchangeRates;
}): ReactElement | null {
  const formats = useFormats();
  if (draft.amount === null || draft.currency === display) return null;
  const converted = convert(rates, draft.amount, draft.currency, display);
  if (converted === null) return null;
  return (
    <p className="g-draft__equivalent">
      {`≈ ${formats.amount(converted, display)} con la tasa de hoy`}
    </p>
  );
}

function Row({ label, value }: { label: string; value: string }): ReactElement {
  return (
    <div className="g-draft__row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
