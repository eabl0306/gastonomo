package com.bahoque.gastonomo.feature.export

/**
 * Un archivo ya generado, listo para escribir en disco y pasar a la hoja de compartir.
 *
 * Se genera en memoria a proposito: un CSV de una cuenta personal cabe de sobra, y asi el
 * ViewModel no necesita ni un Context ni acceso al sistema de ficheros.
 */
data class ExportDocument(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    // ByteArray compara por identidad, asi que un data class con uno dentro necesita esto
    // para que dos documentos con el mismo contenido sean iguales en un test.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExportDocument) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int =
        fileName.hashCode() * HASH_FACTOR + mimeType.hashCode() * HASH_FACTOR + bytes.contentHashCode()

    private companion object {
        const val HASH_FACTOR = 31
    }
}

/** Lo que se comparte de una vez: uno o varios archivos y cuantas filas llevan entre todos. */
data class ExportPayload(val documents: List<ExportDocument>, val rows: Int)
