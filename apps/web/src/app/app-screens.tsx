import type { ReactElement } from 'react';
import { useNavigate, useParams } from 'react-router';

import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { ResetPasswordScreen } from '../ui/auth/reset-password-screen';
import { HomeScreen } from '../ui/home/home-screen';
import { LoanDetailScreen } from '../ui/loans/loan-detail-screen';
import { LoansScreen } from '../ui/loans/loans-screen';
import { NewLoanScreen } from '../ui/loans/new-loan-screen';
import { MovementsScreen } from '../ui/movements/movements-screen';
import { NewMovementScreen } from '../ui/movements/new-movement-screen';
import { SettingsScreen } from '../ui/shell/settings-screen';

import { useResetToken } from './auth-screens';
import type { Container } from './composition-root';
import { ROUTES, loanDetailPath } from './routes';

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
 * El formulario de un movimiento.
 *
 * Al guardar vuelve **reemplazando** la entrada de la pila, no apilando: dejarla dentro haria
 * que el boton de atras del navegador volviera a un formulario ya enviado.
 */
export function NewMovementRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  const leave = (): void => {
    void navigate(ROUTES.movements, { replace: true });
  };
  return <NewMovementScreen onClose={leave} onSaved={leave} ports={container} />;
}

export function LoansRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  return (
    <LoansScreen
      actions={{
        onOpenLoan: (id) => {
          void navigate(loanDetailPath(id));
        },
      }}
      ports={container}
    />
  );
}

export function NewLoanRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  const leave = (): void => {
    void navigate(ROUTES.loans, { replace: true });
  };
  return <NewLoanScreen gateway={container.loans} onClose={leave} onSaved={leave} />;
}

export function LoanDetailRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  const { loanId } = useParams();
  return (
    <LoanDetailScreen
      actions={{
        onBack: () => {
          void navigate(ROUTES.loans);
        },
      }}
      loanId={loanId ?? ''}
      ports={container}
    />
  );
}
