import { z } from 'zod';

/**
 * El cuerpo de error de la API, RFC 9457. Todo es opcional a proposito: un
 * cuerpo raro tiene que poder ignorarse solo en vez de tumbar la pantalla, y el
 * codigo de estado ya dice lo esencial.
 *
 * Los `errors[].message` vienen en castellano y listos para pintar junto al
 * campo; el `detail` es para el resto.
 */
const problemDtoSchema = z.object({
  title: z.string().optional(),
  detail: z.string().optional(),
  errors: z
    .array(z.object({ field: z.string().optional(), message: z.string().optional() }))
    .optional(),
});

/** El primer mensaje util del cuerpo, o `null` si no trae ninguno. */
export function readProblemMessage(payload: unknown): string | null {
  const parsed = problemDtoSchema.safeParse(payload);
  if (!parsed.success) return null;
  const field = parsed.data.errors?.find((entry) => (entry.message ?? '').trim() !== '');
  const message = field?.message ?? parsed.data.detail ?? parsed.data.title ?? '';
  return message.trim() === '' ? null : message;
}
