package com.bahoque.gastonomo

import android.app.Application

/** Sostiene la raiz de composicion durante toda la vida del proceso. */
class GastonomoApplication : Application() {

    /** Los tests la sustituyen por un contenedor con dobles antes de lanzar la actividad. */
    var container: AppContainer by lazyContainer()

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
