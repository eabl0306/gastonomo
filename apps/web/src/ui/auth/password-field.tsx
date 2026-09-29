import { useId, useState, type ReactElement } from 'react';

import { measurePassword, type PasswordStrength } from '../../domain/password';

import { Icon } from './auth-chrome';

/** Los cinco textos del medidor, en el orden de los cinco niveles. */
const STRENGTH_LABELS = ['Mínimo 8 caracteres', 'Muy corta', 'Casi', 'Buena', 'Segura'] as const;

function barColor(bar: number, level: PasswordStrength): string {
  if (bar > level) return 'var(--g-field-line)';
  if (level <= 1) return 'var(--g-red)';
  if (level === 2) return 'var(--g-amber)';
  return 'var(--g-green)';
}

/**
 * El medidor del diseno. **No decide** si la contrasena vale: eso lo dice la
 * regla del dominio y, en ultima instancia, la API. Aqui solo acompana a quien
 * teclea, y por eso queda fuera del arbol de accesibilidad.
 */
function StrengthMeter({ password }: { password: string }): ReactElement {
  const level = measurePassword(password);
  return (
    <>
      <div aria-hidden="true" className="g-strength">
        {[1, 2, 3, 4].map((bar) => (
          <div className="g-strength__bar" key={bar} style={{ background: barColor(bar, level) }} />
        ))}
      </div>
      <p className="g-strength__label">{STRENGTH_LABELS[level]}</p>
    </>
  );
}

/** El ojo de mostrar y ocultar, que el diseno pone dentro del propio campo. */
function RevealButton({ shown, onToggle }: { shown: boolean; onToggle: () => void }): ReactElement {
  return (
    <button
      aria-label={shown ? 'Ocultar la contraseña' : 'Mostrar la contraseña'}
      className="g-field__affordance"
      onClick={onToggle}
      type="button"
    >
      <Icon name={shown ? 'visibility_off' : 'visibility'} />
    </button>
  );
}

export interface PasswordFieldProps {
  readonly label: string;
  readonly value: string;
  readonly onChange: (value: string) => void;
  readonly placeholder?: string | undefined;
  readonly invalid?: boolean;
  readonly describedBy?: string | undefined;
  /** El medidor solo acompana a la contrasena nueva, no a la repeticion. */
  readonly withStrength?: boolean;
}

/** El campo de contrasena del diseno, con su ojo y, si se pide, su medidor. */
export function PasswordField({
  label,
  value,
  onChange,
  placeholder,
  invalid = false,
  describedBy,
  withStrength = false,
}: PasswordFieldProps): ReactElement {
  const [shown, setShown] = useState(false);
  const id = useId();

  return (
    <>
      <label className="g-field__label" htmlFor={id}>
        {label}
      </label>
      <div className="g-field__shell">
        <input
          aria-describedby={describedBy}
          aria-invalid={invalid}
          autoComplete="new-password"
          className="g-field__input"
          data-invalid={invalid}
          data-with-affordance="true"
          id={id}
          onChange={(event) => {
            onChange(event.target.value);
          }}
          placeholder={placeholder}
          type={shown ? 'text' : 'password'}
          value={value}
        />
        <RevealButton
          onToggle={() => {
            setShown((previous) => !previous);
          }}
          shown={shown}
        />
      </div>
      {withStrength ? <StrengthMeter password={value} /> : null}
    </>
  );
}
