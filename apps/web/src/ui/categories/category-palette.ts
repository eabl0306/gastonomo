/**
 * Los 16 iconos y los 8 colores entre los que se elige al crear una categoria.
 *
 * Son los del diseno. No se ofrece cualquier icono de Material Symbols: la fuente que sirve
 * la web esta recortada a los que usa el producto, asi que uno de fuera saldria en blanco.
 */
export const CATEGORY_ICONS: readonly string[] = [
  'shopping_cart',
  'pets',
  'fitness_center',
  'local_cafe',
  'flight',
  'checkroom',
  'child_care',
  'sports_esports',
  'card_giftcard',
  'local_bar',
  'spa',
  'build',
  'savings',
  'subscriptions',
  'local_gas_station',
  'volunteer_activism',
];

export const CATEGORY_COLORS: readonly string[] = [
  '#cf5f35',
  '#c48f12',
  '#2f8a55',
  '#2f7fb5',
  '#3f6fc4',
  '#7a52c7',
  '#b84a86',
  '#5b6573',
];

/** El icono y el color con los que arranca una categoria nueva. */
export const DEFAULT_ICON = 'shopping_cart';
export const DEFAULT_COLOR = '#cf5f35';

/** Cuantos caracteres cabe un nombre de categoria. */
export const MAX_CATEGORY_NAME = 22;

/** "Sin movimientos", "1 movimiento" o "N movimientos". El cero no es un plural. */
export function usesLabel(count: number): string {
  if (count === 0) return 'Sin movimientos';
  return count === 1 ? '1 movimiento' : `${String(count)} movimientos`;
}
