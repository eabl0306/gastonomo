import { useId, type ReactElement } from 'react';

import { Icon } from './auth-chrome';

export interface TermsCheckboxProps {
  readonly checked: boolean;
  readonly onChange: (checked: boolean) => void;
  readonly invalid?: boolean;
}

/**
 * La casilla de los terminos.
 *
 * El cuadrado que se ve es un `span`; el `input` de verdad esta debajo, invisible pero
 * presente: asi el teclado y el lector de pantalla siguen funcionando, que es lo que se
 * pierde cuando una casilla se dibuja con un `div` y un `onClick`.
 */
export function TermsCheckbox({
  checked,
  onChange,
  invalid = false,
}: TermsCheckboxProps): ReactElement {
  const id = useId();
  return (
    <label className="g-terms" data-invalid={invalid} htmlFor={id}>
      <span className="g-terms__box" data-checked={checked}>
        <input
          checked={checked}
          className="g-terms__input"
          id={id}
          onChange={(event) => {
            onChange(event.target.checked);
          }}
          type="checkbox"
        />
        {checked ? <Icon name="check" /> : null}
      </span>
      <span>Acepto los términos de uso y la política de privacidad.</span>
    </label>
  );
}
