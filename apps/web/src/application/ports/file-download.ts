import type { ExportTable } from '../../domain/export/table';

/** Un fichero listo para descargar. */
export interface DownloadableFile {
  readonly name: string;
  readonly content: string;
  readonly mediaType: string;
}

/** Los dos formatos de la exportacion. */
export type ExportFormat = 'xlsx' | 'csv';

/**
 * Convierte las tablas en ficheros.
 *
 * Es un puerto porque el formato es un detalle: Excel sale en un solo libro con una hoja por
 * tabla, y CSV en un fichero por tabla, porque un fichero plano no tiene hojas.
 */
export interface ExportWriter {
  files: (
    tables: readonly ExportTable[],
    format: ExportFormat,
    stamp: string,
  ) => readonly DownloadableFile[];
}

/**
 * Entrega los ficheros al usuario.
 *
 * Recibe la lista entera y no uno a uno: un navegador al que se le piden varias descargas a la
 * vez descarta todas menos la primera, y espaciarlas es cosa del adaptador.
 */
export interface FileDownloader {
  download: (files: readonly DownloadableFile[]) => void;
}
