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
    // emulador; mientras tanto no cuentan para la cobertura, y lo que si se puede probar
    // (que el valor guardado no es el texto en claro, y que sin actividad la huella se
    // responde como cancelada) esta cubierto.
    untestableOnJvm.addAll(
        "com.bahoque.gastonomo.data.session.KeystoreCipher",
        "com.bahoque.gastonomo.data.session.AndroidBiometricLock",
        "com.bahoque.gastonomo.data.session.AndroidBiometricLock*",
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
