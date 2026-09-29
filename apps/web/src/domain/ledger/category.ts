/**
 * Una categoria de la cuenta.
 *
 * Cada movimiento la trae completa, asi que la fila de la lista pinta su tile sin cruzar
 * nada con otra peticion.
 */
export interface Category {
  readonly id: string;
  readonly name: string;
  readonly kind: CategoryKind;
  /** Nombre de un Material Symbol, el mismo que usa el prototipo. */
  readonly icon: string;
  /** Color del tile, en `#rrggbb`. */
  readonly color: string;
  /** Las 12 predeterminadas: no se pueden editar ni borrar. */
  readonly isDefault: boolean;
  /** Alimenta el texto "Sin movimientos / 1 movimiento / N movimientos". */
  readonly movementCount: number;
  /** Ya no se ofrece al registrar, pero sus movimientos la siguen usando. */
  readonly archived: boolean;
}

/**
 * Tipo de una categoria.
 *
 * `both` existe porque tres predeterminadas (Prestamo, Abono prestamo y Otros) salen en el
 * grid de gasto y en el de ingreso, y el nombre no se puede repetir. Esta reservado a las
 * predeterminadas: crear una categoria solo ofrece `in` y `out`.
 */
type CategoryKind = 'in' | 'out' | 'both';

/** Ingreso o gasto. Es el valor de `kind` en la API y el de las pills de Movimientos. */
export type MovementKind = 'in' | 'out';

export function isIncome(kind: MovementKind): boolean {
  return kind === 'in';
}
