import type { ReactElement } from 'react';
import { useLocation, useNavigate } from 'react-router';

import { AppShell } from '../ui/shell/app-shell';

import { DictateRoute } from './app-screens';
import type { Container } from './composition-root';
import { ROUTES } from './routes';

/**
 * Las tres pantallas que ensenan el boton flotante.
 *
 * No sale en Ajustes ni dentro de un formulario: ahi taparia el boton de guardar, que es lo
 * que de verdad hay que pulsar.
 */
const FAB_ROUTES: ReadonlySet<string> = new Set([ROUTES.home, ROUTES.movements, ROUTES.loans]);

/** A donde lleva cada opcion del menu radial. */
const ROUTE_OF_OPTION: Readonly<Record<string, string>> = {
  movement: ROUTES.newMovement,
  loan: ROUTES.newLoan,
  dictate: ROUTES.dictate,
};

/**
 * El armazon con el boton flotante y, encima, el dictado.
 *
 * `dictating` llega por props y no de `useLocation`: dentro de un `<Routes location={...}>` ese
 * hook devuelve la direccion **que se le paso**, que aqui es la pantalla de fondo. Leerlo aqui
 * haria que el modal no saliera nunca al abrirlo desde el boton.
 */
export function ShellWithQuickAdd({
  container,
  dictating,
}: {
  container: Container;
  dictating: boolean;
}): ReactElement {
  const navigate = useNavigate();
  const location = useLocation();
  return (
    <>
      <AppShell
        quickAdd={{
          routes: FAB_ROUTES,
          onChoose: (option) => {
            const to = ROUTE_OF_OPTION[option];
            if (to === undefined) return;
            // El dictado se abre encima de lo que hubiera: se lleva la pantalla actual en el
            // estado para poder pintarla de fondo y volver a ella al cerrar.
            const state = to === ROUTES.dictate ? { background: location } : undefined;
            void navigate(to, { state });
          },
        }}
      />
      {dictating ? <DictateRoute container={container} /> : null}
    </>
  );
}
