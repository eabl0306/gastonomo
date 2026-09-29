import type { ReactElement } from 'react';
import { Navigate, useLocation, useNavigate, useSearchParams } from 'react-router';

import { ForgotScreen } from '../ui/auth/forgot-screen';
import { LoginScreen } from '../ui/auth/login-screen';
import { RegisterScreen } from '../ui/auth/register-screen';
import { useSession } from '../ui/session/use-session';

import { RESET_TOKEN_PARAM, ROUTES, type LoginLocationState } from './routes';

/** Lee el estado que dejo el guardia al mandar aqui. */
function useLoginState(): LoginLocationState {
  const location = useLocation();
  return (location.state ?? {}) as LoginLocationState;
}

/**
 * Entrar.
 *
 * Con sesion ya puesta no se pinta: se va a donde se queria ir. Asi, quien vuelve a la
 * pestana con la sesion viva no se encuentra un formulario de entrada.
 */
export function LoginRoute(): ReactElement {
  const navigate = useNavigate();
  const session = useSession();
  const state = useLoginState();
  const destination = state.from ?? ROUTES.home;

  if (session !== null) return <Navigate replace to={destination} />;

  return (
    <LoginScreen
      onForgotPassword={(email) => {
        void navigate(ROUTES.forgotPassword, { state: { email } satisfies LoginLocationState });
      }}
      onRegister={() => {
        void navigate(ROUTES.register);
      }}
      onSignedIn={() => {
        void navigate(destination, { replace: true });
      }}
    />
  );
}

export function RegisterRoute(): ReactElement {
  const navigate = useNavigate();
  return (
    <RegisterScreen
      onBackToLogin={() => {
        void navigate(ROUTES.login);
      }}
      onSignedUp={() => {
        void navigate(ROUTES.home, { replace: true });
      }}
    />
  );
}

export function ForgotRoute(): ReactElement {
  const navigate = useNavigate();
  const state = useLoginState();
  return (
    <ForgotScreen
      email={state.email ?? ''}
      onBackToLogin={() => {
        void navigate(ROUTES.login);
      }}
    />
  );
}

/** Lee el token de la direccion y manda a entrar cuando la pantalla termina. */
export function useResetToken(): string {
  const [params] = useSearchParams();
  return params.get(RESET_TOKEN_PARAM) ?? '';
}
