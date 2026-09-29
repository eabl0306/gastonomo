import { describe, expect, it } from 'vitest';

import { decimalToString } from '../../domain/money/decimal';

import { readLoan, readLoanList, readMovement, readMovementPage } from './ledger-dto';
import { readMoney, writeMoney } from './money-dto';

const CATEGORY = {
  id: 'c1',
  name: 'Alimentación',
  kind: 'out',
  icon: 'restaurant',
  color: '#cf5f35',
  isDefault: true,
  movementCount: 3,
  archived: false,
};

const MOVEMENT = {
  id: 'm1',
  occurredOn: '2026-09-22',
  kind: 'out',
  title: 'Mercado',
  amount: '186400.0000',
  currency: 'COP',
  base: '186400.0000',
  baseCurrency: 'COP',
  category: CATEGORY,
  loanId: null,
  loanPaymentId: null,
  createdAt: '2026-09-22T10:00:00Z',
};

describe('los cuatro campos de dinero', () => {
  /** Un importe que no se entiende no es un importe de cero: sumarlo falsearia el balance. */
  it('un importe ilegible devuelve null, no cero', () => {
    expect(readMoney({ amount: 'muchos', currency: 'COP' })).toBeNull();
    expect(readMoney({ amount: '10', currency: 'pesos' })).toBeNull();
    expect(readMoney({ amount: '1e3', currency: 'COP' })).toBeNull();
  });

  /** La API deja omitir los dos juntos: entonces la equivalencia es el propio importe. */
  it('sin equivalencia, la equivalencia es el propio importe', () => {
    const money = readMoney({ amount: '186400.0000', currency: 'COP' });
    expect(money?.baseCurrency).toBe('COP');
    expect(decimalToString(money?.base ?? { units: 0n, scale: 0 })).toBe('186400.0000');
  });

  it('lee y devuelve los cuatro campos sin tocar la escala', () => {
    const money = readMoney({
      amount: '140.0000',
      currency: 'USD',
      base: '560000.0000',
      baseCurrency: 'COP',
    });
    expect(money).not.toBeNull();
    expect(writeMoney(money!)).toEqual({
      amount: '140.0000',
      currency: 'USD',
      base: '560000.0000',
      baseCurrency: 'COP',
    });
  });
});

describe('leer movimientos', () => {
  it('lee un movimiento con su categoria', () => {
    const movement = readMovement(MOVEMENT);
    expect(movement?.title).toBe('Mercado');
    expect(movement?.category.name).toBe('Alimentación');
    expect(movement?.loanId).toBeNull();
  });

  it('una fecha que no es un dia deja el movimiento fuera', () => {
    expect(readMovement({ ...MOVEMENT, occurredOn: '2026-09-22T10:00:00Z' })).toBeNull();
    expect(readMovement({ ...MOVEMENT, occurredOn: '2026-02-30' })).toBeNull();
  });

  /**
   * Una fila ilegible se descarta y el resto del periodo sigue sirviendo. Un balance al que
   * le falta un movimiento se nota antes que una pantalla en blanco.
   */
  it('una fila rota no tumba la pagina', () => {
    const page = readMovementPage({
      items: [MOVEMENT, { ...MOVEMENT, id: 'm2', amount: 'nada' }],
      nextCursor: 'c2',
    });
    expect(page?.items).toHaveLength(1);
    expect(page?.nextCursor).toBe('c2');
  });

  it('sin cursor, la pagina es la ultima', () => {
    expect(readMovementPage({ items: [] })?.nextCursor).toBeNull();
    expect(readMovementPage({ nada: true })).toBeNull();
  });
});

describe('leer prestamos', () => {
  const LOAN = {
    id: 'l1',
    person: 'Ana',
    direction: 'lent',
    openedOn: '2026-08-10',
    amount: '400000.0000',
    currency: 'COP',
    payments: [
      {
        id: 'p1',
        loanId: 'l1',
        paidOn: '2026-09-01',
        label: null,
        amount: '150000.0000',
        currency: 'COP',
      },
    ],
  };

  it('lee el prestamo con sus abonos dentro', () => {
    const loans = readLoanList({ items: [LOAN] });
    expect(loans).toHaveLength(1);
    expect(loans?.[0]?.payments).toHaveLength(1);
    expect(loans?.[0]?.payments[0]?.loanId).toBe('l1');
    expect(loans?.[0]?.note).toBe('');
  });

  it('un abono ilegible se descarta y el prestamo se queda', () => {
    const loans = readLoanList({
      items: [
        {
          ...LOAN,
          payments: [{ id: 'p1', loanId: 'l1', paidOn: 'ayer', amount: '1', currency: 'COP' }],
        },
      ],
    });
    expect(loans?.[0]?.payments).toHaveLength(0);
  });

  it('un cuerpo sin items no es una lista', () => {
    expect(readLoanList({ loans: [] })).toBeNull();
  });

  /**
   * `POST /v1/loans/{id}/payments` responde con el **prestamo entero**, no con el abono: el
   * cliente necesita el saldo nuevo para repintar la tarjeta.
   */
  it('la respuesta de un abono es el prestamo con el abono dentro', () => {
    const loan = readLoan(LOAN);
    expect(loan?.payments).toHaveLength(1);
    expect(decimalToString(loan?.payments[0]?.money.amount ?? { units: 0n, scale: 0 })).toBe(
      '150000.0000',
    );
  });
});
