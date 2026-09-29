import type { ReactElement } from 'react';

import { localeOf } from '../../domain/account/settings';
import { isComplete } from '../../domain/dictation/draft';
import { useAccount } from '../account/account-context';
import { Icon, Notice } from '../auth/auth-chrome';
import { useToday } from '../clock/today-context';
import { LoadingBlock, messageOf } from '../common/blocks';
import { Sheet } from '../common/sheet';

import { DICTATION_EXAMPLES } from './dictation-state';
import { DraftCard } from './draft-card';
import { useConfirmDraft, type ConfirmPorts } from './use-confirm-draft';
import { useDictation, type DictationPorts, type DictationState } from './use-dictation';

export type DictationModalPorts = DictationPorts & ConfirmPorts;

/**
 * Dictar un registro.
 *
 * El reconocimiento lo hace el navegador y a la API solo viaja el texto. Lo que devuelve es un
 * **borrador**: guardarlo es otra llamada, con su clave de idempotencia.
 */
export function DictationModal({
  ports,
  onClose,
  onSaved,
}: {
  ports: DictationModalPorts;
  onClose: () => void;
  onSaved: () => void;
}): ReactElement {
  const today = useToday();
  const { settings, rates } = useAccount();
  const state = useDictation({ ports, today, locale: localeOf(settings.language) });
  const confirm = useConfirmDraft({
    ports,
    display: settings.displayCurrency,
    rates,
    onSaved,
  });
  return (
    <Sheet onClose={onClose} title={titleOf(state)}>
      <Body confirm={confirm} state={state} />
      {state.error === null ? null : (
        <Notice icon="error" tone="error">
          {messageOf(state.error)}
        </Notice>
      )}
    </Sheet>
  );
}

function titleOf(state: DictationState): string {
  if (state.phase.step === 'review') return 'Revisa antes de guardar';
  return state.phase.step === 'parsing' ? 'Interpretando…' : 'Dictar';
}

function Body({
  state,
  confirm,
}: {
  state: DictationState;
  confirm: ReturnType<typeof useConfirmDraft>;
}): ReactElement {
  const { settings, rates } = useAccount();
  const phase = state.phase;
  if (phase.step === 'listening') return <Listening heard={phase.heard} state={state} />;
  if (phase.step === 'parsing') return <LoadingBlock />;
  if (phase.step === 'typing') {
    return <Typing reason={phase.reason} state={state} text={phase.text} />;
  }
  return (
    <>
      <p className="g-dictation__said">{`“${phase.text}”`}</p>
      <DraftCard display={settings.displayCurrency} draft={phase.draft} rates={rates} />
      {confirm.error === null ? null : (
        <Notice icon="error" tone="error">
          {messageOf(confirm.error)}
        </Notice>
      )}
      <button
        className="g-sheet__apply"
        disabled={!isComplete(phase.draft) || confirm.saving}
        onClick={() => {
          confirm.confirm(phase.draft);
        }}
        type="button"
      >
        {confirm.saving ? 'Guardando…' : 'Guardar'}
      </button>
      <button className="g-dictation__redo" onClick={state.toTyping} type="button">
        <Icon name="edit" />
        Escribirlo otra vez
      </button>
    </>
  );
}

function Listening({ heard, state }: { heard: string; state: DictationState }): ReactElement {
  return (
    <div className="g-dictation__listening">
      <button
        aria-label="Terminar de dictar"
        className="g-dictation__mic"
        onClick={state.stopListening}
        type="button"
      >
        <Icon filled name="mic" />
      </button>
      <p className="g-dictation__live" data-empty={heard === ''}>
        {heard === '' ? 'Te escucho…' : heard}
      </p>
      <p className="g-dictation__tip">Toca el micrófono para terminar</p>
      <button className="g-dictation__switch" onClick={state.toTyping} type="button">
        Prefiero escribirlo
      </button>
    </div>
  );
}

function Typing({
  text,
  reason,
  state,
}: {
  text: string;
  reason: string | null;
  state: DictationState;
}): ReactElement {
  return (
    <>
      {reason === null ? null : (
        <p className="g-dictation__reason">
          <Icon name="info" />
          <span>{reason}</span>
        </p>
      )}
      <textarea
        aria-label="Lo que quieres registrar"
        className="g-dictation__text"
        onChange={(event) => {
          state.setText(event.target.value);
        }}
        placeholder="Ej.: Gasté 45 mil en almuerzo"
        rows={3}
        value={text}
      />
      <Actions state={state} text={text} />
      {text === '' ? <Examples onPick={state.setText} /> : null}
    </>
  );
}

function Actions({ text, state }: { text: string; state: DictationState }): ReactElement {
  return (
    <div className="g-dictation__actions">
      {state.canDictate ? (
        <button
          aria-label="Dictar"
          className="g-dictation__mic-small"
          onClick={state.startListening}
          type="button"
        >
          <Icon name="mic" />
        </button>
      ) : null}
      <button
        className="g-sheet__apply"
        disabled={text.trim() === ''}
        onClick={() => {
          state.parse(text);
        }}
        type="button"
      >
        Interpretar
      </button>
    </div>
  );
}

function Examples({ onPick }: { onPick: (text: string) => void }): ReactElement {
  return (
    <>
      <h3 className="g-dictation__examples-title">Prueba con</h3>
      <div className="g-dictation__examples">
        {DICTATION_EXAMPLES.map((example) => (
          <button
            className="g-chip"
            key={example}
            onClick={() => {
              onPick(example);
            }}
            type="button"
          >
            {example}
          </button>
        ))}
      </div>
    </>
  );
}
