import type { ReactElement } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';

import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { ResetPasswordScreen } from '../ui/auth/reset-password-screen';
import { CategoriesScreen } from '../ui/categories/categories-screen';
import { DictationModal } from '../ui/dictation/dictation-modal';
import { ExportScreen } from '../ui/export/export-screen';
import { prefillOf, type PrefillState } from '../ui/forms/prefill';
import { HomeScreen } from '../ui/home/home-screen';
import { LoanDetailScreen } from '../ui/loans/loan-detail-screen';
import { LoansScreen } from '../ui/loans/loans-screen';
import { NewLoanScreen } from '../ui/loans/new-loan-screen';
import { MovementsScreen } from '../ui/movements/movements-screen';
import { NewMovementScreen } from '../ui/movements/new-movement-screen';
import { SettingsScreen } from '../ui/settings/settings-screen';

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

export function SettingsRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  return (
    <SettingsScreen
      actions={{
        onOpenCategories: () => {
          void navigate(ROUTES.categories);
        },
        onOpenExport: () => {
          void navigate(ROUTES.export);
        },
        onSignedOut: () => {
          void navigate(ROUTES.login, { replace: true });
        },
      }}
      ports={container}
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
  const prefill = usePrefill().movement;
  const leave = (): void => {
    void navigate(ROUTES.movements, { replace: true });
  };
  return <NewMovementScreen onClose={leave} onSaved={leave} ports={container} prefill={prefill} />;
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
  const prefill = usePrefill().loan;
  const leave = (): void => {
    void navigate(ROUTES.loans, { replace: true });
  };
  return (
    <NewLoanScreen gateway={container.loans} onClose={leave} onSaved={leave} prefill={prefill} />
  );
}

/** Lo que el dictado dejo en el estado de la navegacion, o nada si se entro por el boton. */
function usePrefill(): PrefillState {
  const location = useLocation();
  return (location.state as PrefillState | null) ?? {};
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

export function CategoriesRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  return (
    <CategoriesScreen
      gateway={container.categories}
      onBack={() => {
        void navigate(ROUTES.settings);
      }}
    />
  );
}

export function ExportRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  return (
    <ExportScreen
      onBack={() => {
        void navigate(ROUTES.settings);
      }}
      ports={container}
    />
  );
}

/**
 * El dictado.
 *
 * Es una ruta y no una hoja aunque se vea como un modal: lo abren tres pantallas, y al cerrarlo
 * hay que volver a la que estuviera. Al guardar se va a Movimientos, que es donde aparece lo
 * que se acaba de registrar.
 */
export function DictateRoute({ container }: { container: Container }): ReactElement {
  const navigate = useNavigate();
  return (
    <DictationModal
      onClose={() => {
        void navigate(-1);
      }}
      // `replace`: el formulario **sustituye** al dictado en la pila. Si se apilara, volver
      // atras desde el formulario reabriria el modal con el borrador ya editado.
      onEdit={(draft) => {
        const prefill = prefillOf(draft);
        const to = prefill.movement === undefined ? ROUTES.newLoan : ROUTES.newMovement;
        void navigate(to, { replace: true, state: prefill });
      }}
      onSaved={() => {
        void navigate(ROUTES.movements, { replace: true });
      }}
      ports={container}
    />
  );
}
