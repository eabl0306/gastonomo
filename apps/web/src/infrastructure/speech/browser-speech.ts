import type {
  SpeechFailure,
  SpeechListener,
  SpeechSession,
  SpeechToText,
} from '../../application/ports/speech';

/**
 * Lo que el navegador expone, escrito a mano.
 *
 * `SpeechRecognition` no esta en los tipos del DOM porque sigue sin estandarizar: Chrome y
 * Safari lo traen como `webkitSpeechRecognition` y Firefox no lo trae.
 */
interface Recognition {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((event: RecognitionEvent) => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onend: (() => void) | null;
}

interface RecognitionEvent {
  readonly resultIndex: number;
  readonly results: ArrayLike<ArrayLike<{ transcript: string }> & { isFinal: boolean }>;
}

type RecognitionConstructor = new () => Recognition;

function constructorOf(): RecognitionConstructor | null {
  const scope = globalThis as unknown as {
    SpeechRecognition?: RecognitionConstructor;
    webkitSpeechRecognition?: RecognitionConstructor;
  };
  return scope.SpeechRecognition ?? scope.webkitSpeechRecognition ?? null;
}

/** Traduce el codigo del navegador a lo que la pantalla sabe explicar. */
function failureOf(error: string): SpeechFailure {
  if (error === 'not-allowed' || error === 'service-not-allowed') return 'denied';
  if (error === 'no-speech') return 'no-speech';
  if (error === 'audio-capture') return 'no-microphone';
  return 'failed';
}

/**
 * El reconocimiento del navegador.
 *
 * Se lee todo lo que llega, no solo lo final: el usuario tiene que ver que le estan oyendo, y
 * una pantalla quieta durante cinco segundos parece rota.
 */
export const browserSpeech: SpeechToText = {
  available: constructorOf() !== null,
  listen: (language, listener) => start(language, listener),
};

function start(language: string, listener: SpeechListener): SpeechSession {
  const Recognizer = constructorOf();
  if (Recognizer === null) {
    listener.onFailed('unsupported');
    return { stop: () => undefined, cancel: () => undefined };
  }
  const recognition = new Recognizer();
  recognition.lang = language;
  recognition.continuous = true;
  recognition.interimResults = true;
  let heard = '';
  let failed = false;

  recognition.onresult = (event) => {
    heard = transcriptOf(event, heard);
    listener.onPartial(heard);
  };
  recognition.onerror = (event) => {
    failed = true;
    listener.onFailed(failureOf(event.error));
  };
  // `onend` llega tanto al parar a mano como al cortarse solo: sin el, terminar de dictar
  // dejaria la pantalla escuchando para siempre.
  recognition.onend = () => {
    if (!failed) listener.onFinished(heard.trim());
  };
  recognition.start();

  return {
    stop: () => {
      recognition.stop();
    },
    cancel: () => {
      failed = true;
      recognition.abort();
    },
  };
}

function transcriptOf(event: RecognitionEvent, previous: string): string {
  let text = previous;
  for (let index = event.resultIndex; index < event.results.length; index += 1) {
    const result = event.results[index];
    const alternative = result?.[0];
    if (result?.isFinal === true && alternative !== undefined) text += `${alternative.transcript} `;
  }
  const last = event.results[event.results.length - 1];
  if (last?.isFinal === false) return `${text}${last[0]?.transcript ?? ''}`;
  return text;
}
