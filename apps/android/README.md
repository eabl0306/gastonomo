# Gastonomo Android

App nativa en Kotlin con arquitectura hexagonal y multi-modulo. El primer corte implementa
un solo caso de uso de punta a punta: comprobar la salud de la API.

## Modulos

| Modulo             | Tipo                | Que contiene                                                  |
|--------------------|---------------------|---------------------------------------------------------------|
| `:core:domain`     | Kotlin JVM puro     | `HealthReport`, `ComponentHealth`, `HealthStatus`, `ServiceVersion` |
| `:core:application`| Kotlin JVM puro     | Puerto `HealthGateway` y caso de uso `CheckApiHealth`          |
| `:core:common`     | Kotlin JVM puro     | `Outcome` y `DispatcherProvider`                               |
| `:data:health`     | Android library     | Adaptador driven sobre Ktor client, DTOs y mapeadores          |
| `:feature:health`  | Android library     | `HealthViewModel`, estado de UI y pantalla Compose             |
| `:app`             | Android application | Raiz de composicion, `MainActivity` y el tema                  |

La direccion de las dependencias se fuerza con la estructura, no con convenciones: los dos
modulos del nucleo son `kotlin("jvm")`, asi que el SDK de Android ni siquiera esta en su
classpath. Importar `android.util.Log` desde `:core:domain` no compila. Ademas hay tests de
arquitectura con Konsist que comprueban las reglas que el classpath no puede comprobar solo.

`:feature:health` no depende de `:data:health`. Los une `AppContainer`, en `:app`.

## Contrato con la API

- `GET /health/ready` en `${API_BASE_URL}`; 200 si esta sano, 503 si esta degradado, ambos con informe.
- `API_BASE_URL` sale de `BuildConfig` de `:data:health`: `http://10.0.2.2:8080` en debug
  (el host de la maquina visto desde el emulador) y un placeholder en release.
- Un componente en error degrada el informe. Esa regla vive en `HealthReport`, no en la API.

## Calidad

Todo lo siguiente rompe el build, no es documentacion:

| Regla                                   | Herramienta                                   |
|-----------------------------------------|-----------------------------------------------|
| Fichero <= 300 lineas (tests 400)       | tarea `sourceGuard`                           |
| Tests saltados sin motivo escrito       | tarea `sourceGuard`                           |
| `@Suppress` sin motivo escrito          | tarea `sourceGuard`                           |
| Complejidad ciclomatica <= 10           | detekt `CyclomaticComplexMethod`              |
| Complejidad cognitiva <= 15             | detekt `CognitiveComplexMethod`               |
| Funcion <= 50 lineas                    | detekt `LongMethod`                           |
| Parametros <= 5                         | detekt `LongParameterList`                    |
| Anidamiento <= 4                        | detekt `NestedBlockDepth`                     |
| Nada de codigo muerto ni imports sueltos| detekt `UnusedPrivate*`, `UnusedImports`      |
| TODO o FIXME sin ticket                 | detekt `ForbiddenComment`                     |
| Formato                                 | detekt-formatting (reglas de ktlint)          |
| Avisos de Android Lint                  | `warningsAsErrors = true`, `abortOnError = true` |
| Cobertura: nucleo 80%, proyecto 60%     | Kover, `koverVerify`                          |
| Direccion de dependencias (imports)     | Konsist, en los tests de `:app`               |
| Direccion de dependencias (grafo Gradle)| tarea `moduleGraphGuard`                      |

Se usa detekt-formatting en lugar de ktlint o spotless por separado: son las mismas reglas de
ktlint, pero con una sola herramienta, un solo fichero de configuracion y un solo informe.

## Comandos

```bash
export JAVA_HOME=/snap/android-studio/244/jbr
export PATH=$JAVA_HOME/bin:$PATH

./gradlew check           # detekt + lint + tests + sourceGuard + moduleGraphGuard + koverVerify
./gradlew detekt          # informes en <modulo>/build/reports/detekt/
./gradlew koverHtmlReport # cobertura agregada en build/reports/kover/html/
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest  # compila los tests instrumentados, sin ejecutarlos
./gradlew connectedAndroidTest   # necesita emulador o dispositivo
```

`check` no compila las fuentes de `androidTest`, asi que un test instrumentado roto
pasaria desapercibido: detekt mira estilo, no tipos. Por eso `just android-check` y la
CI encadenan tambien `assembleDebugAndroidTest`. Ejecutarlos de verdad necesita un
emulador y eso se hace a mano con `just android-connected`.

## Notas del entorno

- El JDK 25 de Android Studio rompe el compilador embebido de detekt 1.23.8 si se le pasa
  `--jdk-home`. El convention plugin de calidad lo deja sin asignar; detekt no usa resolucion
  de tipos, asi que no pierde nada.
- Robolectric corre con `sdk=34` porque todavia no hay imagen para la API 37.
