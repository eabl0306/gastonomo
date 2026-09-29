/**
 * El usuario de la sesion. Alimenta el saludo de Inicio, las iniciales del avatar y la
 * tarjeta de perfil de Ajustes.
 */
export interface User {
  readonly id: string;
  readonly name: string;
  readonly email: string;
}

const WHITESPACE = /\s+/u;
const MAX_INITIALS = 2;

/** Las iniciales del avatar: una o dos letras en mayuscula. */
export function initialsOf(user: User): string {
  const words = user.name
    .trim()
    .split(WHITESPACE)
    .filter((word) => word !== '');
  const initials = words
    .slice(0, MAX_INITIALS)
    .map((word) => word.slice(0, 1).toUpperCase())
    .join('');
  return initials === '' ? user.email.slice(0, 1).toUpperCase() : initials;
}

/** El primer nombre, que es lo que usa "Hola, {nombre}". */
export function firstNameOf(user: User): string {
  return user.name.trim().split(WHITESPACE)[0] ?? '';
}
