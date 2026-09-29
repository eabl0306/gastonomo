/**
 * La regla de la contrasena, la misma que aplica la API.
 *
 * Se repite aqui a proposito: sin ella el formulario manda una contrasena corta,
 * la API responde 422 y el usuario descubre el minimo despues de teclearla dos
 * veces. Lo que la web NO hace es fiarse de esto para dar por buena una: quien
 * decide sigue siendo la API, y por eso el 422 tiene su propio camino.
 */
export const MINIMUM_PASSWORD_LENGTH = 8;
export const MAXIMUM_PASSWORD_LENGTH = 128;

/** Por que una contrasena no vale todavia. `null` es que si vale. */
export type PasswordProblem = 'empty' | 'too-short' | 'too-long';

export function checkPassword(password: string): PasswordProblem | null {
  if (password === '') return 'empty';
  if (password.length < MINIMUM_PASSWORD_LENGTH) return 'too-short';
  if (password.length > MAXIMUM_PASSWORD_LENGTH) return 'too-long';
  return null;
}

/**
 * Los cinco niveles del medidor del diseno. No es una regla de seguridad, es una
 * pista para quien teclea: la unica condicion que se exige es la longitud.
 */
export type PasswordStrength = 0 | 1 | 2 | 3 | 4;

const SHORT_LENGTH = 6;

export function measurePassword(password: string): PasswordStrength {
  if (password.length === 0) return 0;
  if (password.length < SHORT_LENGTH) return 1;
  if (password.length < MINIMUM_PASSWORD_LENGTH) return 2;
  const mixed = /\d/u.test(password) && /[A-Za-z]/u.test(password);
  return mixed ? 4 : 3;
}
