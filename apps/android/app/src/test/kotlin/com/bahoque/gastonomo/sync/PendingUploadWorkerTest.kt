package com.bahoque.gastonomo.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.bahoque.gastonomo.data.offline.PendingUploader
import com.bahoque.gastonomo.data.offline.UploadOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * El worker existe para dos cosas que no hay que escribir: sobrevivir a que se cierre la
 * aplicacion y reintentar con espera creciente. Lo que si hay que comprobar es que **pide
 * reintento en vez de rendirse** mientras quede cola: `failure` es definitivo para WorkManager y
 * se perderia lo que el usuario registro.
 */
@RunWith(AndroidJUnit4::class)
class PendingUploadWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `while something is left it asks to be retried`() = runTest {
        val worker = workerWith(UploadOutcome(uploaded = 1, retryLater = true))

        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
    }

    @Test
    fun `with the queue empty it is done`() = runTest {
        val worker = workerWith(UploadOutcome(uploaded = 2, retryLater = false))

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun `it does not upload by itself, it asks whoever knows how`() = runTest {
        var calls = 0
        val worker = workerWith(UploadOutcome(0, false)) { calls++ }

        worker.doWork()

        assertEquals(1, calls)
    }

    private fun workerWith(
        outcome: UploadOutcome,
        onUpload: () -> Unit = {},
    ): PendingUploadWorker = TestListenableWorkerBuilder<PendingUploadWorker>(context)
        .setWorkerFactory(
            GastonomoWorkerFactory {
                object : PendingUploader {
                    override suspend fun uploadAll(): UploadOutcome {
                        onUpload()
                        return outcome
                    }
                }
            },
        )
        .build()
}
