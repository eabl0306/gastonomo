plugins {
    id("gastonomo.android.library")
}

android {
    namespace = "com.bahoque.gastonomo.data.session"
}

quality {
    lineCoverageMinimum.set(60)
    // Estas dos clases hablan con hardware que no existe en la JVM: Robolectric no trae el
    // proveedor AndroidKeyStore ni un sensor de huella, asi que su codigo no se puede
    // ejecutar sin un dispositivo. Se comprueban con `just android-connected` cuando haya
    // emulador; mientras tanto no cuentan para la cobertura.
    //
    // La exclusion se lee facil como "de la huella no se puede probar nada", y eso costo un
    // cierre en produccion: construir el dialogo no necesita sensor, solo se estaba haciendo
    // dentro de la clase excluida. Ahora esa parte esta en UnlockPrompt.kt, que NO esta aqui
    // y tiene su test. Lo que queda excluido es unicamente ensenar el dialogo y esperar.
    untestableOnJvm.addAll(
        "com.bahoque.gastonomo.data.session.KeystoreCipher",
        "com.bahoque.gastonomo.data.session.AndroidBiometricLock",
        "com.bahoque.gastonomo.data.session.AndroidBiometricLock*",
        // El reconocimiento de voz es un servicio del sistema que Robolectric no trae: sin el,
        // isRecognitionAvailable contesta que no y no hay forma de ejecutar el resto. Lo que si
        // se puede probar sin dispositivo, la traduccion de los codigos de error, esta en
        // SpeechFailures.kt, que NO esta aqui y tiene su test.
        "com.bahoque.gastonomo.data.session.AndroidSpeechToText",
        "com.bahoque.gastonomo.data.session.AndroidSpeechSession",
        "com.bahoque.gastonomo.data.session.AndroidSpeechSession*",
        "com.bahoque.gastonomo.data.session.SilentRecognitionListener",
    )
}

dependencies {
    api(project(":core:application"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    // El dialogo de huella necesita una FragmentActivity, y la aplicacion tiene que poder
    // nombrar ese tipo para registrar la actividad viva: va en api, no en implementation.
    api(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
}
