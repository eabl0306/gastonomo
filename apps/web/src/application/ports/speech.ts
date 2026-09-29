/** Lo que puede fallar al dictar. Cada uno tiene su explicacion, y todos acaban en escribir. */
export type SpeechFailure =
  /** El navegador no trae reconocimiento de voz. */
  | 'unsupported'
  /** El usuario no dio permiso al microfono, o lo tiene bloqueado para este sitio. */
  | 'denied'
  /** No se oyo nada. */
  | 'no-speech'
  /** El microfono no esta disponible: lo usa otra aplicacion, o no hay. */
  | 'no-microphone'
  /** Cualquier otro fallo del reconocedor, incluido quedarse sin red. */
  | 'failed';

/** Una sesion de dictado en marcha. */
export interface SpeechSession {
  /** Termina de escuchar y resuelve con lo que se entendio. */
  stop: () => void;
  /** Corta sin devolver nada. */
  cancel: () => void;
}

export interface SpeechListener {
  /** Lo que se lleva oido, que se va corrigiendo mientras se habla. */
  onPartial: (text: string) => void;
  onFinished: (text: string) => void;
  onFailed: (failure: SpeechFailure) => void;
}

/**
 * El reconocimiento de voz.
 *
 * Lo hace el navegador y a la API solo viaja el texto: mandar audio significaria subir un
 * fichero por cada frase y guardar la voz del usuario en algun sitio.
 */
export interface SpeechToText {
  /** False cuando el navegador no lo trae. Entonces solo se ofrece escribir. */
  readonly available: boolean;
  listen: (language: string, listener: SpeechListener) => SpeechSession;
}
