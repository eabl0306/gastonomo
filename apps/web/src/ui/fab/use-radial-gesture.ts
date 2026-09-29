import { useCallback, useRef, useState, type PointerEvent } from 'react';

import { movedEnough, optionAt } from './radial-geometry';

export interface RadialGesture {
  readonly open: boolean;
  readonly hovered: string | null;
  readonly close: () => void;
  readonly toggle: () => void;
  readonly onPointerDown: (event: PointerEvent<HTMLButtonElement>) => void;
  readonly onPointerMove: (event: PointerEvent<HTMLButtonElement>) => void;
  readonly onPointerUp: () => void;
  readonly onPointerCancel: () => void;
}

/** Lo que se recuerda entre el `pointerdown` y el `pointerup`. */
interface Pressed {
  readonly centerX: number;
  readonly centerY: number;
  moved: boolean;
  wasOpen: boolean;
}

/** Hacia donde apunta el dedo, y si el gesto se ha movido lo suficiente para no ser un toque. */
function trackedBy(
  pressed: Pressed | null,
  event: PointerEvent<HTMLButtonElement>,
): { readonly hovered: string | null; readonly moved: boolean } {
  if (pressed === null) return { hovered: null, moved: false };
  const dx = event.clientX - pressed.centerX;
  const dy = event.clientY - pressed.centerY;
  return { hovered: optionAt(dx, dy)?.key ?? null, moved: movedEnough(dx, dy) };
}

function pressedFrom(event: PointerEvent<HTMLButtonElement>, wasOpen: boolean): Pressed {
  const box = event.currentTarget.getBoundingClientRect();
  return {
    centerX: box.left + box.width / 2,
    centerY: box.top + box.height / 2,
    moved: false,
    wasOpen,
  };
}

/**
 * El gesto del boton radial: mantener y deslizar hacia una opcion, o dos toques.
 *
 * El menu **no se cierra al soltar si el gesto no se movio**, que es lo que hace que el toque
 * simple lo deje abierto en vez de abrirlo y cerrarlo en el mismo movimiento.
 */
export function useRadialGesture(onChoose: (key: string) => void): RadialGesture {
  const [open, setOpen] = useState(false);
  const [hovered, setHovered] = useState<string | null>(null);
  const pressed = useRef<Pressed | null>(null);

  const close = useCallback(() => {
    setOpen(false);
    setHovered(null);
  }, []);

  const onPointerUp = useCallback(() => {
    const current = pressed.current;
    pressed.current = null;
    if (current === null) return;
    if (hovered !== null) {
      close();
      onChoose(hovered);
    } else if (current.moved || current.wasOpen) {
      close();
    }
  }, [hovered, close, onChoose]);

  return {
    open,
    hovered,
    close,
    toggle: useCallback(() => {
      setOpen((previous) => !previous);
      setHovered(null);
    }, []),
    onPointerDown: useCallback(
      (event: PointerEvent<HTMLButtonElement>) => {
        if (event.button !== 0) return;
        pressed.current = pressedFrom(event, open);
        event.currentTarget.setPointerCapture(event.pointerId);
        setOpen(true);
        setHovered(null);
      },
      [open],
    ),
    onPointerMove: useCallback((event: PointerEvent<HTMLButtonElement>) => {
      const tracked = trackedBy(pressed.current, event);
      if (tracked.moved && pressed.current !== null) pressed.current.moved = true;
      setHovered(tracked.hovered);
    }, []),
    onPointerUp,
    onPointerCancel: useCallback(() => {
      pressed.current = null;
      close();
    }, [close]),
  };
}
