import type { ReactElement } from 'react';

import type { PasswordResetOutcome } from '../../application/use-cases/reset-password';
import { MINIMUM_PASSWORD_LENGTH } from '../../domain/password';

import { AuthLayout, BrandMark, Icon, Notice } from './auth-chrome';

const MISMATCH = 'Las dos contraseñas no son iguales.';
const WEAK = `Usa al menos ${String(MINIMUM_PASSWORD_LENGTH)} caracteres.`;

/** El texto de cada fallo. El del 422 lo escribe la API, ya en castellano. */
export function Failure({ outcome }: { outcome: PasswordResetOutcome }): ReactElement | null {
  if (outcome.outcome === 'invalid') {
    return (
      <Notice icon="error" tone="error">
        {outcome.problem === 'weak' ? WEAK : MISMATCH}
      </Notice>
    );
  }
  if (outcome.outcome === 'failed') {
    return (
      <Notice icon="error" tone="error">
        {outcome.reason}
      </Notice>
    );
  }
  return null;
}

/** Un final de la pantalla: insignia, titulo, explicacion y una sola salida. */
function Ending({
  icon,
  badgeBackground,
  title,
  children,
  onFinished,
}: {
  icon: string;
  badgeBackground?: string;
  title: string;
  children: string;
  onFinished: () => void;
}): ReactElement {
  return (
    <AuthLayout>
      <BrandMark />
      <div
        className="g-auth__badge"
        {...(badgeBackground === undefined ? {} : { style: { background: badgeBackground } })}
      >
        <Icon filled name={icon} />
      </div>
      <h1 className="g-auth__title" style={{ marginTop: '24px' }}>
        {title}
      </h1>
      <p className="g-auth__lead">{children}</p>
      <button className="g-button" data-variant="primary" onClick={onFinished} type="button">
        Ir a iniciar sesión
      </button>
    </AuthLayout>
  );
}

export function Changed({ onFinished }: { onFinished: () => void }): ReactElement {
  return (
    <Ending icon="lock_reset" onFinished={onFinished} title="Contraseña cambiada">
      Ya puedes entrar con la nueva. Las sesiones que tenías abiertas se cerraron.
    </Ending>
  );
}

/**
 * El enlace no sirve: caduco, ya se uso, o no es de aqui. Los tres son el mismo
 * callejon para quien lo lee, y por eso la API tampoco los distingue.
 */
export function DeadLink({
  onFinished,
  missing = false,
}: {
  onFinished: () => void;
  missing?: boolean;
}): ReactElement {
  return (
    <Ending
      badgeBackground="var(--g-soft)"
      icon="link_off"
      onFinished={onFinished}
      title={missing ? 'Falta el enlace' : 'El enlace ya no sirve'}
    >
      {missing
        ? 'Abre el enlace desde el correo que te enviamos, sin quitarle nada a la dirección.'
        : 'Caducó o ya se usó. Los enlaces duran 30 minutos y solo sirven una vez: pide otro y vuelve a intentarlo.'}
    </Ending>
  );
}
