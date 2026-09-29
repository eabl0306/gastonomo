import { useId, useState, type ReactElement, type SyntheticEvent } from 'react';

import type { PasswordResetGateway } from '../../application/ports/password-reset-gateway';
import {
  resetPassword,
  type PasswordResetOutcome,
} from '../../application/use-cases/reset-password';
import { MINIMUM_PASSWORD_LENGTH } from '../../domain/password';

import { AuthLayout, BrandMark } from './auth-chrome';
import { PasswordField } from './password-field';
import { Changed, DeadLink, Failure } from './reset-outcomes';

export interface ResetPasswordScreenProps {
  readonly gateway: PasswordResetGateway;
  /** El token del enlace del correo. Vacio si alguien entro a pelo. */
  readonly token: string;
  readonly onFinished: () => void;
}

interface FormState {
  readonly password: string;
  readonly repeated: string;
  readonly sending: boolean;
  readonly problem: PasswordResetOutcome | null;
}

const EMPTY: FormState = { password: '', repeated: '', sending: false, problem: null };

/**
 * La pantalla que abre el enlace del correo.
 *
 * Tiene tres finales y son distintos a proposito: **cambiada** lleva a entrar,
 * **el enlace ya no sirve** no pide otra contrasena sino otro enlace, y
 * **cualquier otro fallo** deja el formulario tal cual para reintentar. Mezclar
 * los dos ultimos dejaria a alguien reescribiendo la contrasena contra un enlace
 * muerto.
 */
export function ResetPasswordScreen({
  gateway,
  token,
  onFinished,
}: ResetPasswordScreenProps): ReactElement {
  const [state, setState] = useState<FormState>(EMPTY);
  const [changed, setChanged] = useState(false);

  if (token.trim() === '') return <DeadLink missing onFinished={onFinished} />;
  if (changed) return <Changed onFinished={onFinished} />;
  if (state.problem?.outcome === 'link-unusable') return <DeadLink onFinished={onFinished} />;

  const submit = async (event: SyntheticEvent): Promise<void> => {
    event.preventDefault();
    setState((previous) => ({ ...previous, sending: true }));
    const result = await resetPassword(gateway, {
      token,
      newPassword: state.password,
      repeated: state.repeated,
    });
    if (result.outcome === 'changed') setChanged(true);
    setState((previous) => ({
      ...previous,
      sending: false,
      problem: result.outcome === 'changed' ? null : result,
    }));
  };

  return <ResetPasswordForm onChange={setState} onSubmit={submit} state={state} />;
}

function ResetPasswordForm({
  state,
  onChange,
  onSubmit,
}: {
  state: FormState;
  onChange: (next: FormState) => void;
  onSubmit: (event: SyntheticEvent) => Promise<void>;
}): ReactElement {
  const noticeId = useId();
  const invalid = state.problem?.outcome === 'invalid' ? state.problem.problem : null;

  return (
    <AuthLayout>
      <BrandMark />
      <h1 className="g-auth__title">Crea una contraseña nueva</h1>
      <p className="g-auth__lead">
        Escríbela dos veces. El enlace del correo solo sirve una vez, así que al guardar se cerrarán
        las sesiones abiertas en tus dispositivos.
      </p>
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
      >
        <Fields invalid={invalid} noticeId={noticeId} onChange={onChange} state={state} />
        <div id={noticeId}>
          {state.problem === null ? null : <Failure outcome={state.problem} />}
        </div>
        <button
          className="g-button"
          data-variant="primary"
          disabled={state.sending}
          style={{ width: '100%', marginTop: '24px' }}
          type="submit"
        >
          {state.sending ? 'Guardando…' : 'Guardar contraseña'}
        </button>
      </form>
    </AuthLayout>
  );
}

/** Los dos campos. Van aparte solo para que cada funcion quepa de un vistazo. */
function Fields({
  state,
  invalid,
  noticeId,
  onChange,
}: {
  state: FormState;
  invalid: 'weak' | 'mismatch' | null;
  noticeId: string;
  onChange: (next: FormState) => void;
}): ReactElement {
  const describedBy = state.problem === null ? undefined : noticeId;
  return (
    <>
      <PasswordField
        describedBy={describedBy}
        invalid={invalid === 'weak'}
        label="Contraseña nueva"
        onChange={(password) => {
          onChange({ ...state, password });
        }}
        placeholder={`Mínimo ${String(MINIMUM_PASSWORD_LENGTH)} caracteres`}
        value={state.password}
        withStrength
      />
      <PasswordField
        describedBy={describedBy}
        invalid={invalid === 'mismatch'}
        label="Repite la contraseña"
        onChange={(repeated) => {
          onChange({ ...state, repeated });
        }}
        value={state.repeated}
      />
    </>
  );
}
