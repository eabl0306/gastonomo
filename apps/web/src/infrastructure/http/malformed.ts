import { failure, type Outcome } from '../../application/ports/api-error';

/**
 * El cuerpo llego pero no tiene la forma del contrato.
 *
 * No es un fallo de red ni un error de la API. Se trata como inesperado a proposito: lo que
 * no se entiende no se puede pintar, y pintar un hueco con ceros falsearia el balance.
 */
export function malformed<T>(): Outcome<T> {
  return failure({ kind: 'unexpected', status: null, detail: null });
}
