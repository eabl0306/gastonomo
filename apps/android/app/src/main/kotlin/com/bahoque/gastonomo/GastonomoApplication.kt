package com.bahoque.gastonomo

import android.app.Application
import androidx.work.Configuration
import com.bahoque.gastonomo.sync.GastonomoWorkerFactory
import com.bahoque.gastonomo.sync.PendingUploadWorker

/**
 * Sostiene la raiz de composicion durante toda la vida del proceso.
 *
 * Tambien configura WorkManager a mano, para poder darle al worker lo que necesita del contenedor
 * en vez de que se lo busque por su cuenta.
 */
class GastonomoApplication : Application(), Configuration.Provider {

    /** Los tests la sustituyen por un contenedor con dobles antes de lanzar la actividad. */
    var container: AppContainer by lazyContainer()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            // El contenedor se pide de forma perezosa: construirlo aqui obligaria a abrir la base
            // de datos en el arranque del proceso, incluso cuando no hay nada que subir.
            .setWorkerFactory(GastonomoWorkerFactory { container.uploader })
            .build()

    override fun onCreate() {
        super.onCreate()
        // Al arrancar, por si quedo algo de la sesion anterior. Si no hay nada, el trabajo acaba
        // enseguida; si hay, espera a que haya red.
        PendingUploadWorker.schedule(this)
    }

    private fun lazyContainer() = object : kotlin.properties.ReadWriteProperty<Any?, AppContainer> {
        private var value: AppContainer? = null

        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): AppContainer =
            value ?: AppContainer(applicationContext).also { value = it }

        override fun setValue(
            thisRef: Any?,
            property: kotlin.reflect.KProperty<*>,
            value: AppContainer,
        ) {
            this.value = value
        }
    }
}
