import { z } from 'zod';

import { parseDay } from '../../domain/calendar/day';
import type { DictationDraft } from '../../domain/dictation/draft';
import { parseCurrency } from '../../domain/money/currency';
import { parseDecimal } from '../../domain/money/decimal';

const categorySchema = z.object({ id: z.string(), name: z.string() });

const movementSchema = z.object({
  kind: z.enum(['in', 'out']),
  category: categorySchema,
  note: z.string(),
});

const loanSchema = z.object({
  direction: z.enum(['lent', 'borrowed']),
  person: z.string(),
  note: z.string(),
});

const draftSchema = z.object({
  kind: z.enum(['movement', 'loan']),
  movement: movementSchema.nullish(),
  loan: loanSchema.nullish(),
  amount: z.string().nullish(),
  currency: z.string(),
  occurredOn: z.string(),
  missing: z.array(z.string()),
});

/**
 * Lee el borrador.
 *
 * Devuelve null si `kind` dice una cosa y el objeto que viene relleno es otra: un borrador a
 * medias no se puede revisar, y ensenar la mitad seria peor que decir que no se entendio.
 */
export function readDraft(body: unknown): DictationDraft | null {
  const parsed = draftSchema.safeParse(body);
  if (!parsed.success) return null;
  const currency = parseCurrency(parsed.data.currency);
  const occurredOn = parseDay(parsed.data.occurredOn);
  const entry = entryOf(parsed.data);
  if (currency === null || occurredOn === null || entry === null) return null;
  return {
    entry,
    amount: parsed.data.amount == null ? null : parseDecimal(parsed.data.amount),
    currency,
    occurredOn,
    missing: parsed.data.missing,
  };
}

function entryOf(data: z.infer<typeof draftSchema>): DictationDraft['entry'] | null {
  if (data.kind === 'movement') {
    return data.movement == null ? null : { kind: 'movement', movement: data.movement };
  }
  return data.loan == null ? null : { kind: 'loan', loan: data.loan };
}
