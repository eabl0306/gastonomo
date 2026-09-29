import type { ReactElement } from 'react';
import { useNavigate } from 'react-router';

import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { ResetPasswordScreen } from '../ui/auth/reset-password-screen';
import { HomeScreen } from '../ui/home/home-screen';
import { PendingScreen } from '../ui/shell/pending-screen';
import { SettingsScreen } from '../ui/shell/settings-screen';

import { useResetToken } from './auth-screens';
import type { Container } from './composition-root';
import { ROUTES } from './routes';

export function ResetPasswordRoute({ gateway }: { gateway: PasswordResetGateway }): ReactElement {
  const navigate = useNavigate();
  const token = useResetToken();
  return (
    <ResetPasswordScreen
      gateway={gateway}
      onFinished={() => {
        void navigate(ROUTES.login);
      }}
      token={token}
    />
  );
}

export function SettingsRoute(): ReactElement {
  const navigate = useNavigate();
  return (
    <SettingsScreen
      onSignedOut={() => {
        void navigate(ROUTES.login, { replace: true });
      }}
    />
  );
}

export function HomeRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  const go = (to: string) => () => {
    void navigate(to);
  };
  return (
    <HomeScreen
      actions={{
        // Los dos formularios todavia no existen: de momento las cuatro acciones rapidas
        // llevan a la lista que les corresponde.
        onNewMovement: go(ROUTES.movements),
        onNewLoan: go(ROUTES.loans),
        onOpenLoans: go(ROUTES.loans),
        onOpenMovements: go(ROUTES.movements),
        onOpenSettings: go(ROUTES.settings),
      }}
      ports={container}
    />
  );
}

/**
 * Las dos pantallas que todavia no estan.
 *
 * Se declaran juntas para que se vea de un vistazo lo que falta, y para que quitarlas cuando
 * lleguen las de verdad sea borrar una linea.
 */
export function MovementsRoute(): ReactElement {
  return (
    <PendingScreen
      icon="swap_vert"
      title="Movimientos"
      what="Aquí irá la lista agrupada por día, con el filtro de periodo y el resumen de ingresos, gastos y neto."
    />
  );
}

export function LoansRoute(): ReactElement {
  return (
    <PendingScreen
      icon="handshake"
      title="Préstamos"
      what="Aquí irán lo que te deben y lo que debes, con el saldo de cada préstamo y sus abonos."
    />
  );
}
