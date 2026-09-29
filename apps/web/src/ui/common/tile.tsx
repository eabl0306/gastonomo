import type { ReactElement } from 'react';

import { Icon } from '../auth/auth-chrome';

/**
 * El alfa del fondo de un tile de categoria: el color de la categoria al 9 por ciento.
 *
 * Va como hexadecimal de ocho digitos porque es lo que hace el diseno y porque asi el color
 * llega tal cual desde la API, sin convertir a rgb por el camino.
 */
const TINT_ALPHA = '18';

/** El cuadrado de color con el icono de una categoria. */
export function CategoryTile({
  icon,
  color,
  size,
}: {
  icon: string;
  color: string;
  size: 'small' | 'large';
}): ReactElement {
  return (
    <span className="g-tile" data-size={size} style={{ background: color + TINT_ALPHA, color }}>
      <Icon name={icon} />
    </span>
  );
}
