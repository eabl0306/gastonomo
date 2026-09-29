/** Una entrada de la navegacion, la misma en la barra lateral y en la inferior. */
export interface NavItem {
  readonly to: string;
  readonly label: string;
  /** Nombre del Material Symbol. */
  readonly icon: string;
}

/**
 * Las cuatro pantallas principales, en el orden del diseno.
 *
 * Es una sola lista para las dos navegaciones: si un dia cambia el orden o un icono, cambia
 * en un sitio. En el prototipo tambien es un unico array.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { to: '/', label: 'Inicio', icon: 'home' },
  { to: '/movements', label: 'Movimientos', icon: 'swap_vert' },
  { to: '/loans', label: 'Préstamos', icon: 'handshake' },
  { to: '/settings', label: 'Ajustes', icon: 'settings' },
];
