import { z } from 'zod';

import type { MovementPage } from '../../application/ports/movement-gateway';
import { parseDay } from '../../domain/calendar/day';
import type { Category, MovementKind } from '../../domain/ledger/category';
import type { Loan, LoanDirection, LoanPayment } from '../../domain/ledger/loan';
import type { Movement } from '../../domain/ledger/movement';

import { moneyFieldsSchema, readMoney } from './money-dto';

const categorySchema = z.object({
  id: z.string(),
  name: z.string(),
  kind: z.enum(['in', 'out', 'both']),
  icon: z.string(),
  color: z.string(),
  isDefault: z.boolean().optional(),
  movementCount: z.number().optional(),
  archived: z.boolean().optional(),
});

const movementSchema = moneyFieldsSchema.extend({
  id: z.string(),
  occurredOn: z.string(),
  kind: z.enum(['in', 'out']),
  title: z.string(),
  category: categorySchema,
  loanId: z.string().nullish(),
  loanPaymentId: z.string().nullish(),
});

const movementPageSchema = z.object({
  items: z.array(z.unknown()),
  nextCursor: z.string().nullish(),
});

const paymentSchema = moneyFieldsSchema.extend({
  id: z.string(),
  loanId: z.string(),
  paidOn: z.string(),
  label: z.string().nullish(),
});

const loanSchema = moneyFieldsSchema.extend({
  id: z.string(),
  person: z.string(),
  note: z.string().optional(),
  direction: z.enum(['lent', 'borrowed']),
  openedOn: z.string(),
  payments: z.array(z.unknown()).optional(),
});

const listSchema = z.object({ items: z.array(z.unknown()) });

function readCategory(raw: z.infer<typeof categorySchema>): Category {
  return {
    id: raw.id,
    name: raw.name,
    kind: raw.kind,
    icon: raw.icon,
    color: raw.color,
    isDefault: raw.isDefault ?? false,
    movementCount: raw.movementCount ?? 0,
    archived: raw.archived ?? false,
  };
}

/**
 * Un movimiento, o null si el cuerpo no cuadra.
 *
 * Una fila ilegible se descarta en vez de tumbar la pantalla: el resto del periodo sigue
 * sirviendo, y un balance al que le falta un movimiento se nota antes que una pantalla en
 * blanco. Lo que **no** se hace es rellenarla con ceros.
 */
export function readMovement(raw: unknown): Movement | null {
  const parsed = movementSchema.safeParse(raw);
  if (!parsed.success) return null;
  const money = readMoney(parsed.data);
  const occurredOn = parseDay(parsed.data.occurredOn);
  if (money === null || occurredOn === null) return null;
  const kind: MovementKind = parsed.data.kind;
  return {
    id: parsed.data.id,
    occurredOn,
    kind,
    title: parsed.data.title,
    money,
    category: readCategory(parsed.data.category),
    loanId: parsed.data.loanId ?? null,
    loanPaymentId: parsed.data.loanPaymentId ?? null,
  };
}

export function readMovementPage(body: unknown): MovementPage | null {
  const parsed = movementPageSchema.safeParse(body);
  if (!parsed.success) return null;
  return {
    items: parsed.data.items.map(readMovement).filter((item) => item !== null),
    nextCursor: parsed.data.nextCursor ?? null,
  };
}

/** Un abono, tal como viaja dentro de su prestamo. Trae su `loanId` repetido. */
function readPayment(raw: unknown): LoanPayment | null {
  const parsed = paymentSchema.safeParse(raw);
  if (!parsed.success) return null;
  const money = readMoney(parsed.data);
  const paidOn = parseDay(parsed.data.paidOn);
  if (money === null || paidOn === null) return null;
  return {
    id: parsed.data.id,
    loanId: parsed.data.loanId,
    paidOn,
    label: parsed.data.label ?? null,
    money,
  };
}

export function readLoan(raw: unknown): Loan | null {
  const parsed = loanSchema.safeParse(raw);
  if (!parsed.success) return null;
  const money = readMoney(parsed.data);
  const openedOn = parseDay(parsed.data.openedOn);
  if (money === null || openedOn === null) return null;
  const direction: LoanDirection = parsed.data.direction;
  return {
    id: parsed.data.id,
    person: parsed.data.person,
    note: parsed.data.note ?? '',
    direction,
    openedOn,
    money,
    payments: (parsed.data.payments ?? []).map(readPayment).filter((payment) => payment !== null),
  };
}

export function readLoanList(body: unknown): readonly Loan[] | null {
  const parsed = listSchema.safeParse(body);
  if (!parsed.success) return null;
  return parsed.data.items.map(readLoan).filter((loan) => loan !== null);
}

/** Una categoria suelta, la de `POST` y `PATCH /v1/categories`. */
export function readCategoryBody(raw: unknown): Category | null {
  const parsed = categorySchema.safeParse(raw);
  return parsed.success ? readCategory(parsed.data) : null;
}

export function readCategoryList(body: unknown): readonly Category[] | null {
  const parsed = listSchema.safeParse(body);
  if (!parsed.success) return null;
  return parsed.data.items.map(readCategoryBody).filter((category) => category !== null);
}
