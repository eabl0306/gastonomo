import type { ReactElement } from 'react';

import { Icon } from '../auth/auth-chrome';

export interface PendingScreenProps {
  readonly title: string;
  readonly icon: string;
  /** Que va a ensenar esta pantalla cuando este. */
  readonly what: string;
}

/**
 * Una pantalla que todavia no esta.
 *
 * Existe porque el armazon se construye antes que las pantallas y las cuatro pestanas tienen
 * que llevar a algun sitio. Dice lo que va a haber en vez de quedarse en blanco: una pantalla
 * vacia parece rota, y esto parece lo que es.
 */
export function PendingScreen({ title, icon, what }: PendingScreenProps): ReactElement {
  return (
    <section>
      <h1 className="g-screen__title">{title}</h1>
      <div className="g-pending">
        <Icon name={icon} />
        <p className="g-pending__text">{what}</p>
      </div>
    </section>
  );
}
