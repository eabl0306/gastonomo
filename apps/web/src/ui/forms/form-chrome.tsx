import type { ReactElement, ReactNode } from 'react';

import { Icon } from '../auth/auth-chrome';

/**
 * El armazon de un formulario: la barra con la X, el contenido con scroll y el boton de
 * guardar pegado abajo.
 *
 * El boton va fuera del scroll porque el diseno lo quiere siempre a la vista: en un formulario
 * largo, tener que bajar para encontrarlo es lo que hace que se abandone a medias.
 */
export function FormLayout({
  title,
  onClose,
  action,
  children,
}: {
  title: string;
  onClose: () => void;
  action: ReactNode;
  children: ReactNode;
}): ReactElement {
  return (
    <div className="g-form">
      <header className="g-form__bar">
        <button aria-label="Cerrar" className="g-form__close" onClick={onClose} type="button">
          <Icon name="close" />
        </button>
        <h1 className="g-form__title">{title}</h1>
      </header>
      <div className="g-form__body">{children}</div>
      <div className="g-form__footer">{action}</div>
    </div>
  );
}

/** El boton primario de guardar. Apagado no es invisible: dice que falta algo. */
export function SaveButton({
  label,
  disabled,
  onClick,
}: {
  label: string;
  disabled: boolean;
  onClick: () => void;
}): ReactElement {
  return (
    <button className="g-form__save" disabled={disabled} onClick={onClick} type="button">
      {label}
    </button>
  );
}

/** Las dos pestanas de arriba: ingreso y gasto, o presté y me prestaron. */
export function SegmentedTabs<T extends string>({
  options,
  selected,
  onSelect,
  label,
}: {
  options: readonly { readonly value: T; readonly label: string; readonly icon?: string }[];
  selected: T;
  onSelect: (value: T) => void;
  label: string;
}): ReactElement {
  return (
    <div aria-label={label} className="g-segments" role="tablist">
      {options.map((option) => (
        <button
          aria-selected={option.value === selected}
          className="g-segment"
          data-active={option.value === selected}
          data-value={option.value}
          key={option.value}
          onClick={() => {
            onSelect(option.value);
          }}
          role="tab"
          type="button"
        >
          {option.icon === undefined ? null : <Icon name={option.icon} />}
          {option.label}
        </button>
      ))}
    </div>
  );
}

/** Un campo con su etiqueta. Es el de 52px del diseno, no el compacto del sheet de rango. */
export function FormField({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}): ReactElement {
  return (
    <label className="g-field">
      <span className="g-field__label">
        {label}
        {hint === undefined ? null : <span className="g-field__hint"> {hint}</span>}
      </span>
      {children}
    </label>
  );
}
