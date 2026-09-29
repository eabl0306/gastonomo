import type { ReactElement, ReactNode } from 'react';

import { Icon } from '../auth/auth-chrome';

/** Un grupo de filas de Ajustes, con su nota opcional debajo. */
export function SettingsGroup({
  title,
  note,
  children,
}: {
  title: string;
  note?: string;
  children: ReactNode;
}): ReactElement {
  return (
    <section className="g-settings__group">
      <h2 className="g-settings__title">{title}</h2>
      <div className="g-settings__card">{children}</div>
      {note === undefined ? null : <p className="g-settings__note">{note}</p>}
    </section>
  );
}

/** Una fila que lleva a otra pantalla o abre un sheet. */
export function SettingsLink({
  icon,
  label,
  value,
  onClick,
}: {
  icon: string;
  label: string;
  value: string;
  onClick: () => void;
}): ReactElement {
  return (
    <button className="g-settings__row" onClick={onClick} type="button">
      <span className="g-settings__icon">
        <Icon name={icon} />
      </span>
      <span className="g-settings__label">{label}</span>
      <span className="g-settings__value">
        {value}
        <Icon name="chevron_right" />
      </span>
    </button>
  );
}

/** Una fila que solo informa. No se pulsa: no hay nada que abrir. */
export function SettingsStatus({
  icon,
  label,
  value,
  tone,
}: {
  icon: string;
  label: string;
  value: string;
  tone: 'ok' | 'muted';
}): ReactElement {
  return (
    <div className="g-settings__row" data-static="">
      <span className="g-settings__icon">
        <Icon name={icon} />
      </span>
      <span className="g-settings__label">{label}</span>
      <span className="g-settings__value" data-tone={tone}>
        {value}
      </span>
    </div>
  );
}
