import { useId, type ReactElement } from 'react';

export interface TextFieldProps {
  readonly label: string;
  readonly value: string;
  readonly onChange: (value: string) => void;
  readonly type?: 'text' | 'email';
  readonly placeholder?: string | undefined;
  readonly autoComplete?: string | undefined;
  readonly invalid?: boolean;
  readonly describedBy?: string | undefined;
}

/**
 * Un campo de texto del diseno, con su label encima.
 *
 * El `type="email"` es lo que hace que un movil ofrezca el teclado con la arroba. **No se
 * usa para validar**: quien decide si un correo existe es la API, y un `type=email` que
 * bloquea el envio esconde ese error detras de un globo del navegador que no se puede
 * traducir ni maquetar.
 */
export function TextField({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  autoComplete,
  invalid = false,
  describedBy,
}: TextFieldProps): ReactElement {
  const id = useId();
  return (
    <div className="g-field">
      <label className="g-field__label" htmlFor={id}>
        {label}
      </label>
      <div className="g-field__shell">
        <input
          aria-describedby={describedBy}
          aria-invalid={invalid}
          autoComplete={autoComplete}
          className="g-field__input"
          data-invalid={invalid}
          id={id}
          onChange={(event) => {
            onChange(event.target.value);
          }}
          placeholder={placeholder}
          type={type}
          value={value}
        />
      </div>
    </div>
  );
}
