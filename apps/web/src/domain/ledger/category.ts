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
export type CategoryKind = 'in' | 'out' | 'both';

/** Ingreso o gasto. Es el valor de `kind` en la API y el de las pills de Movimientos. */
export type MovementKind = 'in' | 'out';

export function isIncome(kind: MovementKind): boolean {
  return kind === 'in';
}

/** True si esta categoria se puede ofrecer para un movimiento de tipo [kind]. */
export function categoryAccepts(category: CategoryKind, kind: MovementKind): boolean {
  return category === 'both' || category === kind;
}

/** True si el usuario puede elegir este tipo al crear una categoria. */
export function isSelectableByUsers(kind: CategoryKind): boolean {
  return kind !== 'both';
}

/** Maximo de caracteres del nombre, el mismo que valida la API. */
export const MAX_CATEGORY_NAME_LENGTH = 22;

/** Los 16 iconos del selector de categoria, en el orden del grid de 8x2. */
export const CATEGORY_ICONS: readonly string[] = (
  'shopping_cart pets fitness_center local_cafe flight checkroom child_care ' +
  'sports_esports card_giftcard local_bar spa build savings subscriptions ' +
  'local_gas_station volunteer_activism'
).split(' ');

/** Los 8 colores del selector de categoria, en orden. */
export const CATEGORY_COLORS: readonly string[] =
  '#cf5f35 #c48f12 #2f8a55 #2f7fb5 #3f6fc4 #7a52c7 #b84a86 #5b6573'.split(' ');

/**
 * La predeterminada que recoge lo que no cabe en otra.
 *
 * Ya no es a donde van los movimientos al borrar su categoria: borrar **archiva**, y un
 * movimiento nunca cambia de categoria. Se queda porque es una de las 12 y sale en los dos
 * grids.
 */
export const FALLBACK_CATEGORY_NAME = 'Otros';

/** El tinte del tile de una categoria: su color al 9 por ciento. */
export function categoryTint(color: string): string {
  return `${color}18`;
}
