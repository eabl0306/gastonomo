import { z } from 'zod';

import type { ApiError, FieldError } from '../../application/ports/api-error';

/**
 * El cuerpo de error de la API, RFC 9457.
 *
 * Todo es opcional a proposito: un cuerpo raro tiene que poder ignorarse solo en vez de
 * tumbar la pantalla, y el codigo de estado ya dice lo esencial.
 */
const problemSchema = z.object({
  title: z.string().optional(),
  detail: z.string().optional(),
  code: z.string().optional(),
  errors: z
    .array(z.object({ field: z.string().optional(), message: z.string().optional() }))
    .optional(),
});

type Problem = z.infer<typeof problemSchema>;

const UNAUTHORIZED = 401;
const NOT_FOUND = 404;
const CONFLICT = 409;
const UNPROCESSABLE = 422;
const TOO_MANY = 429;
const SERVICE_UNAVAILABLE = 503;

function fieldsOf(problem: Problem): readonly FieldError[] {
  return (problem.errors ?? [])
    .filter((entry) => (entry.message ?? '').trim() !== '')
    .map((entry) => ({ field: entry.field ?? '', message: entry.message ?? '' }));
}

function detailOf(problem: Problem): string | null {
  const detail = problem.detail ?? problem.title ?? '';
  return detail.trim() === '' ? null : detail;
}

/** El `Retry-After`, que la API manda en segundos. */
function retryAfterOf(headers: Headers): number | null {
  const raw = headers.get('retry-after');
  if (raw === null) return null;
  const seconds = Number(raw);
  return Number.isFinite(seconds) ? seconds : null;
}

export interface ErrorContext {
  readonly status: number;
  readonly headers: Headers;
  readonly body: unknown;
  /**
   * True en login, registro y recuperar contrasena. Ahi un 401 significa "credenciales
   * incorrectas" y no "sesion caducada", que es un error muy distinto para quien lo lee.
   */
  readonly anonymous: boolean;
}

/** Traduce la respuesta de error a lo que el nucleo entiende. */
export function toApiError(context: ErrorContext): ApiError {
  const parsed = problemSchema.safeParse(context.body);
  const problem: Problem = parsed.success ? parsed.data : {};
  switch (context.status) {
    case UNAUTHORIZED:
      return context.anonymous ? { kind: 'invalid-credentials' } : { kind: 'unauthorized' };
    case NOT_FOUND:
      return { kind: 'not-found' };
    case CONFLICT:
      return { kind: 'conflict', code: problem.code ?? null, detail: detailOf(problem) };
    case UNPROCESSABLE:
      return { kind: 'validation', fields: fieldsOf(problem), detail: detailOf(problem) };
    case TOO_MANY:
      return { kind: 'too-many-requests', retryAfterSeconds: retryAfterOf(context.headers) };
    case SERVICE_UNAVAILABLE:
      return { kind: 'service-unavailable' };
    default:
      return { kind: 'unexpected', status: context.status, detail: detailOf(problem) };
  }
}
