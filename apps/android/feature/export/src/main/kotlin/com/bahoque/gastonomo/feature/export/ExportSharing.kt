package com.bahoque.gastonomo.feature.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Escribe los archivos generados en la cache y los entrega a la hoja de compartir del
 * sistema.
 *
 * Van a la cache y no al almacenamiento externo porque son copias: el sistema las borra
 * cuando necesita sitio y la aplicacion no pide ni un permiso. Fuera del directorio que
 * declara `gastonomo_export_paths.xml` no se comparte nada.
 */
internal object ExportSharing {

    /**
     * Devuelve el Intent ya listo, o null si no se pudo escribir. Quien llama decide como
     * contarlo: aqui no se ensena nada.
     */
    fun intentFor(context: Context, payload: ExportPayload, title: String): Intent? =
        runCatching { buildIntent(context, payload, title) }.getOrNull()

    private fun buildIntent(context: Context, payload: ExportPayload, title: String): Intent {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        val authority = context.packageName + AUTHORITY_SUFFIX
        val uris = payload.documents.map { document ->
            val file = File(directory, document.fileName)
            file.writeBytes(document.bytes)
            FileProvider.getUriForFile(context, authority, file)
        }
        val single = uris.size == 1
        val intent = Intent(if (single) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = payload.documents.first().mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (single) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        return Intent.createChooser(intent, title)
    }

    /** El mismo subdirectorio que declara `gastonomo_export_paths.xml`. */
    private const val DIRECTORY = "exports"

    /** La misma autoridad que declara el manifiesto de esta feature. */
    private const val AUTHORITY_SUFFIX = ".exports"
}
