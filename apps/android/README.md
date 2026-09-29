# Gastonomo Android

App nativa en Kotlin con arquitectura hexagonal y multi-modulo. Implementa el rediseno
completo: 14 pantallas, multimoneda y sesion con refresco de tokens.

## Registrar rapido y dictar

Inicio, Movimientos y Prestamos comparten un solo boton flotante. Se puede **mantener y
deslizar** hacia la opcion, o **tocar y volver a tocar**; lo que distingue las dos formas es
cuanto se movio el dedo antes de soltar. El arco y la eleccion por angulo son aritmetica pura y
estan aparte del composable, en `RadialFabGeometry`, porque lo delicado no es como se ve: es que
el dedo elija lo que el usuario cree que esta eligiendo.

La tercera opcion es dictar. El reconocimiento de voz lo hace **el sistema**, que ya sabe el
idioma y no manda audio a ningun sitio; a la API solo viaja el texto, y el borrador que devuelve
se revisa antes de guardar. Guardar pasa por los mismos casos de uso que los formularios, con su
clave de idempotencia: un borrador no es un registro.

**Ningun fallo del microfono deja sin salida.** No haber reconocimiento, no dar el permiso, estar
sin conexion o no entenderse acaban en el modo escrito, con su explicacion y con lo que se hubiera
entendido ya puesto en el campo. Y "Editar" abre el formulario completo con lo que se entendio,
sustituyendo al dictado en la pila: volver atras desde el formulario lleva a la pantalla desde la
que se dicto, no a un borrador que ya no vale.

El dictado solo lo pueden usar las cuentas aprobadas: para el resto la API responde 404 y la hoja
lo dice. Como se aprueba una cuenta esta en el [README de la API](../api/README.md).

## Modulos

| Modulo                | Tipo                | Que contiene                                                          |
|-----------------------|---------------------|-----------------------------------------------------------------------|
| `:core:common`        | Kotlin JVM puro     | `Outcome` y `DispatcherProvider`                                      |
| `:core:domain`        | Kotlin JVM puro     | Dinero, movimientos, prestamos, categorias, periodo y los calculos derivados |
| `:core:application`   | Kotlin JVM puro     | Puertos y casos de uso                                                |
| `:core:designsystem`  | Android library     | Tokens, componentes, iconos y el formato de dinero y fechas           |
| `:core:navigation`    | Android library     | Las rutas de las 14 pantallas y las dos formas de navegar             |
| `:core:testing`       | Kotlin JVM puro     | Dobles de los puertos y datos de ejemplo (solo para tests)            |
| `:core:screenshot`    | Android library     | Captura de composables a PNG desde la JVM (solo para tests)           |
| `:data:network`       | Android library     | Cliente Ktor, refresco serializado, `problem+json` e idempotencia     |
| `:data:session`       | Android library     | Sesion cifrada, PIN, huella, red y reconocimiento de voz              |
| `:data:auth`          | Android library     | `AuthGateway`                                                         |
| `:data:finance`       | Android library     | `MovementGateway` y `LoanGateway`                                     |
| `:data:catalog`       | Android library     | `CategoryGateway`, `SettingsGateway` y `CurrencyGateway`              |
| `:data:database`      | Android library     | Base de datos local con Room: el esquema, los DAO y el mapeo a dominio |
| `:data:offline`       | Android library     | Los gateways que leen de la base local y dejan la red solo refrescando |
| `:feature:*`          | Android library     | Una pantalla o un grupo de pantallas, con su grafo de navegacion      |
| `:app`                | Android application | Raiz de composicion, `MainActivity` y el NavHost                      |

La direccion de las dependencias se fuerza con la estructura, no con convenciones: los tres
modulos del nucleo son `kotlin("jvm")`, asi que el SDK de Android ni siquiera esta en su
classpath. Importar `android.util.Log` desde `:core:domain` no compila. Ademas hay tests de
arquitectura con Konsist y la tarea `moduleGraphGuard`, que lee el grafo real de Gradle.

Ninguna `:feature:*` depende de `:data:*` ni de otra `:feature:*`. Las une `:app`, con un
fichero de cableado por feature en `app/src/main/kotlin/com/bahoque/gastonomo/wiring/`.

