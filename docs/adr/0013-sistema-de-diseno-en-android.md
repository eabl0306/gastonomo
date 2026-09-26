# ADR 0013: El rediseno se implementa con un sistema de diseno propio, no con Material 3

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

El rediseno llego como un prototipo HTML de una sola pagina (`Gastonomo.dc.html`) mas un
documento de entrega que declara al propio prototipo **fuente de verdad para los valores
exactos**. Son 14 pantallas con colores, radios, sombras, tamanos de letra y separaciones
concretas: un fondo `#f5f2ec`, tarjetas de radio 20, un verde `#2f8a55`, textos de 15/600,
pulsaciones que responden con `scale(.98)` y ni un solo borroso circular.

Material 3 no da eso. Da otra cosa, coherente consigo misma: su propia escala de color,
`MaterialTheme.colorScheme`, ripple en cada control, componentes con paddings decididos por
la biblioteca y una tipografia distinta. Se puede forzar, y el camino habitual es construir
un `ColorScheme` con los colores del diseno y sobrescribir los paddings de cada componente
uno por uno.

Ese camino se probo mentalmente y tiene dos problemas que no se arreglan con esfuerzo:

1. **La correspondencia es falsa.** `primary`, `secondary`, `surfaceVariant` y
   `onSurfaceVariant` no significan nada en este diseno. Hay `ink`, `muted`, `faint`, `lime`,
   `bgApp`, `bgNav`, `line`, `divider`, `chipBg`. Meter diez nombres en cuatro ranuras obliga
   a recordar que `surfaceVariant` es en realidad el fondo de un chip, y eso se olvida.
2. **Cada componente hay que desmontarlo.** Un `OutlinedTextField` de Material trae su label
   flotante, su altura minima y sus 16 dp de padding horizontal; el diseno quiere un label
   encima, 54 de alto y 16 de padding. Acabas peleando con la biblioteca en cada pantalla, y
   el resultado se parece al diseno en unas y no en otras.

Tambien se considero **no tener capa comun** y escribir cada pantalla con los valores a mano.
Es lo que pasa siempre: catorce pantallas escritas por turnos, con `Color(0xFF2F8A55)` en
cinco sitios, cuatro de ellos con un digito distinto, y ningun sitio donde cambiarlo.

## Decision

Un modulo `:core:designsystem` propio, encima de Compose Foundation, que **no usa
`MaterialTheme`** y del que salen todos los valores y todos los componentes de la aplicacion.

### Los tokens, por CompositionLocal

`GastonomoTheme` publica seis grupos y son los unicos numeros del proyecto:

| Grupo        | Que hay dentro                                               |
| ------------ | ------------------------------------------------------------ |
| `colors`     | La paleta del diseno con sus nombres, no con los de Material |
| `typography` | Los estilos con su tamano y peso exactos                     |
| `radii`      | Los radios, incluido `pill`                                  |
| `dimens`     | Separaciones, alturas y el minimo tactil                     |
| `shadows`    | Las dos sombras del diseno                                   |
| `formats`    | Dinero, tasas y fechas del idioma y la moneda vigentes       |

La regla que se deriva: **fuera de `:core:designsystem` no se escribe un color ni un tamano.**
Se lee `GastonomoTheme.colors.ink`. Una pantalla que necesite un valor nuevo lo anade al
token, no a su fichero.

Hay una excepcion y esta en el nucleo, no en la UI: los ocho colores que el usuario puede
elegir para una categoria (`Category.COLOR_CHOICES`) son **datos que viajan a la API**, no
tokens de la interfaz. La API los guarda como cadena y los devuelve con cada movimiento.

### Los textos, en `strings.xml`

Ninguna cadena visible se escribe en un composable. Las propias de una pantalla viven en el
`res/values` de su feature; las que comparten varias, en `:core:designsystem`, y se leen por
`GastonomoStrings.actionRetry` en vez de por `R.string.gastonomo_...`, para que una feature no
tenga que saber en que modulo esta el texto. Espanol en `values`, ingles en `values-en`, y las
dos con `tools:locale` declarado para que Android Lint corrija la ortografia del idioma
correcto.

