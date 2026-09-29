import { useEffect, type ReactElement } from 'react';

import { canApplyRange, type RangeDraft } from '../../application/state/period-filter';
import { PERIOD_PRESETS, type PeriodPreset } from '../../domain/calendar/period';

import { usePeriod } from './period-context';

/** Los cinco presets, con el texto del diseno. */
const PRESET_LABELS: Readonly<Record<PeriodPreset, string>> = {
  'this-month': 'Este mes',
  'last-month': 'Mes pasado',
  'last-30-days': 'Últimos 30 días',
  'last-3-months': 'Últimos 3 meses',
  'this-year': 'Este año',
};

/**
 * El sheet de rango, montado **una sola vez** en el armazon: el borrador es el mismo para
 * Inicio y para Movimientos.
 *
 * En escritorio es un modal centrado de 480px y en movil una hoja que sube desde abajo. Lo
 * decide CSS, igual que el resto del armazon.
 */
export function RangeSheet(): ReactElement | null {
  const { state, dispatch } = usePeriod();
  const draft = state.draft;
  useEscapeToClose(draft !== null);
  if (draft === null) return null;
  return (
    <div className="g-sheet">
      <button
        aria-label="Cerrar"
        className="g-sheet__scrim"
        onClick={() => {
          dispatch({ type: 'close' });
        }}
        type="button"
      />
      <Panel draft={draft} />
    </div>
  );
}

/** Escape cierra, que en el modal de escritorio es lo que se espera. */
function useEscapeToClose(open: boolean): void {
  const { dispatch } = usePeriod();
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') dispatch({ type: 'close' });
    };
    globalThis.addEventListener('keydown', onKey);
    return () => {
      globalThis.removeEventListener('keydown', onKey);
    };
  }, [open, dispatch]);
}

function Panel({ draft }: { draft: RangeDraft }): ReactElement {
  const { dispatch } = usePeriod();
  return (
    <div aria-labelledby="g-range-title" aria-modal="true" className="g-sheet__panel" role="dialog">
      <div className="g-sheet__handle" />
      <h2 className="g-sheet__title" id="g-range-title">
        Rango de fechas
      </h2>
      <p className="g-sheet__subtitle">Se aplica al inicio y a movimientos.</p>
      <Presets draft={draft} />
      <Fields draft={draft} />
      <button
        className="g-sheet__apply"
        disabled={!canApplyRange(draft)}
        onClick={() => {
          dispatch({ type: 'apply' });
        }}
        type="button"
      >
        Aplicar rango
      </button>
    </div>
  );
}

function Presets({ draft }: { draft: RangeDraft }): ReactElement {
  const { dispatch } = usePeriod();
  return (
    <div className="g-sheet__presets">
      {PERIOD_PRESETS.map((preset) => (
        <button
          aria-pressed={draft.preset === preset}
          className="g-pill"
          data-active={draft.preset === preset}
          key={preset}
          onClick={() => {
            dispatch({ type: 'preset', preset });
          }}
          type="button"
        >
          {PRESET_LABELS[preset]}
        </button>
      ))}
    </div>
  );
}

/**
 * Los dos campos de fecha.
 *
 * Van como `input type="date"` con el valor crudo en `YYYY-MM-DD`: el navegador lo pinta en
 * el formato local el solo, y es el unico sitio de la aplicacion donde una fecha no pasa por
 * el modulo de formato. Teclear una a mano desmarca la pill pero conserva lo tecleado.
 */
function Fields({ draft }: { draft: RangeDraft }): ReactElement {
  const { dispatch } = usePeriod();
  return (
    <div className="g-sheet__fields">
      <label className="g-sheet__field">
        <span className="g-sheet__field-label">Desde</span>
        <input
          max={draft.to}
          onChange={(event) => {
            dispatch({ type: 'from', day: event.target.value });
          }}
          type="date"
          value={draft.from}
        />
      </label>
      <label className="g-sheet__field">
        <span className="g-sheet__field-label">Hasta</span>
        <input
          min={draft.from}
          onChange={(event) => {
            dispatch({ type: 'to', day: event.target.value });
          }}
          type="date"
          value={draft.to}
        />
      </label>
    </div>
  );
}
