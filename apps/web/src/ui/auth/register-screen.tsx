import { useId, useState, type ReactElement, type SyntheticEvent } from 'react';

import { signUp, type SignUpOutcome } from '../../application/use-cases/sign-up';
import { MINIMUM_PASSWORD_LENGTH } from '../../domain/password';
import { useSessionPorts } from '../session/session-context';

import { AuthLayout, BrandMark, Notice } from './auth-chrome';
import { PasswordField } from './password-field';
import { TermsCheckbox } from './terms-checkbox';
import { TextField } from './text-field';

export interface RegisterScreenProps {
  readonly onSignedUp: () => void;
  readonly onBackToLogin: () => void;
}

interface FormState {
  readonly name: string;
  readonly email: string;
  readonly password: string;
  readonly accepted: boolean;
  readonly sending: boolean;
  readonly problem: SignUpOutcome | null;
}

const EMPTY: FormState = {
  name: '',
  email: '',
  password: '',
  accepted: false,
  sending: false,
  problem: null,
};

/**
 * Crear la cuenta.
 *
 * Al terminar **ya se esta dentro**: la API devuelve la sesion en el mismo 201, asi que no
 * hay que volver a Entrar.
 */
export function RegisterScreen({ onSignedUp, onBackToLogin }: RegisterScreenProps): ReactElement {
  const { auth, session } = useSessionPorts();
  const [state, setState] = useState<FormState>(EMPTY);

  const submit = async (event: SyntheticEvent): Promise<void> => {
    event.preventDefault();
    if (state.sending) return;
    setState((previous) => ({ ...previous, sending: true, problem: null }));
    const result = await signUp(auth, session, {
      name: state.name,
      email: state.email,
      password: state.password,
      acceptedTerms: state.accepted,
    });
    if (result.outcome === 'signed-up') {
      onSignedUp();
      return;
    }
    setState((previous) => ({ ...previous, sending: false, problem: result }));
  };

  return (
    <AuthLayout>
      <RegisterForm onChange={setState} onSubmit={submit} state={state} />
      <div className="g-auth__spacer" />
      <p className="g-auth__footer">
        ¿Ya tienes cuenta?{' '}
        <button onClick={onBackToLogin} type="button">
          Inicia sesión
        </button>
      </p>
    </AuthLayout>
  );
}

function RegisterForm({
  state,
  onChange,
  onSubmit,
}: {
  state: FormState;
  onChange: (next: FormState) => void;
  onSubmit: (event: SyntheticEvent) => Promise<void>;
}): ReactElement {
  const noticeId = useId();
  const ready = state.name.trim() !== '' && state.email.trim() !== '' && state.password !== '';

  return (
    <>
      <BrandMark />
      <h1 className="g-auth__title">Crea tu cuenta</h1>
      <p className="g-auth__lead">
        Tus movimientos y préstamos se guardan en la nube y se sincronizan entre tus dispositivos.
      </p>
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
      >
        <Fields noticeId={noticeId} onChange={onChange} state={state} />
        <TermsCheckbox
          checked={state.accepted}
          invalid={state.problem?.outcome === 'terms-not-accepted'}
          onChange={(accepted) => {
            onChange({ ...state, accepted });
          }}
        />
        <div id={noticeId}>
          {state.problem === null ? null : <SignUpProblem outcome={state.problem} />}
        </div>
        <button
          className="g-button"
          data-variant="primary"
          disabled={!ready || state.sending}
          style={{ width: '100%', marginTop: '16px' }}
          type="submit"
        >
          {state.sending ? 'Creando…' : 'Crear cuenta'}
        </button>
      </form>
    </>
  );
}

function Fields({
  state,
  noticeId,
  onChange,
}: {
  state: FormState;
  noticeId: string;
  onChange: (next: FormState) => void;
}): ReactElement {
  const rejected = state.problem?.outcome === 'rejected';
  const describedBy = state.problem === null ? undefined : noticeId;
  return (
    <>
      <TextField
        autoComplete="name"
        label="Nombre"
        onChange={(name) => {
          onChange({ ...state, name });
        }}
        placeholder="Tu nombre"
        value={state.name}
      />
      <TextField
        autoComplete="email"
        describedBy={describedBy}
        invalid={rejected}
        label="Correo"
        onChange={(email) => {
          onChange({ ...state, email });
        }}
        placeholder="tu@correo.com"
        type="email"
        value={state.email}
      />
      <PasswordField
        describedBy={describedBy}
        invalid={state.problem?.outcome === 'weak-password'}
        label="Contraseña"
        onChange={(password) => {
          onChange({ ...state, password });
        }}
        placeholder={`Mínimo ${String(MINIMUM_PASSWORD_LENGTH)} caracteres`}
        value={state.password}
        withStrength
      />
    </>
  );
}

function SignUpProblem({ outcome }: { outcome: SignUpOutcome }): ReactElement | null {
  if (outcome.outcome === 'signed-up') return null;
  return (
    <Notice icon="error" tone="error">
      {describe(outcome)}
    </Notice>
  );
}

/** El mensaje de la API se pinta tal cual: ya viene en castellano y es mas preciso. */
function describe(outcome: SignUpOutcome): string {
  if (outcome.outcome === 'terms-not-accepted') {
    return 'Tienes que aceptar los términos para crear la cuenta.';
  }
  if (outcome.outcome === 'weak-password') {
    return `La contraseña necesita al menos ${String(MINIMUM_PASSWORD_LENGTH)} caracteres.`;
  }
  if (outcome.outcome === 'rejected') {
    return outcome.emailMessage ?? outcome.detail ?? 'No pudimos crear la cuenta con esos datos.';
  }
  if (outcome.outcome === 'too-many-attempts') {
    return 'Demasiados intentos. Espera un momento y vuelve a probar.';
  }
  return 'No pudimos crear la cuenta. Revisa tu conexión y vuelve a intentarlo.';
}
