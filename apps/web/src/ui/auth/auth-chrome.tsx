import type { ReactElement, ReactNode } from 'react';

/** Un icono de Material Symbols. El nombre del icono va como texto. */
export function Icon({ name, filled = false }: { name: string; filled?: boolean }): ReactElement {
  return (
    <span aria-hidden="true" className="g-icon" data-filled={filled}>
      {name}
    </span>
  );
}

/** La marca. En el panel oscuro se invierten los colores y el tile es mayor. */
export function BrandMark({ onInk = false }: { onInk?: boolean }): ReactElement {
  return (
    <div className="g-brand" data-on-ink={onInk}>
      <div className="g-brand__tile">g</div>
      <span className="g-brand__word">gastonomo</span>
    </div>
  );
}

/**
 * El esqueleto de las pantallas de autenticacion.
 *
 * El panel de marca solo existe de 900px para arriba, y lo decide CSS y no
 * JavaScript: asi no hay un primer fotograma con el ancho equivocado ni hace
 * falta escuchar el `resize`.
 */
export function AuthLayout({ children }: { children: ReactNode }): ReactElement {
  return (
    <div className="g-auth">
      <aside className="g-auth__panel">
        <BrandMark onInk />
        <div className="g-auth__panel-spacer" />
        <p className="g-auth__claim">Movimientos, préstamos y abonos en un solo lugar.</p>
        <p className="g-auth__claim-note">
          Tus cuentas se sincronizan entre el celular y el computador.
        </p>
        <div className="g-auth__panel-spacer" />
        <p className="g-auth__panel-footer">Gastonomo · Finanzas personales</p>
      </aside>
      <main className="g-auth__column">{children}</main>
    </div>
  );
}

/** Un aviso en linea. `error` es bloqueante; `warning` solo informa. */
export function Notice({
  tone,
  icon,
  children,
}: {
  tone: 'error' | 'warning';
  icon: string;
  children: ReactNode;
}): ReactElement {
  return (
    <p className="g-notice" data-tone={tone} role={tone === 'error' ? 'alert' : undefined}>
      <Icon name={icon} />
      <span>{children}</span>
    </p>
  );
}
