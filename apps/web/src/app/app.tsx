import type { ReactElement } from 'react';
import { BrowserRouter, Route, Routes, useLocation, type Location } from 'react-router';

import type { HealthGateway } from '../application/ports/health-gateway';
import { AccountProvider } from '../ui/account/account-context';
import { TodayProvider } from '../ui/clock/today-context';
import { HealthGatewayProvider } from '../ui/health/health-gateway-provider';
import { HealthPanel } from '../ui/health/health-panel';
import { PeriodProvider } from '../ui/period/period-context';
import { SessionProvider } from '../ui/session/session-context';

import {
  CategoriesRoute,
  ExportRoute,
  HomeRoute,
  LoanDetailRoute,
  LoansRoute,
  MovementsRoute,
  NewLoanRoute,
  NewMovementRoute,
  ResetPasswordRoute,
  SettingsRoute,
} from './app-screens';
import { ForgotRoute, LoginRoute, RegisterRoute } from './auth-screens';
import type { Container } from './composition-root';
import { ShellWithQuickAdd } from './quick-add';
import { RequireSession } from './require-session';
import { ROUTES } from './routes';

/**
 * El arbol de rutas.
 *
 * Las cuatro principales cuelgan del armazon y exigen sesion; las de autenticacion no la
 * tienen y por eso no llevan armazon. `/estado` se queda fuera de las dos: es una pantalla de
 * diagnostico, no del producto.
 *
 * Los ajustes de la cuenta y el filtro de periodo se montan **por encima** de las cuatro,
 * porque los comparten: el periodo elegido se ve igual en Inicio y en Movimientos, y la
 * moneda principal decide lo que ensenan todas.
 */
export function App({ container }: { container: Container }): ReactElement {
  return (
    <BrowserRouter>
      <SessionProvider value={{ session: container.session, auth: container.auth }}>
        <Screens container={container} />
      </SessionProvider>
    </BrowserRouter>
  );
}

/**
 * El arbol de rutas.
 *
 * El dictado es una **ruta** y no una hoja, porque lo abren tres pantallas y al cerrarlo hay
 * que volver a la que estuviera. Para que esa pantalla se siga viendo detras, se navega con la
 * anterior en el estado y aqui se pinta esa: el modal lo anade el armazon, que es quien esta
 * dentro de los proveedores. Entrar directo por la direccion tambien funciona, solo que sin
 * pantalla de fondo.
 */
function Screens({ container }: { container: Container }): ReactElement {
  const location = useLocation();
  const background = (location.state as { background?: Location } | null)?.background;
  return (
    <Routes location={background ?? location}>
      <Route
        element={
          <RequireSession>
            <TodayProvider clock={container.clock}>
              <AccountProvider currencies={container.currencies} settings={container.settings}>
                <PeriodProvider>
                  <ShellWithQuickAdd
                    container={container}
                    dictating={location.pathname === ROUTES.dictate}
                  />
                </PeriodProvider>
              </AccountProvider>
            </TodayProvider>
          </RequireSession>
        }
      >
        <Route element={<HomeRoute container={container} />} index />
        <Route element={<MovementsRoute container={container} />} path={ROUTES.movements} />
        <Route element={<NewMovementRoute container={container} />} path={ROUTES.newMovement} />
        <Route element={<LoansRoute container={container} />} path={ROUTES.loans} />
        <Route element={<NewLoanRoute container={container} />} path={ROUTES.newLoan} />
        <Route element={<LoanDetailRoute container={container} />} path={ROUTES.loanDetail} />
        <Route element={<SettingsRoute container={container} />} path={ROUTES.settings} />
        <Route element={<CategoriesRoute container={container} />} path={ROUTES.categories} />
        <Route element={<ExportRoute container={container} />} path={ROUTES.export} />
        {/* El dictado lo pinta el armazon; esta ruta solo existe para que entrar
            directo por la direccion monte el armazon y sus proveedores. */}
        <Route element={null} path={ROUTES.dictate} />
      </Route>
      <Route element={<LoginRoute />} path={ROUTES.login} />
      <Route element={<RegisterRoute />} path={ROUTES.register} />
      <Route element={<ForgotRoute />} path={ROUTES.forgotPassword} />
      <Route
        element={<ResetPasswordRoute gateway={container.passwordResets} />}
        path={ROUTES.resetPassword}
      />
      <Route element={<HealthScreen gateway={container.health} />} path={ROUTES.status} />
    </Routes>
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
