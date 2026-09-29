import type { Decimal } from '../money/decimal';

/**
 * Una celda de la exportacion.
 *
 * El importe va como [Decimal] y no como texto ya formateado: quien abra el fichero tiene que
 * poder sumar la columna, y un "$ 186.400" con separador de miles no es un numero para una
 * hoja de calculo.
 */
export type ExportCell =
  | { readonly kind: 'text'; readonly value: string }
  | { readonly kind: 'number'; readonly value: Decimal };

export function textCell(value: string): ExportCell {
  return { kind: 'text', value };
}

export function numberCell(value: Decimal): ExportCell {
  return { kind: 'number', value };
}

/** Una tabla de la exportacion: una hoja del libro, o un fichero CSV. */
export interface ExportTable {
  readonly name: string;
  readonly head: readonly string[];
  readonly rows: readonly (readonly ExportCell[])[];
}
