import { useCallback, useEffect, useRef } from 'react';

import type { SpeechFailure, SpeechSession, SpeechToText } from '../../application/ports/speech';

export interface ListeningHandlers {
  readonly onPartial: (heard: string) => void;
  /** Lo que se entendio. Vacio si no se oyo nada. */
  readonly onHeard: (text: string) => void;
  readonly onFailed: (failure: SpeechFailure) => void;
}

export interface Listening {
  readonly start: () => void;
  readonly stop: () => void;
  readonly cancel: () => void;
}

/**
 * Una sesion de dictado, con su limpieza.
 *
 * Al desmontar se **cancela**, no se para: cerrar el modal mientras escucha tiene que soltar
 * el microfono, o el navegador deja el indicador de grabacion encendido.
 */
export function useListening(
  speech: SpeechToText,
  locale: string,
  handlers: ListeningHandlers,
): Listening {
  const session = useRef<SpeechSession | null>(null);
  // Los callbacks se leen del ref al llegar el evento y no al componer: si se capturaran, una
  // sesion empezada antes de un render avisaria a la version vieja.
  const latest = useRef(handlers);
  useEffect(() => {
    latest.current = handlers;
  });

  const cancel = useCallback(() => {
    session.current?.cancel();
    session.current = null;
  }, []);

  useEffect(() => cancel, [cancel]);

  return {
    cancel,
    start: useCallback(() => {
      session.current = speech.listen(locale, {
        onPartial: (heard) => {
          latest.current.onPartial(heard);
        },
        onFinished: (text) => {
          session.current = null;
          latest.current.onHeard(text);
        },
        onFailed: (failure) => {
          session.current = null;
          latest.current.onFailed(failure);
        },
      });
    }, [speech, locale]),
    stop: useCallback(() => {
      session.current?.stop();
    }, []),
  };
}