Los seis gateways van envueltos en el adaptador que lee de la base local, y eso tambien se ve
solo en `AppContainer`: los casos de uso reciben el mismo puerto de siempre. Por que la base es la
unica fuente de verdad, y que se degrada sin conexion, esta en el
[ADR 0019](../../docs/adr/0019-leer-de-la-base-local.md).

## Las dos reglas del sistema de diseno

Rompen el build igual que las demas, y son las que hacen que catorce pantallas se parezcan
entre si:

1. **Un color, un tamano o una duracion de animacion no se escriben a mano fuera de
   `:core:designsystem`.** Salen de `GastonomoTheme.colors`, `.typography`, `.radii`, `.dimens`,
   `.shadows`, `.motion` y `.formats`. Si
   falta un valor, se anade al token, no al fichero de la pantalla. La unica excepcion son los
   ocho colores que el usuario elige para una categoria: esos son datos que viajan a la API y
   viven en `:core:domain`.
2. **Un texto visible no se escribe en un composable.** Va al `strings.xml` de su feature, o al
   de `:core:designsystem` si lo comparten varias, y entonces se lee por `GastonomoStrings` en
   vez de por `R.string.gastonomo_...`. Espanol en `values`, ingles en `values-en`, y las
   mismas claves en las dos salvo las `translatable="false"`.

El idioma **no** es el del telefono: se elige en Ajustes y lo aplica `GastonomoTheme`, que
provee `LocalContext`, `LocalConfiguration` y `LocalResources` con la configuracion cambiada.
Por eso el bundle no se reparte por idioma (`bundle { language { enableSplit = false } }` en
`app/build.gradle.kts`): si se repartiera, el telefono instalaria solo el idioma del sistema y
elegir el otro no cambiaria ni un texto.

El porque de todo esto esta en el
[ADR 0013](../../docs/adr/0013-sistema-de-diseno-en-android.md), y el modelo de sesion del
cliente en el [ADR 0014](../../docs/adr/0014-sesion-en-el-cliente-android.md).

## Donde esta cada cosa

- **Tokens del diseno**: `GastonomoTheme.colors`, `.typography`, `.radii`, `.dimens`,
  `.shadows` y `.formats`, en `:core:designsystem`.
- **Formato de dinero y fechas**: `GastonomoFormats`, `AmountInput` y `DateFormats`.
- **Iconos**: `GastonomoIcon(name)` con los nombres de `GastonomoIcons`.
- **Regla de multimoneda**: `DisplayedAmount.of(value, display, rates)`.
- **Idempotencia**: `IdempotencyKey`, una clave por pulsacion de Guardar.
- **Rutas**: `GastonomoRoute`, y `GastonomoNavActions` para navegar.

## Contrato con la API

El contrato entero esta en [docs/api-surface.md](../../docs/api-surface.md). Lo que mas
condiciona al cliente:

- **Los importes viajan como cadena** y se leen a `BigDecimal`, nunca a `Double`.
- **Cada registro lleva cuatro campos de dinero**: `amount`+`currency` y `base`+`baseCurrency`.
  Al crear se omiten los dos ultimos juntos o ninguno; mandar uno solo es un 422.
- **Los tres POST que crean dinero llevan `Idempotency-Key`**, la misma en todos los
  reintentos de un intento.
- **`POST /v1/auth/refresh` rota**: presentar un token ya rotado revoca la familia entera,
  asi que los refrescos van serializados en `TokenRefresher`.
- `API_BASE_URL` sale de `BuildConfig` de `:data:network`: `http://10.0.2.2:8080` en debug
  (el host de la maquina visto desde el emulador) y `https://api.gastonomo.bahoque.com` en
  release. Se define por buildType en `data/network/build.gradle.kts`, nunca con un `if` en
  el codigo. El texto claro solo esta permitido en debug y solo para los hosts locales: cada
  buildType tiene su `app/src/<tipo>/res/xml/network_security_config.xml`. El detalle del
  despliegue esta en [docs/despliegue.md](../../docs/despliegue.md).

## Capturas sin emulador

`:core:screenshot` renderiza composables a PNG desde la JVM con Robolectric y Roborazzi.
Las imagenes quedan en `<modulo>/build/outputs/roborazzi/`, que ya esta en `.gitignore`.

