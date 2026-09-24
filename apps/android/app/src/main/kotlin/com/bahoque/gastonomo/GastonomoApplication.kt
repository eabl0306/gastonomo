package com.bahoque.gastonomo

import android.app.Application

class GastonomoApplication : Application() {

    /** Los tests sustituyen el contenedor antes de lanzar la actividad. */
    var container: AppContainer = AppContainer()
}
