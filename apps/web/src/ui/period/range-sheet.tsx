import { useCallback, type ReactElement } from 'react';

import { canApplyRange, type RangeDraft } from '../../application/state/period-filter';
import { PERIOD_PRESETS, type PeriodPreset } from '../../domain/calendar/period';
import { Sheet } from '../common/sheet';

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
  const close = useCallback(() => {
    dispatch({ type: 'close' });
  }, [dispatch]);
  if (draft === null) return null;
  return (
    <Sheet onClose={close} subtitle="Se aplica al inicio y a movimientos." title="Rango de fechas">
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
    </Sheet>
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
