import type { ReactElement } from 'react';
import { BrowserRouter, Route, Routes, useNavigate, useSearchParams } from 'react-router';

import type { HealthGateway } from '../application/ports/health-gateway';
import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import { ResetPasswordScreen } from '../ui/auth/reset-password-screen';
import { HealthGatewayProvider } from '../ui/health/health-gateway-provider';
import { HealthPanel } from '../ui/health/health-panel';

import { RESET_TOKEN_PARAM, ROUTES } from './routes';

export interface AppProps {
  readonly gateway: HealthGateway;
  readonly passwordResets: PasswordResetGateway;
}

/** Lee el token de la direccion y manda al inicio cuando la pantalla termina. */
function ResetPasswordRoute({ gateway }: { gateway: PasswordResetGateway }): ReactElement {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  return (
    <ResetPasswordScreen
      gateway={gateway}
      onFinished={() => {
        void navigate(ROUTES.home);
      }}
      token={params.get(RESET_TOKEN_PARAM) ?? ''}
    />
  );
}

export function App({ gateway, passwordResets }: AppProps): ReactElement {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          element={<ResetPasswordRoute gateway={passwordResets} />}
          path={ROUTES.resetPassword}
        />
        <Route
          element={
            <HealthGatewayProvider gateway={gateway}>
              <HealthPanel />
            </HealthGatewayProvider>
          }
          path="*"
        />
      </Routes>
    </BrowserRouter>
  );
}
