import { COP, type CurrencyCode } from '../../domain/money/currency';
import { ZERO, divideDecimals, signOf, type Decimal } from '../../domain/money/decimal';
import { convert, rateOf, type ExchangeRates } from '../../domain/money/exchange-rates';
import { amountOf, type MonetaryAmount } from '../../domain/money/monetary-amount';
import { moneyOf, roundedMoney } from '../../domain/money/money';

/**
 * El campo de importe de los tres formularios: lo que se teclea, en que moneda, y la
 * equivalencia si el usuario la cambio a mano.
 *
 * Los importes van como **cadena raw** (punto decimal, sin miles) porque el campo tiene que
 * conservar lo que se teclea, incluido el separador a medias: un `Decimal` perderia el "12,"
 * de quien todavia no ha escrito los centavos.
 */
export interface MoneyFieldState {
  readonly amount: string;
  readonly currency: CurrencyCode;
  /** La equivalencia tecleada a mano. Null significa "usa la sugerida por la tasa". */
  readonly conversion: string | null;
}

export function emptyMoneyField(currency: CurrencyCode = COP): MoneyFieldState {
  return { amount: '', currency, conversion: null };
}

/** Lo que se deriva del campo para pintarlo y para decidir si se puede guardar. */
export interface MoneyFieldView {
  readonly amount: Decimal;
  readonly currency: CurrencyCode;
  /** True si el registro no esta en la moneda principal, que es cuando sale la caja. */
  readonly foreign: boolean;
  /** True si el usuario escribio la equivalencia a mano. */
  readonly edited: boolean;
  /** La equivalencia que se va a guardar, en la moneda principal. */
  readonly base: Decimal;
  /** La tasa de hoy, o null si no hay ninguna para esta moneda. */
  readonly rate: Decimal | null;
  /** La equivalencia que sugiere la tasa, o null si no hay tasa. */
  readonly suggested: Decimal | null;
  /** True si se puede guardar: hay importe y hay equivalencia. */
  readonly valid: boolean;
}

export interface MoneyFieldInput {
  readonly state: MoneyFieldState;
  /** El importe ya leido del campo, que lo convierte el modulo de formato. */
  readonly amount: Decimal;
  /** La equivalencia tecleada, ya leida. Null si no la escribio. */
  readonly conversion: Decimal | null;
  readonly display: CurrencyCode;
  readonly rates: ExchangeRates;
}

/**
 * Deriva todo lo que la caja de conversion necesita.
 *
 * La equivalencia se guarda **tal como quede aqui**, con la tasa que el usuario pudo editar:
 * la API no recalcula nada, y cambiar la moneda principal despues no toca ningun registro ya
 * hecho. Por eso un registro sin equivalencia no se puede guardar aunque tenga importe.
 */
export function viewOfMoneyField(input: MoneyFieldInput): MoneyFieldView {
  const { state, amount, conversion, display, rates } = input;
  const foreign = state.currency !== display;
  const edited = state.conversion !== null;
  const rate = foreign ? rateOf(rates, state.currency, display) : null;
  const suggested = foreign ? convert(rates, amount, state.currency, display) : null;
  const base = baseOf({ foreign, edited, amount, conversion, suggested });
  return {
    amount,
    currency: state.currency,
    foreign,
    edited,
    base,
    rate,
    suggested,
    valid: signOf(amount) > 0 && signOf(base) > 0,
  };
}

interface BaseInput {
  readonly foreign: boolean;
  readonly edited: boolean;
  readonly amount: Decimal;
  readonly conversion: Decimal | null;
  readonly suggested: Decimal | null;
}

function baseOf(input: BaseInput): Decimal {
  if (!input.foreign) return input.amount;
  if (input.edited) return input.conversion ?? ZERO;
  return input.suggested ?? ZERO;
}

/**
 * Los cuatro campos que se le mandan a la API.
 *
 * La equivalencia se redondea a los decimales de la moneda principal, que es **exactamente lo
 * que la caja ensena**: guardar 9.915.172,9819 despues de haber ensenado 9.915.173 seria
 * guardar un numero que el usuario no llego a ver.
 */
export function monetaryAmountOf(view: MoneyFieldView, display: CurrencyCode): MonetaryAmount {
  return amountOf(moneyOf(view.amount, view.currency), roundedMoney(moneyOf(view.base, display)));
}

/** Con cuantos decimales se calcula la tasa que sale de lo tecleado. */
const IMPLIED_RATE_SCALE = 10;

/**
 * La tasa que sale de lo tecleado: `base / amount`.
 *
 * Es lo que ensena la linea "Tu tasa", y no la sugerida: si el usuario escribio la
 * equivalencia a mano, la tasa que de verdad se guarda es la suya.
 */
export function impliedRate(view: MoneyFieldView): Decimal | null {
  return divideDecimals(view.base, view.amount, IMPLIED_RATE_SCALE);
}
