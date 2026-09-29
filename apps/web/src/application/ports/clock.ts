import type { Day } from '../../domain/calendar/day';

/**
 * El dia de hoy.
 *
 * Es un puerto y no una llamada a `Date` porque de el dependen el tope del selector de mes,
 * los cinco presets de rango y el corte de la tendencia: con el reloj real, esos tests solo
 * pasarian el dia que se escribieron.
 */
export interface Clock {
  today: () => Day;
}
