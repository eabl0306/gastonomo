import { describe, expect, it } from 'vitest';

import { COP } from '../../domain/money/currency';
import { decimalOf, decimalToString } from '../../domain/money/decimal';
import { singleAmount } from '../../domain/money/monetary-amount';
import { createMemorySessionStore } from '../memory/in-memory-auth';

import { createApiClient } from './api-client';
import { createHttpLoanGateway } from './http-loan-gateway';

const LOAN = {
  id: 'l1',
  person: 'Ana',
  note: 'Viaje',
  direction: 'lent',
  openedOn: '2026-08-10',
  amount: '400000.0000',
  currency: 'COP',
  base: '400000.0000',
  baseCurrency: 'COP',
  payments: [
    {
      id: 'p1',
      loanId: 'l1',
      paidOn: '2026-09-01',
      label: 'Primera cuota',
      amount: '150000.0000',
      currency: 'COP',
      base: '150000.0000',
      baseCurrency: 'COP',
    },
  ],
};

/** El cliente siempre pide con una cadena, pero `fetch` admite tres formas. */
function urlOf(input: RequestInfo | URL): string {
  if (typeof input === 'string') return input;
  return input instanceof URL ? input.href : input.url;
}

/** Lo que se le pasa a `fetch`, que es lo que estos tests miran. */
interface Sent {
  readonly url: string;
  readonly method: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly body: string;
}

function gatewayReturning(body: unknown): {
  gateway: ReturnType<typeof createHttpLoanGateway>;
  sent: Sent[];
} {
  const sent: Sent[] = [];
  const fetchImpl = (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    sent.push({
      url: urlOf(input),
      method: init?.method ?? 'GET',
      headers: (init?.headers ?? {}) as Record<string, string>,
      body: typeof init?.body === 'string' ? init.body : '',
    });
    return Promise.resolve(
      new Response(JSON.stringify(body), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      }),
    );
  };
  const client = createApiClient({
    baseUrl: 'http://api.test',
    session: createMemorySessionStore({
      accessToken: 'a1',
      refreshToken: 'r1',
      user: { id: 'u1', name: 'Ana', email: 'a@b.c' },
    }),
    refresh: () => Promise.resolve(null),
    fetchImpl,
  });
  return { gateway: createHttpLoanGateway(client), sent };
}

describe('prestamos contra la API', () => {
  /**
   * `POST /v1/loans/{id}/payments` responde con el **prestamo entero**, no con el abono: quien
   * abona necesita el saldo nuevo para repintar la tarjeta. Leerlo como si fuera un abono
   * hacia que un abono guardado bien ensenara "Algo no fue bien".
   */
  it('un abono devuelve el prestamo con el abono dentro', async () => {
    const { gateway, sent } = gatewayReturning(LOAN);

    const outcome = await gateway.addPayment(
      'l1',
      { paidOn: '2026-09-01', label: '', money: singleAmount(decimalOf('150000.0000'), COP) },
      'clave-1',
    );

    expect(outcome.ok).toBe(true);
    if (!outcome.ok) return;
    expect(outcome.value.id).toBe('l1');
    expect(outcome.value.payments).toHaveLength(1);
    expect(
      decimalToString(outcome.value.payments[0]?.money.amount ?? { units: 0n, scale: 0 }),
    ).toBe('150000.0000');
    expect(sent[0]?.url).toBe('http://api.test/v1/loans/l1/payments');
    expect(sent[0]?.method).toBe('POST');
  });

  /** Sin concepto no se manda el campo: la API distingue null de cadena vacia. */
  it('un abono sin concepto no manda el campo', async () => {
    const { gateway, sent } = gatewayReturning(LOAN);
    await gateway.addPayment(
      'l1',
      { paidOn: '2026-09-01', label: '', money: singleAmount(decimalOf('1'), COP) },
      'clave-1',
    );

    const body = JSON.parse(sent[0]?.body ?? '{}') as Record<string, unknown>;
    expect(body).not.toHaveProperty('label');
    expect(body['amount']).toBe('1');
  });

  it('la clave de idempotencia viaja en su cabecera', async () => {
    const { gateway, sent } = gatewayReturning(LOAN);
    await gateway.create(
      {
        person: 'Ana',
        note: '',
        direction: 'lent',
        openedOn: '2026-08-10',
        money: singleAmount(decimalOf('400000.0000'), COP),
      },
      'clave-2',
    );

    expect(sent[0]?.headers['idempotency-key']).toBe('clave-2');
  });
});
