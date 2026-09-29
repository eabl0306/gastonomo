import type { ReactElement } from 'react';

import { Icon } from '../auth/auth-chrome';

import { RADIAL_OPTIONS, type RadialOption } from './radial-geometry';
import { useRadialGesture } from './use-radial-gesture';

export interface RadialFabProps {
  /** Lo que hace cada opcion, por su clave. */
  readonly onChoose: (key: string) => void;
}

/**
 * El boton flotante con su menu en arco de tres opciones.
 *
 * Se usa de dos formas: manteniendo pulsado y deslizando hacia una opcion, o con dos toques.
 * El menu **no se cierra al soltar si el gesto no se movio**, que es lo que hace que el toque
 * simple lo deje abierto.
 */
export function RadialFab({ onChoose }: RadialFabProps): ReactElement {
  const gesture = useRadialGesture(onChoose);
  const choose = (key: string) => () => {
    gesture.close();
    onChoose(key);
  };
  return (
    <div className="g-fab" data-open={gesture.open}>
      <button
        aria-label="Cerrar el menú"
        className="g-fab__scrim"
        onClick={gesture.close}
        type="button"
      />
      <div className="g-fab__dock">
        {gesture.open ? <p className="g-fab__hint">Desliza o toca una opción</p> : null}
        {RADIAL_OPTIONS.map((option, index) => (
          <Option
            hovered={gesture.hovered === option.key}
            index={index}
            key={option.key}
            onClick={choose(option.key)}
            open={gesture.open}
            option={option}
          />
        ))}
        <button
          aria-expanded={gesture.open}
          aria-label="Nuevo registro"
          className="g-fab__button"
          onClick={(event) => {
            // `detail === 0` es un clic de teclado: ahi no hubo gesto que interpretar.
            if (event.detail === 0) gesture.toggle();
          }}
          onPointerCancel={gesture.onPointerCancel}
          onPointerDown={gesture.onPointerDown}
          onPointerMove={gesture.onPointerMove}
          onPointerUp={gesture.onPointerUp}
          type="button"
        >
          <Icon name="add" />
        </button>
      </div>
    </div>
  );
}

const STAGGER_MS = 35;
const HOVER_SCALE = 1.14;
const CLOSED_SCALE = 0.3;

function Option({
  option,
  open,
  hovered,
  index,
  onClick,
}: {
  option: RadialOption;
  open: boolean;
  hovered: boolean;
  index: number;
  onClick: () => void;
}): ReactElement {
  const scale = hovered ? HOVER_SCALE : 1;
  return (
    <button
      aria-label={option.label}
      className="g-fab__option"
      data-hovered={hovered}
      onClick={onClick}
      style={{
        transform: open
          ? `translate(${String(option.x)}px, ${String(option.y)}px) scale(${String(scale)})`
          : `translate(0, 0) scale(${String(CLOSED_SCALE)})`,
        transitionDelay: open ? `${String(index * STAGGER_MS)}ms` : '0ms',
      }}
      type="button"
    >
      <Icon filled name={option.icon} />
    </button>
  );
}
