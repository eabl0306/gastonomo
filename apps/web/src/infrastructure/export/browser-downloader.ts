import type { FileDownloader } from '../../application/ports/file-download';

/** Cuanto se espera antes de soltar la URL temporal. */
const RELEASE_DELAY_MS = 500;

/**
 * Cuanto se espera entre dos ficheros.
 *
 * Un navegador al que se le piden varias descargas seguidas descarta todas menos la primera.
 */
const BETWEEN_FILES_MS = 400;

/**
 * La descarga del navegador: un `<a download>` con una URL de objeto.
 *
 * La URL se libera con retraso y no en el acto: soltarla justo despues del clic cancela la
 * descarga en algunos navegadores, que todavia no han empezado a leer el blob.
 */
export const browserDownloader: FileDownloader = {
  download: (files) => {
    files.forEach((file, index) => {
      globalThis.setTimeout(() => {
        save(file);
      }, index * BETWEEN_FILES_MS);
    });
  },
};

function save(file: { name: string; content: string; mediaType: string }): void {
  const blob = new Blob([file.content], { type: file.mediaType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = file.name;
  document.body.append(anchor);
  anchor.click();
  globalThis.setTimeout(() => {
    URL.revokeObjectURL(url);
    anchor.remove();
  }, RELEASE_DELAY_MS);
}
