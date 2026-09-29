/**
 * Las rutas de la web, en un solo sitio.
 *
 * `RESET_PASSWORD` no se puede tocar a la ligera: es la ruta que la API mete en
 * el correo de recuperacion, y su valor esta fijado en `compose.prod.yaml`, no
 * en el `.env` del servidor. Cambiarla aqui sin cambiarla alli deja el enlace
 * roto otra vez, que es exactamente el fallo que esta pantalla vino a arreglar.
 */
export const ROUTES = {
  home: '/',
  resetPassword: '/reset-password',
} as const;

/** El parametro que la API pone en el enlace del correo. */
export const RESET_TOKEN_PARAM = 'token';
