import type { ReactElement } from 'react';

import type { ApiError } from '../../application/ports/api-error';
import { Icon } from '../auth/auth-chrome';

/** El texto de cada fallo, en los mismos terminos que Android. */
export function messageOf(error: ApiError): string {
  if (error.kind === 'network') return 'No hay conexión. Inténtalo de nuevo.';
  if (error.kind === 'unauthorized') return 'Tu sesión caducó. Entra otra vez.';
  return 'Algo no fue bien. Inténtalo de nuevo.';
}

/** El bloque de carga que ocupa el sitio del contenido mientras llega. */
export function LoadingBlock(): ReactElement {
  return (
    <div className="g-block" role="status">
      <span className="g-spinner" />
      <p className="g-block__text">Cargando…</p>
    </div>
  );
}

/** El bloque de fallo, con su boton de reintentar. */
export function RetryBlock({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}): ReactElement {
  return (
    <div className="g-block" role="alert">
      <p className="g-block__text">{message}</p>
      <button className="g-block__retry" onClick={onRetry} type="button">
        Reintentar
      </button>
    </div>
  );
}

/**
 * Una linea de aviso que no bloquea, como la de las tasas que aun no estan.
 *
 * No es un error: lo que esta en la moneda principal se ensena igual.
 */
export function NoteLine({ children }: { children: string }): ReactElement {
  return (
    <p className="g-note-line">
      <Icon name="info" />
      <span>{children}</span>
    </p>
  );
}
