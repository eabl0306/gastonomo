import type { ReactElement } from 'react';

import { firstNameOf, initialsOf, type User } from '../../domain/account/user';
import type { Day } from '../../domain/calendar/day';
import { useFormats } from '../format/formats-context';

/**
 * El encabezado de Inicio: la fecha de hoy, el saludo y, solo en movil, el avatar que lleva
 * a Ajustes. En escritorio la identidad esta en el pie de la barra lateral.
 *
 * Sin el perfil se saluda sin nombre y el avatar sale vacio, en vez de dejar un hueco: el
 * resto de la pantalla ya tiene datos que ensenar.
 */
export function HomeHeader({
  today,
  user,
  onOpenSettings,
}: {
  today: Day;
  user: User | null;
  onOpenSettings: () => void;
}): ReactElement {
  const formats = useFormats();
  const name = user === null ? '' : firstNameOf(user);
  return (
    <header className="g-home__head">
      <div>
        <p className="g-home__date">{formats.dates.weekdayWithDate(today)}</p>
        <p className="g-home__greeting">{name === '' ? 'Hola' : `Hola, ${name}`}</p>
      </div>
      <button
        aria-label="Abrir ajustes"
        className="g-home__avatar"
        onClick={onOpenSettings}
        type="button"
      >
        {user === null ? '' : initialsOf(user)}
      </button>
    </header>
  );
}
