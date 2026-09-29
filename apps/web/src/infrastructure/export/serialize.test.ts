import { describe, expect, it } from 'vitest';

import type { ExportTable } from '../../domain/export/table';
import { decimalOf } from '../../domain/money/decimal';

import { exportWriter } from './serialize';

const TABLE: ExportTable = {
  name: 'Movimientos',
  head: ['Fecha', 'Descripción', 'Monto (COP)'],
  rows: [
    [
      { kind: 'text', value: '2026-09-22' },
      { kind: 'text', value: 'Café; con "leche"' },
      { kind: 'number', value: decimalOf('-186400.0000') },
    ],
  ],
};

describe('exportar', () => {
  /**
   * El separador es el punto y coma: en un idioma con coma decimal, Excel entiende la coma
   * como decimal y parte las celdas donde no debe.
   */
  it('el CSV separa por punto y coma y entrecomilla lo que hace falta', () => {
    const [file] = exportWriter.files([TABLE], 'csv', '2026-09-22');

    expect(file?.name).toBe('gastonomo_movimientos_2026-09-22.csv');
    expect(file?.content).toContain('Fecha;Descripción;Monto (COP)');
    expect(file?.content).toContain('2026-09-22;"Café; con ""leche""";-186400.0000');
  });

  /** Sin el BOM, Excel se come las tildes. */
  it('el CSV lleva BOM y saltos de Windows', () => {
    const [file] = exportWriter.files([TABLE], 'csv', '2026-09-22');
    expect(file?.content.startsWith('﻿')).toBe(true);
    expect(file?.content).toContain('\r\n');
  });

  it('un CSV por tabla, porque un fichero plano no tiene hojas', () => {
    const files = exportWriter.files([TABLE, { ...TABLE, name: 'Abonos' }], 'csv', '2026-09-22');
    expect(files.map((file) => file.name)).toEqual([
      'gastonomo_movimientos_2026-09-22.csv',
      'gastonomo_abonos_2026-09-22.csv',
    ]);
  });

  it('Excel sale en un solo libro con una hoja por tabla', () => {
    const files = exportWriter.files([TABLE, { ...TABLE, name: 'Abonos' }], 'xlsx', '2026-09-22');

    expect(files).toHaveLength(1);
    expect(files[0]?.name).toBe('gastonomo_2026-09-22.xls');
    expect(files[0]?.content).toContain('<Worksheet ss:Name="Movimientos">');
    expect(files[0]?.content).toContain('<Worksheet ss:Name="Abonos">');
  });

  /** Un importe entrecomillado como texto no se puede sumar en la hoja. */
  it('el importe va como numero y con el punto decimal', () => {
    const [file] = exportWriter.files([TABLE], 'xlsx', '2026-09-22');
    expect(file?.content).toContain('<Data ss:Type="Number">-186400.0000</Data>');
  });

  it('el XML escapa lo que rompe el fichero', () => {
    const table: ExportTable = {
      ...TABLE,
      rows: [[{ kind: 'text', value: 'Pan & <queso>' }]],
    };
    const [file] = exportWriter.files([table], 'xlsx', '2026-09-22');
    expect(file?.content).toContain('Pan &amp; &lt;queso&gt;');
  });
});
