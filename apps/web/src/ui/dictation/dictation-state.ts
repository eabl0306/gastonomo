import type { SpeechFailure } from '../../application/ports/speech';
import type { DictationDraft } from '../../domain/dictation/draft';

/** En que va el dictado. */
export type DictationPhase =
  | { readonly step: 'listening'; readonly heard: string }
  | { readonly step: 'typing'; readonly text: string; readonly reason: string | null }
  | { readonly step: 'parsing'; readonly text: string }
  | { readonly step: 'review'; readonly text: string; readonly draft: DictationDraft };

/**
 * La explicacion de cada fallo del microfono.
 *
 * **Ninguno deja sin salida**: los cinco acaban en el modo escrito con su motivo, porque quien
 * queria registrar un gasto sigue queriendo registrarlo aunque el microfono no vaya.
 */
export function reasonOf(failure: SpeechFailure): string {
  switch (failure) {
    case 'unsupported':
      return 'Tu navegador no puede dictar. Escríbelo y lo interpretamos igual.';
    case 'denied':
      return 'No diste permiso al micrófono. Puedes escribirlo.';
    case 'no-speech':
      return 'No se oyó nada. Inténtalo otra vez o escríbelo.';
    case 'no-microphone':
      return 'No encontramos el micrófono. Escríbelo y seguimos.';
    case 'failed':
      return 'El dictado falló. Escríbelo y lo interpretamos igual.';
  }
}

/** Las frases de ejemplo que se ofrecen mientras no hay nada escrito. */
export const DICTATION_EXAMPLES: readonly string[] = [
  'Gasté 45 mil en almuerzo',
  'Me pagaron 2 millones de salario',
  'Le presté 300 mil a Rosa',
  'Pagué 80 mil de gasolina ayer',
];
