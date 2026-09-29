import { useState, useRef, type ChangeEvent, type ReactElement } from 'react';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import type { MoneyFieldState, MoneyFieldView } from '../../application/state/money-field';
import { impliedRate } from '../../application/state/money-field';
import type { CurrencyCode } from '../../domain/money/currency';
import { signOf } from '../../domain/money/decimal';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { Icon } from '../auth/auth-chrome';
import { amountToRaw, caretAfter, digitsBefore, type AmountInput } from '../format/amount-input';
import type { GastonomoFormats } from '../format/formats';
import { useFormats } from '../format/formats-context';

import { CurrencySheet } from './currency-sheet';

export interface AmountFieldProps {
  readonly state: MoneyFieldState;
  readonly view: MoneyFieldView;
  readonly tone: 'income' | 'expense' | 'lent' | 'borrowed' | 'neutral';
  /** La linea de debajo del importe, como "Monto total". */
  readonly caption?: string;
  readonly catalog: readonly CurrencyInfo[];
  readonly rates: ExchangeRates;
  readonly onAmount: (raw: string) => void;
  readonly onConversion: (raw: string | null) => void;
  readonly onCurrency: (code: CurrencyCode) => void;
}

/**
 * El importe grande de los tres formularios, con su chip de moneda y, cuando el registro no
 * esta en la moneda principal, la caja de equivalencia.
 *
 * Esa caja no es un adorno: lo que se guarda es lo que quede ahi, y la API no recalcula nada.
 * Por eso un registro en otra moneda sin equivalencia no se puede guardar.
 */
export function AmountField(props: AmountFieldProps): ReactElement {
  const formats = useFormats();
  const input = formats.amountInput;
  const [picking, setPicking] = useState(false);
  const close = (): void => {
    setPicking(false);
  };
  return (
    <div className="g-amount" data-tone={props.tone}>
      <div className="g-amount__row">
        <span className="g-amount__symbol">{formats.symbolOf(props.state.currency)}</span>
        <RawInput
          ariaLabel="Importe"
          className="g-amount__input"
          input={input}
          onChange={(typed) => {
            props.onAmount(input.sanitize(typed, props.state.currency));
          }}
          raw={props.state.amount}
        />
      </div>
      {props.caption === undefined ? null : <p className="g-amount__caption">{props.caption}</p>}
      <CurrencyChip
        code={props.state.currency}
        language={formats.language}
        onClick={() => {
          setPicking(true);
        }}
      />
      {props.view.foreign ? <ConversionBox {...props} /> : null}
      {picking ? (
        <CurrencySheet
          catalog={props.catalog}
          onClose={close}
          onPick={(code) => {
            props.onCurrency(code);
            close();
          }}
          rates={props.rates}
          selected={props.state.currency}
          subtitle={`Por defecto se usa ${formats.displayCurrency}. Si eliges otra, guardamos también su equivalencia.`}
          title="Moneda del registro"
        />
      ) : null}
    </div>
  );
}

function CurrencyChip({
  code,
  language,
  onClick,
}: {
  code: CurrencyCode;
  language: string;
  onClick: () => void;
}): ReactElement {
  return (
    <button className="g-amount__chip" onClick={onClick} type="button">
      {`${code} · ${currencyName(code, language)}`}
      <Icon name="expand_more" />
    </button>
  );
}

/**
 * El nombre de la moneda en el idioma vigente.
 *
 * Sale de `Intl.DisplayNames` y no de la API, que lo manda siempre en ingles. Si el navegador
 * no conoce el codigo se queda con el codigo: nunca en blanco.
 */
function currencyName(code: string, language: string): string {
  try {
    const names = new Intl.DisplayNames([language === 'en' ? 'en-US' : 'es-CO'], {
      type: 'currency',
    });
    const name = names.of(code) ?? code;
    return name === code ? code : name.slice(0, 1).toUpperCase() + name.slice(1);
  } catch {
    return code;
  }
}

function ConversionBox(props: AmountFieldProps): ReactElement {
  const formats = useFormats();
  const input = formats.amountInput;
  const display = formats.displayCurrency;
  const shown =
    props.state.conversion ??
    (props.view.suggested !== null && signOf(props.view.amount) > 0
      ? amountToRaw(props.view.suggested, display)
      : '');
  return (
    <div className="g-conversion">
      <div className="g-conversion__head">
        <span className="g-conversion__label">{`Equivale a (${display})`}</span>
        {props.view.edited ? (
          <button
            className="g-conversion__reset"
            onClick={() => {
              props.onConversion(null);
            }}
            type="button"
          >
            Usar sugerida
          </button>
        ) : null}
      </div>
      <div className="g-conversion__row">
        <span className="g-conversion__symbol">{formats.symbolOf(display)}</span>
        <RawInput
          ariaLabel={`Equivalencia en ${display}`}
          className="g-conversion__input"
          input={input}
          onChange={(typed) => {
            props.onConversion(input.sanitize(typed, display));
          }}
          raw={shown}
        />
        <Icon name="edit" />
      </div>
      <p className="g-conversion__rate">{rateLineOf(props, formats)}</p>
    </div>
  );
}

/** La linea de debajo de la caja, que dice de donde sale la equivalencia. */
function rateLineOf(props: AmountFieldProps, formats: GastonomoFormats): string {
  const display = formats.displayCurrency;
  const from = props.state.currency;
  const own = impliedRate(props.view);
  if (props.view.edited && own !== null && signOf(props.view.base) > 0) {
    const suffix = props.view.rate === null ? '' : ` · sugerida ${formats.rate(props.view.rate)}`;
    return `Tu tasa: 1 ${from} = ${formats.rate(own)} ${display}${suffix}`;
  }
  if (props.view.rate === null) {
    return `No hay tasa disponible para ${from}. Escribe la equivalencia.`;
  }
  return `Tasa sugerida: 1 ${from} = ${formats.rate(props.view.rate)} ${display}. Puedes cambiarla.`;
}

/**
 * Un campo que pinta el valor con separadores pero guarda el raw.
 *
 * El cursor se recoloca a mano: el campo reescribe el texto en cada pulsacion, y meter un
 * separador de miles corre a la derecha todo lo que hay detras. Sin esto, editar el medio de
 * un importe es imposible.
 */
function RawInput({
  raw,
  input,
  onChange,
  className,
  ariaLabel,
}: {
  raw: string;
  input: AmountInput;
  onChange: (typed: string) => void;
  className: string;
  ariaLabel: string;
}): ReactElement {
  const element = useRef<HTMLInputElement>(null);
  const shown = input.display(raw);
  const handle = (event: ChangeEvent<HTMLInputElement>): void => {
    const typed = event.target.value;
    const digits = digitsBefore(typed, event.target.selectionStart ?? typed.length);
    onChange(typed);
    queueMicrotask(() => {
      const node = element.current;
      if (node === null) return;
      const caret = caretAfter(node.value, digits);
      node.setSelectionRange(caret, caret);
    });
  };
  return (
    <input
      aria-label={ariaLabel}
      className={className}
      inputMode="decimal"
      onChange={handle}
      placeholder="0"
      ref={element}
      value={shown}
    />
  );
}