```kotlin
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS)   // 390x844, el tamano del prototipo
class HomeScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `home screen`() {
        compose.captureGastonomoScreen("home") { HomeScreen(state = sample) }
    }
}
```

Las cuatro pantallas que llevan tab bar se capturan con `captureGastonomoTabScreen`, que las
mete en el armazon de verdad, el mismo `GastonomoShell` que monta la aplicacion:

```kotlin
compose.captureGastonomoTabScreen("home", TopLevelDestination.HOME) { HomeScreen(state = sample) }
```

Sin eso la pantalla se lleva los 844 dp enteros y la captura ensena 78 dp de contenido que en el
telefono tapa la barra, ademas de sacar lo que flota abajo donde no va. Asi se escondio que el
boton de registro rapido estaba a 92 dp de la barra en vez de a 20. Las otras diez pantallas no
llevan barra y siguen con `captureGastonomoScreen`; una hoja suelta, sin su pantalla detras,
tambien, porque con el armazon se veria la barra debajo de un hueco vacio.

El `robolectric.properties` del modulo tiene que llevar `graphicsMode=NATIVE`, o la captura
sale en blanco.

Son 71 imagenes y unos 8 MB, y **no estan versionadas**: lo que Roborazzi compararia depende de
las fuentes y de la version de Robolectric de la maquina, asi que una referencia fijada aqui
daria diferencias en la CI que no son regresiones. La CI las sube como artefacto
(`android-screenshots`) para poder mirarlas desde un PR. Fijarlas como golden pide antes una
imagen de contenedor con las fuentes clavadas.

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
| Cobertura: nucleo 80%, feature 70%, proyecto 60% | Kover, `koverVerify`                 |
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
- `LongParameterList` ignora los `@Composable`: un componente lleva por contrato su
  `modifier`, sus slots y los estados que pinta. El limite de 5 sigue vigente fuera de la UI.
- `KeystoreCipher` y `AndroidBiometricLock` no cuentan para la cobertura: hablan con
  hardware que Robolectric no tiene. Se comprueban con `just android-connected`.

## Lo que no esta verificado

Se dice aqui porque no se ve en el arbol de verde:

- **No hay emulador ni dispositivo en la maquina de desarrollo**, asi que
  `connectedAndroidTest` no se ha ejecutado nunca. De `androidTest` solo se comprueba que
  compila, con `assembleDebugAndroidTest`, y hoy no hay ninguna clase ahi: la unica que habia
  era la de la pantalla de salud y se borro con ella.
- **`KeystoreCipher` y `AndroidBiometricLock` no se han ejecutado.** Robolectric no trae
  proveedor `AndroidKeyStore` ni sensor de huella. Estan excluidos del calculo de cobertura de
  `:data:session` y hay que comprobarlos con `just android-connected`.
- **El dictado por voz no se ha probado con voz.** El emulador no tiene microfono: lo que se ha
  ejercitado de punta a punta es el modo escrito, contra una API local, incluida la interpretacion
  y el guardado. Del reconocimiento en si solo se prueba la traduccion de sus codigos de error, en
  `SpeechFailuresTest`; `AndroidSpeechToText` esta excluido de la cobertura por la misma razon que
  la huella.
- **Nadie ha visto una respuesta real de la API desde la app.** Los DTO se revisaron contra
  [docs/api-surface.md](../../docs/api-surface.md) y hay tests contra un mock de Ktor, pero el
  contrato no se ha ejercitado contra el servidor.
- **Las capturas usan datos de ejemplo pequenos** (tres o cuatro categorias, cuatro
  movimientos), asi que no cubren listas largas ni desbordes de texto.
- **Los estados de interaccion no salen en una captura estatica**: el foco, el `scale(.98)` de
  la pulsacion y el 0.4 de un control deshabilitado estan en el codigo y no se han visto
  pintados.
- **Faltan dos pantallas que el handoff deja fuera**: Cambiar contrasena y la tarjeta de perfil
  de Ajustes. Los casos de uso `changePassword` y `loadProfile` ya existen y estan inyectados.
- **Los tamanos tactiles del diseno son los del diseno.** Las pills de 36 dp y los iconos y
  colores del sheet de categoria quedan por debajo del minimo de 44 dp. Cambiarlo es cambiar el
  diseno, asi que esta apuntado y no tocado.
