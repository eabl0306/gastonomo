import { describe, expect, it } from 'vitest';

import { monthPeriod, rangePeriod } from '../../domain/calendar/period';
import { COP } from '../../domain/money/currency';

import { dateFormats } from './date-formats';
import { gastonomoFormats } from './formats';

const LABELS = { today: 'Hoy', yesterday: 'Ayer' };
const TODAY = '2026-09-22';

const es = dateFormats('es');
const en = dateFormats('en');

describe('formato de fechas', () => {
  /** Los cortos del CLDR no valen: los devuelve con punto y en ingles cambia el orden. */
  it('usa los nombres del diseno, no los del navegador', () => {
    expect(es.short('2026-09-22')).toBe('22 sept');
    expect(es.long('2026-09-22')).toBe('22 sept 2026');
    expect(en.long('2026-09-22')).toBe('22 Sep 2026');
    expect(es.short('2026-01-05')).toBe('5 ene');
  });

  it('el mes del selector se capitaliza y el de una frase no, salvo en ingles', () => {
    expect(es.monthAndYear('2026-09')).toBe('Septiembre 2026');
    expect(es.monthName('2026-09')).toBe('septiembre');
    expect(en.monthName('2026-09')).toBe('September');
  });

  it('la cabecera de Inicio lleva coma y el grupo de dia no', () => {
    expect(es.weekdayWithDate(TODAY)).toBe('Martes, 22 sept');
    expect(es.dayGroup('2026-09-19', TODAY, LABELS)).toBe('Sábado 19 sept');
  });

  it('el grupo de dia dice Hoy y Ayer', () => {
    expect(es.dayGroup(TODAY, TODAY, LABELS)).toBe('Hoy');
    expect(es.dayGroup('2026-09-21', TODAY, LABELS)).toBe('Ayer');
  });

  /** El separador es el en dash U+2013, y el ano sale solo al final. */
  it('el rango junta las dos fechas con un en dash', () => {
    expect(es.range('2026-08-22', '2026-09-22')).toBe('22 ago – 22 sept 2026');
  });

  it('la etiqueta del selector cambia con el modo', () => {
    const formats = gastonomoFormats({ displayCurrency: COP, language: 'es' });
    expect(formats.periodLabel(monthPeriod('2026-09'))).toBe('Septiembre 2026');
    expect(formats.periodLabel(rangePeriod('2026-08-22', '2026-09-22'))).toBe(
      '22 ago – 22 sept 2026',
    );
  });
});
