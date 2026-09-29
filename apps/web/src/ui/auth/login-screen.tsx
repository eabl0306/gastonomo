import { useId, useState, type ReactElement, type SyntheticEvent } from 'react';

import { signIn, type SignInOutcome } from '../../application/use-cases/sign-in';
import { useSessionPorts } from '../session/session-context';

import { AuthLayout, BrandMark, Notice } from './auth-chrome';
import { PasswordField } from './password-field';
import { TextField } from './text-field';

export interface LoginScreenProps {
  /** Se llama cuando la sesion ya esta guardada. Quien hospeda decide a donde ir. */
  readonly onSignedIn: () => void;
  readonly onForgotPassword: (email: string) => void;
  readonly onRegister: () => void;
}

interface FormState {
  readonly email: string;
  readonly password: string;
  readonly sending: boolean;
  readonly problem: SignInOutcome | null;
}

const EMPTY: FormState = { email: '', password: '', sending: false, problem: null };

/**
 * Entrar con correo y contrasena.
 *
 * El boton se apaga con cualquiera de los dos campos vacio. El prototipo lo deja siempre
 * encendido, pero mandar un formulario vacio solo sirve para gastar un viaje a la API y
 * recibir el mismo "correo o contrasena incorrectos" que no dice nada.
 *
 * Lo que **no** se valida aqui es si el correo tiene forma de correo: quien decide si esa
 * cuenta existe es la API, y un `type=email` que bloquea el envio esconde el error detras de
 * un globo del navegador que no se puede ni traducir ni maquetar.
 */
export function LoginScreen({
  onSignedIn,
  onForgotPassword,
  onRegister,
}: LoginScreenProps): ReactElement {
  const { auth, session } = useSessionPorts();
  const [state, setState] = useState<FormState>(EMPTY);

  const submit = async (event: SyntheticEvent): Promise<void> => {
    event.preventDefault();
    if (state.sending) return;
    setState((previous) => ({ ...previous, sending: true, problem: null }));
    const result = await signIn(auth, session, state.email, state.password);
    if (result.outcome === 'signed-in') {
      onSignedIn();
      return;
    }
    setState((previous) => ({ ...previous, sending: false, problem: result }));
  };

  return (
    <AuthLayout>
      <LoginForm
        onChange={setState}
        onForgotPassword={() => {
          onForgotPassword(state.email);
        }}
        onRegister={onRegister}
        onSubmit={submit}
        state={state}
      />
    </AuthLayout>
  );
}

interface FormProps {
  readonly state: FormState;
  readonly onChange: (next: FormState) => void;
  readonly onSubmit: (event: SyntheticEvent) => Promise<void>;
  readonly onForgotPassword: () => void;
  readonly onRegister: () => void;
}

function LoginForm({
  state,
  onChange,
  onSubmit,
  onForgotPassword,
  onRegister,
}: FormProps): ReactElement {
  const noticeId = useId();
  const ready = state.email.trim() !== '' && state.password !== '';

  return (
    <>
      <BrandMark />
      <h1 className="g-auth__title" data-size="large">
        Bienvenido de nuevo
      </h1>
      <p className="g-auth__lead">Entra para ver tus cuentas sincronizadas.</p>
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
      >
        <Fields noticeId={noticeId} onChange={onChange} state={state} />
        <button className="g-auth__aside-link" onClick={onForgotPassword} type="button">
          ¿Olvidaste tu contraseña?
        </button>
        <div id={noticeId}>
          {state.problem === null ? null : <SignInProblem outcome={state.problem} />}
        </div>
        <button
          className="g-button"
          data-variant="primary"
          disabled={!ready || state.sending}
          style={{ width: '100%', marginTop: '8px' }}
          type="submit"
        >
          {state.sending ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <div className="g-auth__spacer" />
      <p className="g-auth__footer">
        ¿No tienes cuenta?{' '}
        <button onClick={onRegister} type="button">
          Regístrate
        </button>
      </p>
    </>
  );
}

/** Los dos campos. Van aparte solo para que cada funcion quepa de un vistazo. */
function Fields({
  state,
  noticeId,
  onChange,
}: {
  state: FormState;
  noticeId: string;
  onChange: (next: FormState) => void;
}): ReactElement {
  const failed = state.problem !== null;
  const describedBy = failed ? noticeId : undefined;
  return (
    <>
      <TextField
        autoComplete="email"
        describedBy={describedBy}
        invalid={failed}
        label="Correo"
        onChange={(email) => {
          onChange({ ...state, email });
        }}
        placeholder="tu@correo.com"
        type="email"
        value={state.email}
      />
      <PasswordField
        autoComplete="current-password"
        describedBy={describedBy}
        invalid={failed}
        label="Contraseña"
        onChange={(password) => {
          onChange({ ...state, password });
        }}
        value={state.password}
      />
    </>
  );
}

/**
 * Por que no se pudo entrar.
 *
 * Un 401 no dice si fallo el correo o la contrasena, y la pantalla tampoco: decirlo
 * confirmaria que ese correo tiene cuenta aqui.
 */
function SignInProblem({ outcome }: { outcome: SignInOutcome }): ReactElement | null {
  if (outcome.outcome === 'signed-in') return null;
  if (outcome.outcome === 'invalid-credentials') {
    return (
      <Notice icon="error" tone="error">
        Correo o contraseña incorrectos.
      </Notice>
    );
  }
  if (outcome.outcome === 'too-many-attempts') {
    return (
      <Notice icon="error" tone="error">
        Demasiados intentos. Espera un momento y vuelve a probar.
      </Notice>
    );
  }
  return (
    <Notice icon="error" tone="error">
      No pudimos entrar. Revisa tu conexión y vuelve a intentarlo.
    </Notice>
  );
}
