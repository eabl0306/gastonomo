import { describe, expect, it } from 'vitest';

import { monthPeriod, rangePeriod } from '../calendar/period';
import type { Category } from '../ledger/category';
import type { Movement } from '../ledger/movement';
import { COP, USD } from '../money/currency';
import { decimalOf, decimalToString } from '../money/decimal';
import type { ExchangeRates } from '../money/exchange-rates';
import { amountOf, singleAmount } from '../money/monetary-amount';
import { moneyOf } from '../money/money';

import { balanceTrend, isEmptyTrend, trendMaximum, trendMinimum } from './balance-trend';
import { TOP_SLICES, breakdownOfExpenses } from './category-breakdown';
import { groupByDay } from './day-group';
import { summarizePeriod, totalIn } from './period-summary';

const RATES: ExchangeRates = {
  base: USD,
  date: '2026-09-22',
  status: 'ok',
  rates: new Map([
    [USD, decimalOf('1')],
    [COP, decimalOf('3199.41')],
  ]),
};

function category(name: string, id = name.toLowerCase()): Category {
  return {
    id,
    name,
    kind: 'out',
    icon: 'restaurant',
    color: '#cf5f35',
    isDefault: true,
    movementCount: 0,
    archived: false,
  };
}

function movement(
  id: string,
  occurredOn: string,
  kind: 'in' | 'out',
  amount: string,
  categoryName = 'Alimentación',
): Movement {
  return {
    id,
    occurredOn,
    kind,
    title: categoryName,
    money: singleAmount(decimalOf(amount), COP),
    category: category(categoryName),
    loanId: null,
    loanPaymentId: null,
  };
}

describe('period summary', () => {
  it('separa ingresos de gastos y saca el neto', () => {
    const summary = summarizePeriod(
      [
        movement('1', '2026-09-01', 'in', '3000000'),
        movement('2', '2026-09-02', 'out', '186400'),
        movement('3', '2026-09-03', 'out', '13600'),
      ],
      COP,
      RATES,
    );
    expect(decimalToString(summary.income.amount)).toBe('3000000');
    expect(decimalToString(summary.expense.amount)).toBe('200000');
    expect(decimalToString(summary.net.amount)).toBe('2800000');
  });

  it('sin movimientos todo es cero', () => {
    const summary = summarizePeriod([], COP, RATES);
    expect(decimalToString(summary.net.amount)).toBe('0');
    expect(decimalToString(totalIn([], COP, RATES).amount)).toBe('0');
  });

  it('un movimiento en otra moneda entra por su equivalencia guardada', () => {
    const foreign: Movement = {
      ...movement('1', '2026-09-01', 'out', '0'),
      money: amountOf(moneyOf(decimalOf('140'), USD), moneyOf(decimalOf('447917'), COP)),
    };
    expect(decimalToString(summarizePeriod([foreign], COP, RATES).expense.amount)).toBe('447917');
  });
});

describe('day grouping', () => {
  it('agrupa por dia, del mas reciente al mas antiguo, con su neto', () => {
    const groups = groupByDay(
      [
        movement('1', '2026-09-01', 'out', '1000'),
        movement('2', '2026-09-03', 'in', '5000'),
        movement('3', '2026-09-03', 'out', '2000'),
      ],
      COP,
      RATES,
    );
    expect(groups.map((group) => group.date)).toEqual(['2026-09-03', '2026-09-01']);
    expect(decimalToString(groups[0]!.net.amount)).toBe('3000');
    expect(groups[0]!.movements).toHaveLength(2);
  });
});

describe('category breakdown', () => {
  it('ordena las categorias por gasto y saca su porcentaje', () => {
    const breakdown = breakdownOfExpenses(
      [
        movement('1', '2026-09-01', 'out', '75000', 'Alimentación'),
        movement('2', '2026-09-02', 'out', '25000', 'Transporte'),
        // Un ingreso no cuenta como gasto.
        movement('3', '2026-09-02', 'in', '900000', 'Salario'),
      ],
      COP,
      RATES,
    );
    expect(decimalToString(breakdown.total.amount)).toBe('100000');
    expect(breakdown.slices.map((slice) => slice.category.name)).toEqual([
      'Alimentación',
      'Transporte',
    ]);
    expect(breakdown.slices[0]!.percent).toBe(75);
    expect(breakdown.slices[0]!.fraction).toBeCloseTo(0.75, 5);
  });

  it('sin gastos no hay porciones', () => {
    const breakdown = breakdownOfExpenses([movement('1', '2026-09-01', 'in', '100')], COP, RATES);
    expect(breakdown.slices).toEqual([]);
  });

  it('la barra apilada ensena como mucho seis categorias', () => {
    const many = Array.from({ length: 9 }, (_, index) =>
      movement(
        String(index),
        '2026-09-01',
        'out',
        String((index + 1) * 1000),
        `Cat${String(index)}`,
      ),
    );
    expect(breakdownOfExpenses(many, COP, RATES).slices).toHaveLength(TOP_SLICES);
  });
});

describe('balance trend', () => {
  it('acumula desde cero, un punto por dia, y corta en hoy', () => {
    const trend = balanceTrend({
      movements: [
        movement('1', '2026-09-01', 'in', '1000'),
        movement('2', '2026-09-03', 'out', '400'),
      ],
      period: monthPeriod('2026-09'),
      today: '2026-09-04',
      display: COP,
      rates: RATES,
    });
    expect(trend.points).toHaveLength(4);
    expect(trend.points.map((point) => decimalToString(point.cumulative))).toEqual([
      '1000',
      '1000',
      '600',
      '600',
    ]);
    expect(decimalToString(trendMaximum(trend))).toBe('1000');
    expect(decimalToString(trendMinimum(trend))).toBe('600');
  });

  it('un periodo que todavia no ha empezado no tiene puntos', () => {
    const trend = balanceTrend({
      movements: [],
      period: rangePeriod('2026-10-01', '2026-10-31'),
      today: '2026-09-22',
      display: COP,
      rates: RATES,
    });
    expect(isEmptyTrend(trend)).toBe(true);
  });
});
