import type { ReactElement } from 'react';
import { useNavigate } from 'react-router';

import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { ResetPasswordScreen } from '../ui/auth/reset-password-screen';
import { HomeScreen } from '../ui/home/home-screen';
import { MovementsScreen } from '../ui/movements/movements-screen';
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

export function MovementsRoute({ container }: { container: Container }): ReactElement {
  return <MovementsScreen ports={container} />;
}

/**
 * La pantalla que todavia no esta. Se queda declarada aqui para que quitarla cuando llegue la
 * de verdad sea borrar una linea.
 */
export function LoansRoute(): ReactElement {
  return (
    <PendingScreen
      icon="handshake"
      title="Préstamos"
      what="Aquí irán lo que te deben y lo que debes, con el saldo de cada préstamo y sus abonos."
    />
  );
}
