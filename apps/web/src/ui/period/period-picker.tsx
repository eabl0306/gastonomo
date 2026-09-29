import type { ReactElement } from 'react';

import { canGoForward, isMonthSelection } from '../../application/state/period-filter';
import { Icon } from '../auth/auth-chrome';
import { useFormats } from '../format/formats-context';

import { usePeriod } from './period-context';

/**
 * El selector de periodo que comparten Inicio y Movimientos.
 *
 * Dos detalles del diseno que parecen descuidos y no lo son: con un rango activo la flecha
 * izquierda se vuelve **invisible pero sigue ocupando sus 36px**, que es lo que mantiene la
 * etiqueta centrada, y el boton de rango conserva su borde claro tambien cuando se pone en
 * negro. Invisible va con `disabled`, no solo con opacidad: si no, se sigue llegando con el
 * tabulador a un boton que no se ve.
 */
export function PeriodPicker(): ReactElement {
  const { state, today, dispatch } = usePeriod();
  const formats = useFormats();
  const isMonth = isMonthSelection(state.selection);
  const forward = canGoForward(state.selection, today);
  return (
    <div className="g-period">
      <div className="g-period__box">
        <button
          aria-label="Mes anterior"
          className="g-period__arrow"
          data-hidden={!isMonth}
          disabled={!isMonth}
          onClick={() => {
            dispatch({ type: 'previous' });
          }}
          type="button"
        >
          <Icon name="chevron_left" />
        </button>
        <span className="g-period__label">{formats.periodLabel(state.selection.period)}</span>
        <button
          aria-label={isMonth ? 'Mes siguiente' : 'Volver al mes actual'}
          className="g-period__arrow"
          data-dim={isMonth && !forward}
          disabled={isMonth && !forward}
          onClick={() => {
            dispatch({ type: 'next-or-clear' });
          }}
          type="button"
        >
          <Icon name={isMonth ? 'chevron_right' : 'close'} />
        </button>
      </div>
      <button
        aria-label="Elegir un rango de fechas"
        className="g-period__range"
        data-active={!isMonth}
        onClick={() => {
          dispatch({ type: 'open' });
        }}
        type="button"
      >
        <Icon name="date_range" />
      </button>
    </div>
  );
}
