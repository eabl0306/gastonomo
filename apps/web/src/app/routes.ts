/**
 * Las rutas de la web, en un solo sitio.
 *
 * `RESET_PASSWORD` no se puede tocar a la ligera: es la ruta que la API mete en el correo de
 * recuperacion, y su valor esta fijado en `compose.prod.yaml`, no en el `.env` del servidor.
 * Cambiarla aqui sin cambiarla alli deja el enlace roto otra vez, que es exactamente el fallo
 * que esa pantalla vino a arreglar.
 *
 * Las demas van en ingles por coherencia con esa, que es la unica que no se puede mover.
 */
export const ROUTES = {
  home: '/',
  movements: '/movements',
  newMovement: '/movements/new',
  loans: '/loans',
  newLoan: '/loans/new',
  loanDetail: '/loans/:loanId',
  settings: '/settings',
  login: '/login',
  register: '/register',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  status: '/estado',
} as const;

/**
 * La pantalla de diagnostico **no** puede vivir en `/health`.
 *
 * `/health` es de la API: el proxy de desarrollo y el Caddy de produccion mandan ese prefijo
 * al servicio, asi que el navegador nunca veria la SPA. Se descubrio con los e2e en rojo.
 */
/** La direccion del detalle de un prestamo concreto. */
export function loanDetailPath(loanId: string): string {
  return `/loans/${encodeURIComponent(loanId)}`;
}

/** El parametro que la API pone en el enlace del correo. */
export const RESET_TOKEN_PARAM = 'token';

/**
 * Donde volver despues de entrar.
 *
 * Se guarda en el estado de la navegacion, no en la direccion: un `?next=` es una puerta
 * abierta a que alguien mande un enlace que, tras entrar, lleve a donde el quiera.
 */
export interface LoginLocationState {
  readonly from?: string;
  readonly email?: string;
}
