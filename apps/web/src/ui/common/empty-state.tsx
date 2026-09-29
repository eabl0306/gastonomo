import type { ReactElement } from 'react';

import { Icon } from '../auth/auth-chrome';

/** El hueco de una lista vacia: un icono grande apagado y una linea. */
export function EmptyState({ icon, message }: { icon: string; message: string }): ReactElement {
  return (
    <div className="g-empty">
      <span className="g-empty__icon">
        <Icon name={icon} />
      </span>
      {message}
    </div>
  );
}
