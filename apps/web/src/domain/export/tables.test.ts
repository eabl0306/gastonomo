import { describe, expect, it } from 'vitest';

import {
  SAMPLE_LOANS,
  SAMPLE_MOVEMENTS,
  SAMPLE_RATES,
} from '../../infrastructure/memory/sample-ledger';
import { COP } from '../money/currency';
import { decimalToString } from '../money/decimal';

import type { ExportCell } from './table';
import { tableOf, type ExportInput } from './tables';

const INPUT: ExportInput = {
  movements: SAMPLE_MOVEMENTS,
  loans: SAMPLE_LOANS,
  display: COP,
  rates: SAMPLE_RATES,
};

function text(cell: ExportCell | undefined): string {
  if (cell === undefined) return '';
  return cell.kind === 'number' ? decimalToString(cell.value) : cell.value;
}

describe('tablas de exportacion', () => {
  it('los movimientos van del mas antiguo al mas reciente', () => {
    const table = tableOf('movements', INPUT);
    expect(table.rows.map((row) => text(row[0]))).toEqual([
      '2026-09-12',
      '2026-09-15',
      '2026-09-18',
      '2026-09-20',
    ]);
  });

  /** Quien sume la columna en la hoja tiene que obtener el neto, no lo que se movio. */
  it('un gasto va con signo negativo y un ingreso con signo positivo', () => {
    const rows = tableOf('movements', INPUT).rows;
    const taxi = rows[0];
    const freelance = rows[3];

    expect(text(taxi?.[1])).toBe('Gasto');
    expect(text(taxi?.[6])).toBe('-18500');
    expect(text(freelance?.[1])).toBe('Ingreso');
    expect(text(freelance?.[6])).toBe('560000');
  });

  /** Un "$ 186.400" con separador de miles no es un numero para una hoja de calculo. */
  it('los importes van como numero y con el punto decimal', () => {
    const row = tableOf('movements', INPUT).rows[3];
    expect(row?.[4]).toEqual({ kind: 'number', value: { units: 1400000n, scale: 4 } });
    expect(text(row?.[5])).toBe('USD');
  });

  it('el importe en la moneda principal se redondea a sus decimales', () => {
    const rows = tableOf('loans', INPUT).rows;
    expect(text(rows[0]?.[6])).toBe('400000');
    expect(text(rows[0]?.[8])).toBe('250000');
    expect(text(rows[0]?.[9])).toBe('Parcial');
  });

  it('los abonos de todos los prestamos van juntos y ordenados', () => {
    const table = tableOf('payments', INPUT);
    expect(table.rows.map((row) => text(row[0]))).toEqual(['2026-06-20', '2026-09-01']);
    expect(text(table.rows[0]?.[1])).toBe('Marta');
    expect(text(table.rows[0]?.[2])).toBe('Todo de una');
    // Sin concepto, la celda dice "Abono": una celda vacia no dice que fue un abono.
    expect(text(table.rows[1]?.[2])).toBe('Abono');
  });

  it('la cabecera dice en que moneda esta la columna convertida', () => {
    expect(tableOf('movements', INPUT).head[6]).toBe('Monto (COP)');
  });
});
