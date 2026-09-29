import type { ReactElement } from 'react';

import { MASK_SHORT, MINUS, PLUS } from '../format/formats';

/**
 * Un importe ya formateado.
 *
 * Existe por dos cosas que se pierden si cada pantalla escribe un `<span>`: las cifras
 * tabulares, sin las que las columnas bailan al cambiar de valor, y el nombre accesible. Un
 * lector de pantalla lee el menos U+2212 como nada o como "signo menos" segun el motor, y
 * los puntos de la mascara como seis vinetas.
 */
export function Amount({ text, className }: { text: string; className?: string }): ReactElement {
  return (
    <span aria-label={spokenOf(text)} className={className} data-amount="">
      {text}
    </span>
  );
}

function spokenOf(text: string): string {
  if (text.includes(MASK_SHORT)) return 'Importe oculto';
  if (text.startsWith(MINUS)) return `menos ${text.slice(MINUS.length)}`;
  if (text.startsWith(PLUS)) return `más ${text.slice(PLUS.length)}`;
  return text;
}
