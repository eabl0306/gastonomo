import type { ReactElement } from 'react';

import { EXPORT_SETS, EXPORT_SET_NAMES, type ExportSet } from '../../domain/export/tables';
import { useAccount } from '../account/account-context';
import { Icon } from '../auth/auth-chrome';
import { useToday } from '../clock/today-context';
import { useFormats } from '../format/formats-context';
import { usePeriod } from '../period/period-context';

import { useExport, type ExportPorts, type ExportState } from './use-export';

const SET_ICONS: Readonly<Record<ExportSet, string>> = {
  movements: 'swap_vert',
  loans: 'handshake',
  payments: 'add_card',
};

/**
 * Exportar datos: formato, que incluir y de que periodo.
 *
 * El fichero se arma en el navegador con lo que ya esta cargado. No hay endpoint de
 * exportacion, y no hace falta: los datos de una cuenta caben de sobra en memoria.
 */
export function ExportScreen({
  ports,
  onBack,
}: {
  ports: ExportPorts;
  onBack: () => void;
}): ReactElement {
  const today = useToday();
  const { state: period } = usePeriod();
  const { settings, rates } = useAccount();
  const state = useExport({
    ports,
    period: period.selection.period,
    today,
    display: settings.displayCurrency,
    rates,
  });
  return (
    <div className="g-export">
      <header className="g-detail__bar">
        <button aria-label="Volver" className="g-detail__back" onClick={onBack} type="button">
          <Icon name="arrow_back" />
        </button>
        <h1 className="g-detail__title">Exportar datos</h1>
      </header>
      <Body state={state} />
      <button className="g-export__go" disabled={!state.ready} onClick={state.run} type="button">
        <Icon name="download" />
        {state.format === 'xlsx' ? 'Exportar a Excel' : 'Exportar CSV'}
      </button>
    </div>
  );
}

function Body({ state }: { state: ExportState }): ReactElement {
  const formats = useFormats();
  const { state: period } = usePeriod();
  return (
    <>
      <h2 className="g-export__title">Formato</h2>
      <Formats state={state} />
      <h2 className="g-export__title">Qué incluir</h2>
      <Sets state={state} />
      <h2 className="g-export__title">Periodo de movimientos</h2>
      <div className="g-filters">
        <button
          className="g-filter"
          data-active={state.range === 'all'}
          onClick={() => {
            state.setRange('all');
          }}
          type="button"
        >
          Todo el historial
        </button>
        <button
          className="g-filter"
          data-active={state.range === 'period'}
          onClick={() => {
            state.setRange('period');
          }}
          type="button"
        >
          {`Periodo actual · ${formats.periodLabel(period.selection.period)}`}
        </button>
      </div>
    </>
  );
}

const FORMATS = [
  {
    value: 'xlsx' as const,
    label: 'Excel',
    icon: 'table_view',
    description: 'Un libro con una hoja por conjunto',
  },
  {
    value: 'csv' as const,
    label: 'CSV',
    icon: 'data_object',
    description: 'Texto plano, compatible con todo',
  },
];

function Formats({ state }: { state: ExportState }): ReactElement {
  return (
    <div aria-label="Formato" className="g-export__formats" role="radiogroup">
      {FORMATS.map((format) => (
        <button
          aria-checked={state.format === format.value}
          className="g-export__format"
          data-active={state.format === format.value}
          data-value={format.value}
          key={format.value}
          onClick={() => {
            state.setFormat(format.value);
          }}
          role="radio"
          type="button"
        >
          <span className="g-export__format-icon">
            <Icon name={format.icon} />
          </span>
          <span className="g-export__format-label">{format.label}</span>
          <span className="g-export__format-note">{format.description}</span>
        </button>
      ))}
    </div>
  );
}

function Sets({ state }: { state: ExportState }): ReactElement {
  return (
    <div className="g-settings__card">
      {EXPORT_SETS.map((set) => (
        <button
          aria-pressed={state.sets.has(set)}
          className="g-settings__row"
          key={set}
          onClick={() => {
            state.toggle(set);
          }}
          type="button"
        >
          <span className="g-settings__icon">
            <Icon name={SET_ICONS[set]} />
          </span>
          <span className="g-export__set">
            <span className="g-export__set-name">{EXPORT_SET_NAMES[set]}</span>
            <span className="g-export__set-rows">{rowsLabel(state.counts[set])}</span>
          </span>
          <span className="g-export__check" data-active={state.sets.has(set)}>
            {state.sets.has(set) ? <Icon name="check" /> : null}
          </span>
        </button>
      ))}
    </div>
  );
}

/** "Sin filas", "1 fila" o "N filas". El prototipo escribe "1 filas"; aqui se dice bien. */
function rowsLabel(count: number): string {
  if (count === 0) return 'Sin filas';
  return count === 1 ? '1 fila' : `${String(count)} filas`;
}
