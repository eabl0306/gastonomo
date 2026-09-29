import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import type { DictationDraft } from '../../domain/dictation/draft';
import type { Category } from '../../domain/ledger/category';
import type { Loan } from '../../domain/ledger/loan';
import type { Movement } from '../../domain/ledger/movement';
import { COP, USD, currencyOf } from '../../domain/money/currency';
import { decimalOf } from '../../domain/money/decimal';
import type { ExchangeRates } from '../../domain/money/exchange-rates';
import { singleAmount } from '../../domain/money/monetary-amount';

/** El dia de hoy de los datos de muestra. Es el del prototipo, para que cuadren las fechas. */
export const SAMPLE_TODAY = '2026-09-22';

export const SAMPLE_CATEGORIES: readonly Category[] = [
  {
    id: 'c1',
    name: 'Alimentación',
    kind: 'out',
    icon: 'restaurant',
    color: '#cf5f35',
    isDefault: true,
    movementCount: 2,
    archived: false,
  },
  {
    id: 'c2',
    name: 'Transporte',
    kind: 'out',
    icon: 'directions_bus',
    color: '#9a6a3a',
    isDefault: true,
    movementCount: 1,
    archived: false,
  },
  {
    id: 'c3',
    name: 'Freelance',
    kind: 'in',
    icon: 'laptop_mac',
    color: '#2f7fb5',
    isDefault: true,
    movementCount: 1,
    archived: false,
  },
];

function categoryAt(index: number): Category {
  const category = SAMPLE_CATEGORIES[index];
  if (category === undefined) throw new Error(`No hay categoria de muestra en ${String(index)}`);
  return category;
}

interface SampleMovement {
  readonly id: string;
  readonly occurredOn: string;
  readonly kind: 'in' | 'out';
  readonly title: string;
  readonly amount: string;
  readonly category: Category;
}

function movement(sample: SampleMovement): Movement {
  return {
    id: sample.id,
    occurredOn: sample.occurredOn,
    kind: sample.kind,
    title: sample.title,
    money: singleAmount(decimalOf(sample.amount), COP),
    category: sample.category,
    loanId: null,
    loanPaymentId: null,
  };
}

export const SAMPLE_MOVEMENTS: readonly Movement[] = [
  {
    // El unico en otra moneda, para que se vea el importe original en el subtitulo.
    ...movement({
      id: 'm1',
      occurredOn: '2026-09-20',
      kind: 'in',
      title: 'Diseño web',
      amount: '0',
      category: categoryAt(2),
    }),
    money: {
      amount: decimalOf('140.0000'),
      currency: USD,
      base: decimalOf('560000.0000'),
      baseCurrency: COP,
    },
  },
  movement({
    id: 'm2',
    occurredOn: '2026-09-18',
    kind: 'out',
    title: 'Mercado del mes',
    amount: '186400.0000',
    category: categoryAt(0),
  }),
  movement({
    id: 'm3',
    occurredOn: '2026-09-15',
    kind: 'out',
    title: 'Almuerzo',
    amount: '32000.0000',
    category: categoryAt(0),
  }),
  movement({
    id: 'm4',
    occurredOn: '2026-09-12',
    kind: 'out',
    title: 'Taxi',
    amount: '18500.0000',
    category: categoryAt(1),
  }),
];

export const SAMPLE_LOANS: readonly Loan[] = [
  {
    id: 'l1',
    person: 'Ana',
    note: 'Viaje',
    direction: 'lent',
    openedOn: '2026-08-10',
    money: singleAmount(decimalOf('400000.0000'), COP),
    payments: [
      {
        id: 'p1',
        loanId: 'l1',
        paidOn: '2026-09-01',
        label: null,
        money: singleAmount(decimalOf('150000.0000'), COP),
      },
    ],
  },
  {
    id: 'l2',
    person: 'Julián',
    note: '',
    direction: 'borrowed',
    openedOn: '2026-07-02',
    money: singleAmount(decimalOf('220000.0000'), COP),
    payments: [],
  },
  // Uno ya pagado, que es el que ensena "Pagado completo" y se va al final de la lista.
  {
    id: 'l3',
    person: 'Marta',
    note: 'Entradas',
    direction: 'lent',
    openedOn: '2026-06-01',
    money: singleAmount(decimalOf('90000.0000'), COP),
    payments: [
      {
        id: 'p2',
        loanId: 'l3',
        paidOn: '2026-06-20',
        label: 'Todo de una',
        money: singleAmount(decimalOf('90000.0000'), COP),
      },
    ],
  },
];

export const SAMPLE_CURRENCIES: readonly CurrencyInfo[] = [
  { code: COP, name: 'Colombian Peso', symbol: '$' },
  { code: USD, name: 'US Dollar', symbol: '$' },
  { code: currencyOf('EUR'), name: 'Euro', symbol: '€' },
];

export const SAMPLE_RATES: ExchangeRates = {
  base: USD,
  date: SAMPLE_TODAY,
  status: 'ok',
  rates: new Map([
    [USD, decimalOf('1')],
    [COP, decimalOf('4000')],
    [currencyOf('EUR'), decimalOf('0.92')],
  ]),
};

/** El borrador de muestra del dictado: "Gasté 45 mil en almuerzo". */
export const SAMPLE_DRAFT: DictationDraft = {
  entry: {
    kind: 'movement',
    movement: {
      kind: 'out',
      category: { id: 'c1', name: 'Alimentación' },
      note: 'Almuerzo',
    },
  },
  amount: decimalOf('45000.0000'),
  currency: COP,
  occurredOn: SAMPLE_TODAY,
  missing: [],
};
