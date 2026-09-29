import type { ReactElement } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';

import type { AuthGateway } from '../application/ports/auth-gateway';
import type { HealthGateway } from '../application/ports/health-gateway';
import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import type { SessionStore } from '../application/ports/session-store';
import { HealthGatewayProvider } from '../ui/health/health-gateway-provider';
import { HealthPanel } from '../ui/health/health-panel';
import { SessionProvider } from '../ui/session/session-context';
import { AppShell } from '../ui/shell/app-shell';

import {
  HomeRoute,
  LoansRoute,
  MovementsRoute,
  ResetPasswordRoute,
  SettingsRoute,
} from './app-screens';
import { ForgotRoute, LoginRoute, RegisterRoute } from './auth-screens';
import { RequireSession } from './require-session';
import { ROUTES } from './routes';

export interface AppProps {
  readonly session: SessionStore;
  readonly auth: AuthGateway;
  readonly health: HealthGateway;
  readonly passwordResets: PasswordResetGateway;
}

/**
 * El arbol de rutas.
 *
 * Las cuatro principales cuelgan del armazon y exigen sesion; las de autenticacion no la
 * tienen y por eso no llevan armazon. `/estado` se queda fuera de las dos: es una pantalla de
 * diagnostico, no del producto.
 */
export function App({ session, auth, health, passwordResets }: AppProps): ReactElement {
  return (
    <BrowserRouter>
      <SessionProvider value={{ session, auth }}>
        <Routes>
          <Route
            element={
              <RequireSession>
                <AppShell />
              </RequireSession>
            }
          >
            <Route element={<HomeRoute />} index />
            <Route element={<MovementsRoute />} path={ROUTES.movements} />
            <Route element={<LoansRoute />} path={ROUTES.loans} />
            <Route element={<SettingsRoute />} path={ROUTES.settings} />
          </Route>
          <Route element={<LoginRoute />} path={ROUTES.login} />
          <Route element={<RegisterRoute />} path={ROUTES.register} />
          <Route element={<ForgotRoute />} path={ROUTES.forgotPassword} />
          <Route
            element={<ResetPasswordRoute gateway={passwordResets} />}
            path={ROUTES.resetPassword}
          />
          <Route element={<HealthScreen gateway={health} />} path={ROUTES.status} />
        </Routes>
      </SessionProvider>
    </BrowserRouter>
  );
}

/**
 * La pantalla de diagnostico. Se envuelve en su propia caja centrada porque no lleva
 * armazon: es lo unico que queda del andamio anterior.
 */
function HealthScreen({ gateway }: { gateway: HealthGateway }): ReactElement {
  return (
    <div className="health-panel--page">
      <HealthGatewayProvider gateway={gateway}>
        <HealthPanel />
      </HealthGatewayProvider>
    </div>
  );
}
