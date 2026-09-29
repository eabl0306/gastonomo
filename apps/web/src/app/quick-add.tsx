import type { ReactElement } from 'react';
import { useNavigate } from 'react-router';

import { AppShell } from '../ui/shell/app-shell';

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
  // Todavia no existen: de momento llevan a su lista.
  loan: ROUTES.loans,
  dictate: ROUTES.movements,
};

/**
 * El armazon con el boton flotante ya cableado.
 *
 * El mapa de rutas vive aqui y no en `ui`: la interfaz no conoce las direcciones, que son de
 * la raiz de composicion.
 */
export function ShellWithQuickAdd(): ReactElement {
  const navigate = useNavigate();
  return (
    <AppShell
      quickAdd={{
        routes: FAB_ROUTES,
        onChoose: (option) => {
          const to = ROUTE_OF_OPTION[option];
          if (to !== undefined) void navigate(to);
        },
      }}
    />
  );
}
