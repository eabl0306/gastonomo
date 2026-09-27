package com.bahoque.gastonomo.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.bahoque.gastonomo.data.offline.PendingUploader

/**
 * Le da al `Worker` lo que necesita de la raiz de composicion.
 *
 * WorkManager construye los workers por reflexion con un constructor de dos parametros, asi que
 * sin esto el worker tendria que buscarse la vida para llegar al contenedor, que es como acaba
 * habiendo un singleton global.
 */
internal class GastonomoWorkerFactory(
    private val uploader: () -> PendingUploader,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        PendingUploadWorker::class.java.name ->
            PendingUploadWorker(appContext, workerParameters, uploader())
        // null deja que WorkManager use su fabrica por defecto para lo que no sea nuestro.
        else -> null
    }
}
