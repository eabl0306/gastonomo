import { failure, success, type Outcome } from '../../application/ports/api-error';
import type { SessionStore } from '../../application/ports/session-store';

import { toApiError } from './api-error-mapping';
import { createTokenSource, type RotatedTokens, type TokenSource } from './api-tokens';

export interface ApiClientOptions {
  readonly baseUrl: string;
  readonly session: SessionStore;
  readonly refresh: (refreshToken: string) => Promise<RotatedTokens | null>;
  readonly timeoutMs?: number;
  readonly fetchImpl?: typeof fetch;
}

export interface RequestOptions {
  readonly method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  readonly body?: unknown;
  readonly query?: Readonly<Record<string, string | number | undefined>>;
  /** Las rutas de autenticacion, donde un 401 es el resultado y no una sesion caducada. */
  readonly anonymous?: boolean;
  /** La cabecera `Idempotency-Key` de los tres POST que crean dinero. */
  readonly idempotencyKey?: string;
}

/** Lo que devuelve una llamada: el cuerpo ya leido y si fue un reintento idempotente. */
export interface ApiResponse {
  readonly body: unknown;
  readonly isReplay: boolean;
}

/**
 * El cliente que usan todos los adaptadores. Hace tres cosas que ninguno tiene que repetir:
 *
 * 1. Pone el `Bearer` y, ante un 401, refresca **una sola vez** y reintenta.
 * 2. Traduce `problem+json` a un error del nucleo, con sus mensajes por campo.
 * 3. Manda `Idempotency-Key` y lee la cabecera `Idempotent-Replay`.
 *
 * Va al mismo origen que la web: la API no manda cabeceras CORS a proposito, y en
 * produccion Caddy sirve `/v1/` bajo el dominio de la web.
 */
export interface ApiClient {
  request: (path: string, options?: RequestOptions) => Promise<Outcome<ApiResponse>>;
}

const DEFAULT_TIMEOUT_MS = 15_000;
const UNAUTHORIZED = 401;
const NO_CONTENT = 204;

function searchOf(query: RequestOptions['query']): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) params.set(key, String(value));
  }
  const text = params.toString();
  return text === '' ? '' : `?${text}`;
}

function headersOf(request: RequestOptions, token: string | null): HeadersInit {
  const headers: Record<string, string> = {
    accept: 'application/json, application/problem+json',
  };
  if (request.body !== undefined) headers['content-type'] = 'application/json';
  if (token !== null) headers.authorization = `Bearer ${token}`;
  if (request.idempotencyKey !== undefined) headers['idempotency-key'] = request.idempotencyKey;
  return headers;
}

async function readBody(response: Response): Promise<unknown> {
  if (response.status === NO_CONTENT) return null;
  try {
    return (await response.json()) as unknown;
  } catch {
    return null;
  }
}

async function finish(response: Response, anonymous: boolean): Promise<Outcome<ApiResponse>> {
  const body = await readBody(response);
  if (!response.ok) {
    return failure(
      toApiError({ status: response.status, headers: response.headers, body, anonymous }),
    );
  }
  return success({ body, isReplay: response.headers.get('idempotent-replay') === 'true' });
}

interface Transport {
  execute: (
    path: string,
    request: RequestOptions,
    token: string | null,
  ) => Promise<Response | null>;
}

/** Una peticion que no llega devuelve null: sin red, DNS o tiempo agotado. */
function createTransport(options: ApiClientOptions): Transport {
  const call = options.fetchImpl ?? globalThis.fetch;
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const base = options.baseUrl.replace(/\/+$/u, '');
  return {
    execute: async (path, request, token) => {
      const url = `${base}/v1/${path.replace(/^\/+/u, '')}${searchOf(request.query)}`;
      try {
        return await call(url, {
          method: request.method ?? 'GET',
          headers: headersOf(request, token),
          ...(request.body === undefined ? {} : { body: JSON.stringify(request.body) }),
          ...(typeof AbortSignal.timeout === 'function'
            ? { signal: AbortSignal.timeout(timeoutMs) }
            : {}),
        });
      } catch {
        return null;
      }
    },
  };
}

async function send(
  transport: Transport,
  tokens: TokenSource,
  path: string,
  request: RequestOptions,
): Promise<Outcome<ApiResponse>> {
  const anonymous = request.anonymous ?? false;
  const token = anonymous ? null : await tokens.accessToken(null);
  const first = await transport.execute(path, request, token);
  if (first === null) return failure({ kind: 'network' });
  // Sin Bearer, el 401 es la respuesta del propio endpoint y no una sesion caducada:
  // refrescar ahi taparia el error que la pantalla tiene que ensenar.
  if (first.status !== UNAUTHORIZED || token === null) return finish(first, anonymous);
  const renewed = await tokens.accessToken(token);
  if (renewed === null) return failure({ kind: 'unauthorized' });
  const second = await transport.execute(path, request, renewed);
  return second === null ? failure({ kind: 'network' }) : finish(second, anonymous);
}

export function createApiClient(options: ApiClientOptions): ApiClient {
  const transport = createTransport(options);
  const tokens = createTokenSource({ session: options.session, refresh: options.refresh });
  return { request: async (path, request = {}) => send(transport, tokens, path, request) };
}
