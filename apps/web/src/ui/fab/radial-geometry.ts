/** Una opcion del menu radial: a donde va, hacia donde apunta y como se llama. */
export interface RadialOption {
  readonly key: string;
  readonly label: string;
  readonly icon: string;
  /** Desplazamiento en pixeles desde el centro del boton. */
  readonly x: number;
  readonly y: number;
  /** El mismo desplazamiento normalizado, que es contra lo que se compara el dedo. */
  readonly ux: number;
  readonly uy: number;
}

const DIAGONAL = 0.7071;

/**
 * Las tres opciones, en arco de un cuarto: arriba, arriba a la izquierda, y a la izquierda.
 *
 * El boton vive en la esquina inferior derecha, asi que el arco se abre hacia dentro de la
 * pantalla; en la otra direccion las opciones caerian fuera.
 */
export const RADIAL_OPTIONS: readonly RadialOption[] = [
  { key: 'movement', label: 'Nuevo movimiento', icon: 'swap_vert', x: 0, y: -132, ux: 0, uy: -1 },
  {
    key: 'loan',
    label: 'Nuevo préstamo',
    icon: 'handshake',
    x: -93,
    y: -93,
    ux: -DIAGONAL,
    uy: -DIAGONAL,
  },
  { key: 'dictate', label: 'Dictar', icon: 'mic', x: -132, y: 0, ux: -1, uy: 0 },
];

/** A partir de cuanto se considera que el dedo se movio y no fue un toque. */
const MOVED_DISTANCE = 14;

/**
 * A partir de cuanto empieza a elegirse una opcion.
 *
 * **No es la distancia a la que se dibujan** (132 px): tener que llegar hasta la opcion para
 * elegirla haria el gesto imposible en una pantalla pequena.
 */
const SELECTION_DISTANCE = 46;

/** Cuanto tiene que apuntar el dedo hacia una opcion para que cuente. */
const SELECTION_COSINE = 0.8;

export function movedEnough(dx: number, dy: number): boolean {
  return Math.hypot(dx, dy) > MOVED_DISTANCE;
}

/**
 * La opcion hacia la que apunta el dedo, o null si no apunta a ninguna.
 *
 * Se elige por angulo y no por cercania: el dedo no tiene que llegar encima de la opcion, solo
 * ir en su direccion.
 */
export function optionAt(dx: number, dy: number): RadialOption | null {
  const distance = Math.hypot(dx, dy);
  if (distance <= SELECTION_DISTANCE) return null;
  let best: RadialOption | null = null;
  let bestDot = -1;
  for (const option of RADIAL_OPTIONS) {
    const dot = (dx * option.ux + dy * option.uy) / distance;
    if (dot > bestDot) {
      bestDot = dot;
      best = option;
    }
  }
  return bestDot < SELECTION_COSINE ? null : best;
}