El idioma **no** es el del telefono: se elige en Ajustes, y el tema lo aplica proveyendo
`LocalContext`, `LocalConfiguration` y `LocalResources` con la configuracion cambiada. Por eso
el bundle de la aplicacion no puede repartirse por idioma, y `app/build.gradle.kts` lo
desactiva: si se reparte, el telefono instala solo el idioma del sistema y elegir el otro no
cambia ni un texto.

### Los componentes, compartidos de verdad

Todo lo que sale en mas de una pantalla es un componente del sistema: `GastonomoTextField`,
`AmountInputField`, `ListRow`, `MovementRow`, `SegmentedControl`, `FilterPill`, `StatusBadge`,
`BottomSheet`, `CurrencySheet`, `PeriodPicker`, `TabBar`, los botones, el toast, los estados
de carga y de reintento. Una pantalla compone; no dibuja.

### La fidelidad se mide, no se recuerda

Es la parte que hace que esto no se degrade:

- **Capturas desde la JVM.** `:core:screenshot` renderiza cualquier composable a PNG con
  Robolectric y Roborazzi, a 390x844 xxhdpi, que es el tamano del prototipo. No hace falta
  emulador, asi que las capturas salen del mismo `./gradlew check` que los tests.
- **Se comparan contra el marcado del prototipo**, que es la fuente de verdad declarada, y
  midiendo pixeles, no a ojo. Asi se encontraron una tarjeta con mayuscula de mas, un
  segmento inactivo mas fino que el activo, un padding de 14 donde el diseno pone 16 y la
  linea superior que le faltaba a la tab bar.
- **Los limites de tamano empujan a partir.** Un fichero son 300 lineas, asi que una pantalla
  de catorce secciones no cabe en uno y se parte en composables pequenos por si misma.

### Lo que no se hizo

- **No hay modo oscuro.** El diseno entregado es uno solo. Un segundo esquema seria inventar
  valores que nadie ha decidido, y hacerlo mal es peor que no tenerlo.
- **Las capturas no se versionan como referencia.** Son 54 imagenes y 6 MB, y lo que Roborazzi
  compararia depende de las fuentes y de la version de Robolectric de la maquina, asi que la
  primera ejecucion en la CI daria diferencias que no son regresiones. Se suben como artefacto
  del workflow para poder mirarlas en un PR. Fijarlas como golden pide antes una imagen de
  contenedor con las fuentes clavadas, y eso es otra decision.

## Consecuencias

A favor:

- Los valores del diseno estan en un sitio y se cambian en un sitio.
- Los nombres de los tokens son los del diseno, asi que leer el codigo al lado del prototipo
  no obliga a traducir.
- No se pelea con los paddings de ningun componente ajeno.
- Una regresion visual se ve en una captura que sale de la CI, no cuando alguien abre la app.
- El idioma de la interfaz es una decision de la cuenta y no del telefono, que es lo que pide
  el diseno.

En contra, y hay que saberlo:

- **Hay que escribir los componentes.** Un `TextField`, un sheet y un segmented control son
  codigo propio con sus estados de foco y de pulsacion. Es la mayor parte de
  `:core:designsystem`.
- **No se hereda lo que Material trae hecho**, y accesibilidad incluida: los tamanos tactiles
  del diseno son los del diseno, y varios quedan por debajo de 44 dp (las pills de 36, los
  iconos y los colores del sheet de categoria). Esta apuntado y no se ha cambiado, porque
  cambiarlo es cambiar el diseno.
- **Compose sigue pidiendo `material3` para algunas piezas** (el dialogo de fecha, por
  ejemplo), asi que la dependencia esta en el classpath aunque `MaterialTheme` no se use.
- **Un diseno nuevo no llega gratis.** Si algun dia hay modo oscuro o una segunda marca, hay
  que duplicar la paleta a mano; Material lo habria generado.
