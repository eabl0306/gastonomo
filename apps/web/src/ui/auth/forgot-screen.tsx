import { useState, type ReactElement, type SyntheticEvent } from 'react';

import {
  requestPasswordReset,
  type PasswordResetRequestOutcome,
} from '../../application/use-cases/request-password-reset';
import { useSessionPorts } from '../session/session-context';

import { AuthLayout, BrandMark, Icon, Notice } from './auth-chrome';
import { TextField } from './text-field';

export interface ForgotScreenProps {
  /** El correo que ya estaba escrito en Entrar, si lo habia. */
  readonly email: string;
  readonly onBackToLogin: () => void;
}

/**
 * Pide el enlace para crear una contrasena nueva.
 *
 * **Se ensena lo mismo exista la cuenta o no.** La API contesta igual a proposito, y la
 * pantalla tiene que respetarlo: un "ese correo no esta registrado" le dice a cualquiera
 * quien tiene cuenta aqui.
 */
export function ForgotScreen({ email, onBackToLogin }: ForgotScreenProps): ReactElement {
  const { auth } = useSessionPorts();
  const [address, setAddress] = useState(email);
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<PasswordResetRequestOutcome | null>(null);

  const submit = async (event: SyntheticEvent): Promise<void> => {
    event.preventDefault();
    if (sending) return;
    setSending(true);
    setResult(await requestPasswordReset(auth, address));
    setSending(false);
  };

  if (result?.outcome === 'sent') {
    return <ResetLinkSent address={address.trim()} onBackToLogin={onBackToLogin} />;
  }
  return (
    <AuthLayout>
      <ForgotForm
        address={address}
        onAddress={setAddress}
        onSubmit={submit}
        result={result}
        sending={sending}
      />
      <div className="g-auth__spacer" />
      <p className="g-auth__footer">
        <button onClick={onBackToLogin} type="button">
          Volver a iniciar sesión
        </button>
      </p>
    </AuthLayout>
  );
}

function ForgotForm({
  address,
  onAddress,
  onSubmit,
  result,
  sending,
}: {
  address: string;
  onAddress: (value: string) => void;
  onSubmit: (event: SyntheticEvent) => Promise<void>;
  result: PasswordResetRequestOutcome | null;
  sending: boolean;
}): ReactElement {
  return (
    <>
      <BrandMark />
      <h1 className="g-auth__title">Recupera tu contraseña</h1>
      <p className="g-auth__lead">
        Escribe el correo de tu cuenta y te enviaremos un enlace para crear una nueva.
      </p>
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
      >
        <TextField
          autoComplete="email"
          label="Correo"
          onChange={onAddress}
          placeholder="tu@correo.com"
          type="email"
          value={address}
        />
        {result === null ? null : <RequestProblem outcome={result} />}
        <button
          className="g-button"
          data-variant="primary"
          disabled={address.trim() === '' || sending}
          style={{ width: '100%', marginTop: '16px' }}
          type="submit"
        >
          {sending ? 'Enviando…' : 'Enviar enlace'}
        </button>
      </form>
    </>
  );
}

/** El estado enviado. No confirma que la cuenta exista, solo que el correo salio. */
function ResetLinkSent({
  address,
  onBackToLogin,
}: {
  address: string;
  onBackToLogin: () => void;
}): ReactElement {
  return (
    <AuthLayout>
      <BrandMark />
      <div className="g-auth__badge">
        <Icon name="mark_email_read" />
      </div>
      <h1 className="g-auth__title">Revisa tu correo</h1>
      <p className="g-auth__lead">
        Si <strong>{address}</strong> tiene una cuenta, le acaba de llegar un enlace. Caduca en 30
        minutos.
      </p>
      <button
        className="g-button"
        data-variant="primary"
        onClick={onBackToLogin}
        style={{ width: '100%', marginTop: '8px' }}
        type="button"
      >
        Volver a iniciar sesión
      </button>
      <div className="g-auth__spacer" />
    </AuthLayout>
  );
}

function RequestProblem({
  outcome,
}: {
  outcome: PasswordResetRequestOutcome;
}): ReactElement | null {
  if (outcome.outcome === 'sent') return null;
  if (outcome.outcome === 'too-many-attempts') {
    return (
      <Notice icon="error" tone="error">
        Has pedido el enlace muchas veces seguidas. Espera un momento.
      </Notice>
    );
  }
  return (
    <Notice icon="error" tone="error">
      No pudimos enviar el enlace. Revisa tu conexión y vuelve a intentarlo.
    </Notice>
  );
}
