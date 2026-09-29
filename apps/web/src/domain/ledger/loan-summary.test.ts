import { describe, expect, it } from 'vitest';

import { COP, USD } from '../money/currency';
import { decimalOf, decimalToString } from '../money/decimal';
import type { ExchangeRates } from '../money/exchange-rates';
import { singleAmount, amountOf } from '../money/monetary-amount';
import { moneyOf } from '../money/money';

import type { Loan, LoanPayment } from './loan';
import { isOpen, loanTotals, sortedLoans, summarize, type LoanRow } from './loan-summary';

const RATES: ExchangeRates = {
  base: USD,
  date: '2026-09-22',
  status: 'ok',
  rates: new Map([
    [USD, decimalOf('1')],
    [COP, decimalOf('3199.41')],
  ]),
};

function payment(amount: string, id = 'p1'): LoanPayment {
  return {
    id,
    loanId: 'l1',
    paidOn: '2026-09-19',
    label: null,
    money: singleAmount(decimalOf(amount), COP),
  };
}

function loan(amount: string, payments: readonly LoanPayment[] = [], id = 'l1'): Loan {
  return {
    id,
    person: 'Rosa',
    note: '',
    direction: 'lent',
    openedOn: '2026-06-28',
    money: singleAmount(decimalOf(amount), COP),
    payments,
  };
}

describe('loan summary', () => {
  it('sin abonos esta pendiente y debe el total', () => {
    const summary = summarize(loan('600000'), COP, RATES);
    expect(summary.status).toBe('pending');
    expect(decimalToString(summary.outstanding.amount)).toBe('600000');
    expect(summary.percent).toBe(0);
    expect(isOpen(summary.status)).toBe(true);
  });

  it('con un abono parcial dice cuanto queda y que porcentaje va', () => {
    const summary = summarize(loan('600000', [payment('200000')]), COP, RATES);
    expect(summary.status).toBe('partial');
    expect(decimalToString(summary.outstanding.amount)).toBe('400000');
    expect(summary.percent).toBe(33);
  });

  it('pagado del todo deja el saldo en cero', () => {
    const summary = summarize(loan('600000', [payment('600000')]), COP, RATES);
    expect(summary.status).toBe('paid');
    expect(decimalToString(summary.outstanding.amount)).toBe('0');
    expect(summary.percent).toBe(100);
    expect(isOpen(summary.status)).toBe(false);
  });

  /**
   * Sin el margen, una conversion deja un saldo de 0,0001 y un prestamo pagado sale como
   * Parcial para siempre.
   */
  it('un resto menor que el margen de la moneda cuenta como pagado', () => {
    const almost = loan('600000', [payment('599999.6')]);
    expect(summarize(almost, COP, RATES).status).toBe('paid');
  });

  it('abonar de mas son intereses ganados, no un saldo negativo', () => {
    const summary = summarize(loan('600000', [payment('700000')]), COP, RATES);
    expect(summary.status).toBe('paid');
    expect(decimalToString(summary.outstanding.amount)).toBe('0');
    expect(decimalToString(summary.interest.amount)).toBe('100000');
    // La barra no se pasa de 100 aunque lo abonado si.
    expect(summary.percent).toBe(100);
  });

  it('un prestamo en otra moneda se resume con las tasas de hoy', () => {
    const foreign: Loan = {
      ...loan('0'),
      money: amountOf(moneyOf(decimalOf('100'), USD), moneyOf(decimalOf('300000'), COP)),
      payments: [],
    };
    const summary = summarize(foreign, COP, RATES);
    // La equivalencia guardada ya esta en la moneda que se pinta: se usa tal cual.
    expect(decimalToString(summary.total.amount)).toBe('300000');
  });
});

describe('loan totals', () => {
  const rows: LoanRow[] = [
    {
      loan: loan('600000', [payment('200000')], 'l1'),
      summary: summarize(loan('600000', [payment('200000')]), COP, RATES),
    },
    {
      loan: loan('100000', [payment('100000')], 'l2'),
      summary: summarize(loan('100000', [payment('100000')]), COP, RATES),
    },
  ];

  it('solo suman los prestamos abiertos', () => {
    const totals = loanTotals(rows, 'lent', COP);
    expect(decimalToString(totals.outstanding.amount)).toBe('400000');
    expect(totals.openCount).toBe(1);
  });

  it('la otra direccion no suma nada', () => {
    const totals = loanTotals(rows, 'borrowed', COP);
    expect(totals.openCount).toBe(0);
    expect(decimalToString(totals.outstanding.amount)).toBe('0');
  });
});

describe('loan order', () => {
  it('los abiertos van primero y los pagados al final', () => {
    const open = {
      loan: loan('600000', [], 'abierto'),
      summary: summarize(loan('600000'), COP, RATES),
    };
    const paid = {
      loan: loan('100000', [payment('100000')], 'pagado'),
      summary: summarize(loan('100000', [payment('100000')]), COP, RATES),
    };
    expect(sortedLoans([paid, open]).map((row) => row.loan.id)).toEqual(['abierto', 'pagado']);
  });
});
