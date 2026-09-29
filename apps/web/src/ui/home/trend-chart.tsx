import type { ReactElement } from 'react';

import type { BalanceTrend } from '../../domain/analysis/balance-trend';
import { decimalToFixed, type Decimal } from '../../domain/money/decimal';

/** La caja del diseno. El SVG se estira en horizontal, asi que el ancho es nominal. */
const WIDTH = 310;
const HEIGHT = 64;
const INSET = 4;

const VIEW_BOX = `0 0 ${String(WIDTH)} ${String(HEIGHT)}`;

/** El cierre del area: baja al suelo por la derecha y vuelve por la izquierda. */
const CLOSE_PATH = `L${String(WIDTH)},${String(HEIGHT)} L0,${String(HEIGHT)} Z`;

/**
 * El acumulado del periodo dia a dia, en linea lime sobre un area al 12 por ciento y con la
 * linea cero punteada.
 *
 * Tres cosas que no se pueden perder: `preserveAspectRatio="none"` estira el dibujo a lo
 * ancho de la columna, asi que los dos trazos llevan `vector-effect="non-scaling-stroke"` o
 * la linea de 2px se vuelve de 4 y el punteado se deforma; la escala **incluye siempre el
 * cero**, o un periodo que solo sube dibujaria la linea cero fuera de la caja; y se pinta
 * primero la linea cero, luego el area y luego la linea, porque el area la tapa a medias a
 * proposito.
 */
export function TrendChart({ trend }: { trend: BalanceTrend }): ReactElement {
  const values = paddedValues(trend);
  const low = Math.min(0, ...values);
  const high = Math.max(0, ...values);
  const span = high - low === 0 ? 1 : high - low;
  const y = (value: number): number => INSET + (1 - (value - low) / span) * (HEIGHT - INSET * 2);
  const step = WIDTH / (values.length - 1);
  const line = values
    .map((value, index) => `${index === 0 ? 'M' : 'L'}${String(index * step)},${String(y(value))}`)
    .join(' ');
  return (
    <svg aria-hidden="true" className="g-trend" preserveAspectRatio="none" viewBox={VIEW_BOX}>
      <line
        stroke="var(--g-on-ink-line)"
        strokeDasharray="3 4"
        vectorEffect="non-scaling-stroke"
        x1={0}
        x2={WIDTH}
        y1={y(0)}
        y2={y(0)}
      />
      <path d={`${line} ${CLOSE_PATH}`} fill="var(--g-lime)" fillOpacity={0.12} />
      <path
        d={line}
        fill="none"
        stroke="var(--g-lime)"
        strokeLinejoin="round"
        strokeWidth={2}
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  );
}

/**
 * Dos puntos como minimo: con uno solo no hay linea que trazar, asi que se antepone un cero.
 *
 * Aqui si se pasa a `number`, y es correcto: esto son coordenadas de un dibujo de 310 por 64
 * puntos, no un importe. Lo que se ensena como cifra nunca pasa por aqui.
 */
function paddedValues(trend: BalanceTrend): readonly number[] {
  const values = trend.points.map((point) => toNumber(point.cumulative));
  if (values.length === 0) return [0, 0];
  return values.length === 1 ? [0, values[0] ?? 0] : values;
}

const CHART_SCALE = 2;

function toNumber(value: Decimal): number {
  return Number(decimalToFixed(value, CHART_SCALE));
}
