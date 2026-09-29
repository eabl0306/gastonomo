/** Un error de un campo concreto, tal como lo manda un 422. */
export interface FieldError {
  readonly field: string;
  /** Ya viene en castellano desde la API, listo para pintar junto al campo. */
  readonly message: string;
}

/**
 * Lo que puede salir mal en una llamada a la API, ya traducido de `problem+json` a algo
 * que el nucleo entiende. Todos los puertos devuelven esto o su valor.
 */
export type ApiError =
  /** 422. Trae los errores por campo, que van marcados en el formulario. */
  | {
      readonly kind: 'validation';
      readonly fields: readonly FieldError[];
      readonly detail: string | null;
    }
  /** 409. El `code` dice cual de los conflictos es. */
  | { readonly kind: 'conflict'; readonly code: string | null; readonly detail: string | null }
  /** 401 de login: correo o contrasena incorrectos. */
  | { readonly kind: 'invalid-credentials' }
  /** 401 con sesion: el token no vale y el refresco no lo pudo arreglar. */
  | { readonly kind: 'unauthorized' }
  /** 404. Tambien es lo que responde un recurso de otra cuenta: nunca un 403. */
  | { readonly kind: 'not-found' }
  /** 429. Pasa en registro, login y recuperar contrasena. */
  | { readonly kind: 'too-many-requests'; readonly retryAfterSeconds: number | null }
  /**
   * 503. No es un fallo: falta un dato que llena una tarea de fondo, asi que se reintenta.
   * Pasa en `GET /v1/exchange-rates` sin ninguna pasada guardada.
   */
  | { readonly kind: 'service-unavailable' }
  /** No se pudo hablar con la API: sin red, DNS o tiempo agotado. */
  | { readonly kind: 'network' }
  /** Cualquier otra cosa, con el estado si lo hubo. */
  | { readonly kind: 'unexpected'; readonly status: number | null; readonly detail: string | null };

/** El mensaje de un campo, o null si el 422 no hablaba de el. */
export function messageFor(error: ApiError, field: string): string | null {
  if (error.kind !== 'validation') return null;
  return error.fields.find((candidate) => candidate.field === field)?.message ?? null;
}

/**
 * El resultado de todo lo que habla con la API.
 *
 * Es explicito y no una excepcion porque asi el error forma parte de la firma del puerto:
 * quien llama no puede olvidarse de que la red existe.
 */
export type Outcome<T> =
  { readonly ok: true; readonly value: T } | { readonly ok: false; readonly error: ApiError };

export function success<T>(value: T): Outcome<T> {
  return { ok: true, value };
}

export function failure<T>(error: ApiError): Outcome<T> {
  return { ok: false, error };
}

/** El valor, o null si la llamada fallo. Para cuando el fallo ya se trato en otro sitio. */
export function valueOrNull<T>(outcome: Outcome<T>): T | null {
  return outcome.ok ? outcome.value : null;
}
