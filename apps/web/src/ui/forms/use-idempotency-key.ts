import { useState } from 'react';

/**
 * La clave de idempotencia del formulario.
 *
 * Se genera **una vez y no se regenera nunca**, ni al reintentar: un reintento con clave
 * nueva tras un tiempo agotado que si llego dejaria el gasto dos veces, y no hay pantalla
 * para borrarlo. Cambia solo cuando el formulario se abre otra vez.
 */
export function useIdempotencyKey(): string {
  const [key] = useState(newKey);
  return key;
}

const RADIX = 36;
const RANDOM_LENGTH = 16;

function newKey(): string {
  const random = globalThis.crypto as { randomUUID?: () => string } | undefined;
  if (typeof random?.randomUUID === 'function') return random.randomUUID();
  // Sin `crypto.randomUUID` basta con que sea distinta de la del formulario anterior: la
  // clave solo tiene que identificar este intento dentro de esta cuenta.
  return Array.from({ length: RANDOM_LENGTH }, () =>
    Math.floor(Math.random() * RADIX).toString(RADIX),
  ).join('');
}
