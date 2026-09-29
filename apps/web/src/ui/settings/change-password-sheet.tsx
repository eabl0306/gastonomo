import { useState, type ReactElement } from 'react';

import type { AuthGateway } from '../../application/ports/auth-gateway';
import { MINIMUM_PASSWORD_LENGTH } from '../../domain/password';
import { Notice } from '../auth/auth-chrome';
import { PasswordField } from '../auth/password-field';
import { messageOf } from '../common/blocks';
import { Sheet } from '../common/sheet';
import { useSession } from '../session/use-session';

/**
 * Cambiar la contrasena desde dentro.
 *
 * Se manda el refresh token de esta sesion para no cerrarla: sin el, la API revoca todas y
 * quien acaba de cambiar su contrasena se encuentra de vuelta en Entrar.
 */
export function ChangePasswordSheet({
  auth,
  onClose,
  onChanged,
}: {
  auth: AuthGateway;
  onClose: () => void;
  onChanged: () => void;
}): ReactElement {
  const session = useSession();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canSave = current !== '' && next.length >= MINIMUM_PASSWORD_LENGTH && !saving;

  const submit = (): void => {
    if (!canSave || session === null) return;
    setSaving(true);
    setError(null);
    void auth.changePassword(current, next, session.refreshToken).then((outcome) => {
      setSaving(false);
      if (outcome.ok) onChanged();
      else setError(messageFor(outcome.error));
    });
  };

  return (
    <Sheet
      onClose={onClose}
      subtitle="Seguirás dentro en este dispositivo. Las demás sesiones se cerrarán."
      title="Cambiar contraseña"
    >
      <Fields current={current} next={next} onCurrent={setCurrent} onNext={setNext} />
      {error === null ? null : (
        <Notice icon="error" tone="error">
          {error}
        </Notice>
      )}
      <button className="g-sheet__apply" disabled={!canSave} onClick={submit} type="button">
        {saving ? 'Guardando…' : 'Cambiar contraseña'}
      </button>
    </Sheet>
  );
}

function Fields({
  current,
  next,
  onCurrent,
  onNext,
}: {
  current: string;
  next: string;
  onCurrent: (value: string) => void;
  onNext: (value: string) => void;
}): ReactElement {
  return (
    <div className="g-settings__fields">
      <PasswordField
        autoComplete="current-password"
        label="Contraseña actual"
        onChange={onCurrent}
        value={current}
      />
      <PasswordField
        autoComplete="new-password"
        label="Contraseña nueva"
        onChange={onNext}
        value={next}
        withStrength
      />
    </div>
  );
}

/** Un 401 aqui no es la sesion: es que la contrasena actual no es la que se escribio. */
function messageFor(error: Parameters<typeof messageOf>[0]): string {
  if (error.kind === 'unauthorized' || error.kind === 'invalid-credentials') {
    return 'La contraseña actual no es correcta.';
  }
  if (error.kind === 'validation') {
    return error.fields[0]?.message ?? 'La contraseña nueva no cumple los requisitos.';
  }
  return messageOf(error);
}
