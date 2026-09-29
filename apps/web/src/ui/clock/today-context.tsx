import { createContext, useContext, useMemo, type ReactElement, type ReactNode } from 'react';

import type { Clock } from '../../application/ports/clock';
import type { Day } from '../../domain/calendar/day';

const TodayContext = createContext<Day | null>(null);

/**
 * El dia de hoy, para toda la aplicacion.
 *
 * Se fija al montar y no se recalcula en cada render: moverlo a media interaccion cambiaria el
 * tope del selector de mes y la fecha por defecto de un formulario ya abierto.
 */
export function TodayProvider({
  clock,
  children,
}: {
  clock: Clock;
  children: ReactNode;
}): ReactElement {
  const today = useMemo(() => clock.today(), [clock]);
  return <TodayContext.Provider value={today}>{children}</TodayContext.Provider>;
}

export function useToday(): Day {
  const today = useContext(TodayContext);
  if (today === null) throw new Error('useToday se usa dentro de TodayProvider');
  return today;
}
