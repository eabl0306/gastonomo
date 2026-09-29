import type { DownloadableFile, ExportWriter } from '../../application/ports/file-download';
import type { ExportCell, ExportTable } from '../../domain/export/table';
import { decimalToString } from '../../domain/money/decimal';

/**
 * El separador de un CSV pensado para abrirse en Excel en castellano.
 *
 * Es el punto y coma y no la coma porque en un locale con coma decimal, Excel entiende la coma
 * como separador decimal y parte las celdas donde no debe.
 */
const SEPARATOR = ';';

/** Windows-1252 no, UTF-8 con BOM: sin el, Excel se come las tildes. */
const BOM = '﻿';

const CSV_MEDIA_TYPE = 'text/csv;charset=utf-8';
const XLS_MEDIA_TYPE = 'application/vnd.ms-excel';

function csvCell(cell: ExportCell): string {
  const text = cell.kind === 'number' ? decimalToString(cell.value) : cell.value;
  return /[;"\n]/u.test(text) ? `"${text.replace(/"/gu, '""')}"` : text;
}

function toCsv(table: ExportTable, stamp: string, key: string): DownloadableFile {
  const lines = [
    table.head.join(SEPARATOR),
    ...table.rows.map((row) => row.map(csvCell).join(SEPARATOR)),
  ];
  return {
    name: `gastonomo_${key}_${stamp}.csv`,
    content: BOM + lines.join('\r\n'),
    mediaType: CSV_MEDIA_TYPE,
  };
}

function xmlEscape(text: string): string {
  return text.replace(/&/gu, '&amp;').replace(/</gu, '&lt;').replace(/>/gu, '&gt;');
}

function xmlCell(cell: ExportCell, header: boolean): string {
  const style = header ? ' ss:StyleID="h"' : '';
  if (cell.kind === 'number') {
    // El punto decimal es obligatorio: el XML de Excel no depende del idioma del sistema.
    return `<Cell${style}><Data ss:Type="Number">${decimalToString(cell.value)}</Data></Cell>`;
  }
  return `<Cell${style}><Data ss:Type="String">${xmlEscape(cell.value)}</Data></Cell>`;
}

function worksheet(table: ExportTable): string {
  const head = `<Row>${table.head.map((text) => xmlCell({ kind: 'text', value: text }, true)).join('')}</Row>`;
  const rows = table.rows
    .map((row) => `<Row>${row.map((cell) => xmlCell(cell, false)).join('')}</Row>`)
    .join('');
  return `<Worksheet ss:Name="${xmlEscape(table.name)}"><Table>${head}${rows}</Table></Worksheet>`;
}

/**
 * Un solo libro con una hoja por conjunto, en SpreadsheetML.
 *
 * Es XML y no un .xlsx de verdad porque un .xlsx es un zip, y generarlo pediria una libreria
 * entera para algo que Excel y LibreOffice abren igual. Por eso la extension es `.xls`.
 */
function toWorkbook(tables: readonly ExportTable[], stamp: string): DownloadableFile {
  const content =
    '<?xml version="1.0" encoding="UTF-8"?><?mso-application progid="Excel.Sheet"?>' +
    '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" ' +
    'xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">' +
    '<Styles><Style ss:ID="h"><Font ss:Bold="1"/></Style></Styles>' +
    tables.map(worksheet).join('') +
    '</Workbook>';
  return { name: `gastonomo_${stamp}.xls`, content, mediaType: XLS_MEDIA_TYPE };
}

/** Las claves de fichero, que van en el nombre y por eso no llevan tildes. */
const FILE_KEYS: Readonly<Record<string, string>> = {
  Movimientos: 'movimientos',
  Préstamos: 'prestamos',
  Abonos: 'abonos',
};

/** Excel sale en un solo libro; CSV, en un fichero por tabla. */
export const exportWriter: ExportWriter = {
  files: (tables, format, stamp) => {
    if (format === 'xlsx') return [toWorkbook(tables, stamp)];
    return tables.map((table) => toCsv(table, stamp, FILE_KEYS[table.name] ?? 'datos'));
  },
};
