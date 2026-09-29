import { useCallback, useEffect, useRef, useState } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import type { DictationGateway } from '../../application/ports/dictation-gateway';
import type { SpeechToText } from '../../application/ports/speech';

import { reasonOf, type DictationPhase } from './dictation-state';
import { useListening } from './use-listening';

export interface DictationPorts {
  readonly dictation: DictationGateway;
  readonly speech: SpeechToText;
}

export interface DictationState {
  readonly phase: DictationPhase;
  readonly error: ApiError | null;
  readonly canDictate: boolean;
  startListening: () => void;
  stopListening: () => void;
  toTyping: () => void;
  setText: (text: string) => void;
  parse: (text: string) => void;
}

export interface DictationInputs {
  readonly ports: DictationPorts;
  readonly today: string;
  readonly locale: string;
}

/** La fase de arranque: escuchando si se puede, y escribiendo si el navegador no lo trae. */
function initialPhase(speech: SpeechToText): DictationPhase {
  if (speech.available) return { step: 'listening', heard: '' };
  return { step: 'typing', text: '', reason: reasonOf('unsupported') };
}

function typing(text: string, reason: string | null): DictationPhase {
  return { step: 'typing', text, reason };
}

/** Lo que hace cada cosa que puede pasar mientras se escucha. */
function handlersOf(
  setPhase: (phase: DictationPhase) => void,
  parse: (text: string) => void,
): Parameters<typeof useListening>[2] {
  return {
    onPartial: (heard) => {
      setPhase({ step: 'listening', heard });
    },
    onHeard: (text) => {
      if (text === '') setPhase(typing('', reasonOf('no-speech')));
      else parse(text);
    },
    onFailed: (failure) => {
      setPhase(typing('', reasonOf(failure)));
    },
  };
}

/**
 * El dictado.
 *
 * El reconocimiento lo hace el navegador y a la API solo viaja el texto. Cualquier fallo del
 * microfono cae al modo escrito **con su explicacion**, nunca a un callejon sin salida.
 */
export function useDictation({ ports, today, locale }: DictationInputs): DictationState {
  const [phase, setPhase] = useState<DictationPhase>(() => initialPhase(ports.speech));
  const [error, setError] = useState<ApiError | null>(null);

  const parse = useCallback(
    (text: string) => {
      const trimmed = text.trim();
      if (trimmed === '') return;
      setPhase({ step: 'parsing', text: trimmed });
      setError(null);
      void ports.dictation.parse(trimmed, today).then((outcome) => {
        setPhase(
          outcome.ok
            ? { step: 'review', text: trimmed, draft: outcome.value }
            : typing(trimmed, null),
        );
        setError(outcome.ok ? null : outcome.error);
      });
    },
    [ports, today],
  );

  const listening = useListening(ports.speech, locale, handlersOf(setPhase, parse));

  // Se empieza a escuchar al abrir: quien pulsa "Dictar" ya va a hablar, y pedirle otro toque
  // para arrancar el microfono es un paso de mas.
  const started = useRef(false);
  useEffect(() => {
    if (started.current || !ports.speech.available) return;
    started.current = true;
    listening.start();
  }, [ports.speech.available, listening]);

  return {
    phase,
    error,
    canDictate: ports.speech.available,
    startListening: useCallback(() => {
      setPhase({ step: 'listening', heard: '' });
      listening.start();
    }, [listening]),
    stopListening: listening.stop,
    toTyping: useCallback(() => {
      listening.cancel();
      setPhase(typing('', null));
    }, [listening]),
    setText: useCallback((text: string) => {
      setPhase((previous) =>
        previous.step === 'typing' ? { ...previous, text } : typing(text, null),
      );
    }, []),
    parse,
  };
}
