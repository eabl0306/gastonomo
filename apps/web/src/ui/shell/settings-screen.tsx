import { useState, type ReactElement } from 'react';

import { signOut } from '../../application/use-cases/sign-out';
import { initialsOf, type User } from '../../domain/account/user';
import { Icon } from '../auth/auth-chrome';
import { useSessionPorts } from '../session/session-context';
import { useSession } from '../session/use-session';

/**
 * Ajustes, de momento solo con la tarjeta de perfil y cerrar sesion.
 *
 * Cerrar sesion vive aqui y no en la barra lateral porque es donde lo pone el diseno, y
 * porque un boton de salir a un clic de distancia en toda la aplicacion se pulsa sin querer.
 */
export function SettingsScreen({ onSignedOut }: { onSignedOut: () => void }): ReactElement {
  const { auth, session } = useSessionPorts();
  const stored = useSession();
  const [leaving, setLeaving] = useState(false);

  const leave = async (): Promise<void> => {
    setLeaving(true);
    await signOut(auth, session);
    onSignedOut();
  };

  return (
    <section>
      <h1 className="g-screen__title">Ajustes</h1>
      {stored === null ? null : <ProfileCard user={stored.user} />}
      <button
        className="g-signout"
        disabled={leaving}
        onClick={() => {
          void leave();
        }}
        type="button"
      >
        <Icon name="logout" />
        {leaving ? 'Cerrando…' : 'Cerrar sesión'}
      </button>
    </section>
  );
}

function ProfileCard({ user }: { user: User }): ReactElement {
  return (
    <div className="g-profile">
      <div className="g-profile__avatar">{initialsOf(user)}</div>
      <div className="g-profile__identity">
        <p className="g-profile__name">{user.name}</p>
        <p className="g-profile__email">{user.email}</p>
      </div>
    </div>
  );
}
