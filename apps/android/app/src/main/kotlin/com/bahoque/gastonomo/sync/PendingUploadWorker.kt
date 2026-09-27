package com.bahoque.gastonomo.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bahoque.gastonomo.data.offline.PendingUploader

/**
 * Sube lo que quedo pendiente en cuanto hay red.
 *
 * Es un `Worker` y no una corrutina propia por dos cosas que no hay que escribir: **sobrevive a
 * que se cierre la aplicacion** y reintenta con espera creciente. Sin eso, registrar un gasto en
 * el metro y cerrar la aplicacion dejaria el gasto sin subir hasta la proxima vez que el usuario
 * la abriera.
 *
 * Devuelve `retry()` y no `failure()` cuando queda cola: `failure` es definitivo para WorkManager
 * y se perderia el reintento.
 */
internal class PendingUploadWorker(
    context: Context,
    parameters: WorkerParameters,
    private val uploader: PendingUploader,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val outcome = uploader.uploadAll()
        return if (outcome.retryLater) Result.retry() else Result.success()
    }

    companion object {
        /**
         * Un solo trabajo a la vez. `KEEP` y no `REPLACE` porque reemplazarlo reiniciaria la
         * espera creciente en cada registro nuevo, y entonces una racha de registros sin red
         * dejaria el reintento siempre empezando de cero.
         */
        const val WORK_NAME = "gastonomo-pending-uploads"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request(),
            )
        }

        private fun request() = OneTimeWorkRequestBuilder<PendingUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
    }
}
