import { describe, expect, it } from 'vitest';

import { COP, USD, currencyOf } from '../../domain/money/currency';
import { decimalOf, decimalToString, type Decimal } from '../../domain/money/decimal';
import { EMPTY_RATES } from '../../domain/money/exchange-rates';
import { SAMPLE_RATES } from '../../infrastructure/memory/sample-ledger';

import {
  emptyMoneyField,
  impliedRate,
  monetaryAmountOf,
  viewOfMoneyField,
  type MoneyFieldState,
} from './money-field';

function view(state: MoneyFieldState, rates = SAMPLE_RATES) {
  return viewOfMoneyField({
    state,
    amount: decimalOf(state.amount === '' ? '0' : state.amount),
    conversion: state.conversion === null ? null : decimalOf(state.conversion),
    display: COP,
    rates,
  });
}

function text(value: Decimal | null): string {
  return value === null ? 'null' : decimalToString(value);
}

describe('el campo de importe', () => {
  it('en la moneda principal no hay nada que convertir', () => {
    const shown = view({ amount: '186400', currency: COP, conversion: null });

    expect(shown.foreign).toBe(false);
    expect(text(shown.base)).toBe('186400');
    expect(shown.valid).toBe(true);
  });

  it('en otra moneda la equivalencia sale de la tasa', () => {
    const shown = view({ amount: '140', currency: USD, conversion: null });

    expect(shown.foreign).toBe(true);
    expect(shown.edited).toBe(false);
    expect(text(shown.rate)).toBe('4000.00000000000000000000');
    expect(text(shown.base)).toBe('560000.00000000000000000000');
  });

  /** La API guarda lo que quede aqui y no recalcula nada: la tasa que manda es la del usuario. */
  it('la equivalencia tecleada gana a la sugerida', () => {
    const shown = view({ amount: '140', currency: USD, conversion: '600000' });

    expect(shown.edited).toBe(true);
    expect(text(shown.base)).toBe('600000');
    expect(text(impliedRate(shown))).toBe('4285.7142857143');
  });

  /**
   * Un registro en otra moneda sin equivalencia no se puede guardar: la API lo aceptaria, pero
   * la equivalencia seria el propio importe y el balance quedaria falseado.
   */
  it('sin tasa y sin equivalencia tecleada no se puede guardar', () => {
    const shown = view({ amount: '140', currency: USD, conversion: null }, EMPTY_RATES);

    expect(shown.rate).toBeNull();
    expect(text(shown.base)).toBe('0');
    expect(shown.valid).toBe(false);
  });

  it('un importe en cero no se puede guardar', () => {
    expect(view(emptyMoneyField(COP)).valid).toBe(false);
  });

  it('una moneda sin tasa se salva escribiendo la equivalencia a mano', () => {
    const shown = view(
      { amount: '10', currency: currencyOf('NOK'), conversion: '4000' },
      SAMPLE_RATES,
    );

    expect(shown.rate).toBeNull();
    expect(shown.valid).toBe(true);
    expect(text(shown.base)).toBe('4000');
  });

  /** Guardar 9.915.172,9819 tras ensenar 9.915.173 seria guardar algo que nadie vio. */
  it('la equivalencia se guarda con los decimales de la moneda principal', () => {
    const shown = view({ amount: '45000', currency: currencyOf('EUR'), conversion: null });
    const money = monetaryAmountOf(shown, COP);

    expect(text(money.amount)).toBe('45000');
    expect(money.currency).toBe('EUR');
    expect(money.baseCurrency).toBe('COP');
    expect(text(money.base)).toBe('195652174');
  });
});
