# ADR 0017: Movimiento: como se animan los cambios de pantalla y los overlays

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

El `NavHost` de la aplicacion se escribio sin animaciones, asi que Compose usaba el corte seco
por defecto: una pantalla desaparece y otra aparece, sin nada en medio.

El efecto que eso tiene se nota mas de lo que parece. Cada pantalla tarda un poco en traer sus
datos, y ese retardo es pequeno; lo que lo hace notorio es que **no hay nada que ocupe el hueco**.
Un corte seco seguido de un indicador de carga se lee como lentitud, aunque la lentitud sea de
milisegundos.

El handoff de diseno no trae animaciones: es HTML estatico. Pero si trae una pista que hay que
respetar, y es la unica que hay: los formularios de creacion (Nuevo movimiento, Nuevo prestamo)
llevan `close` en la barra, y todas las demas pantallas secundarias llevan `arrow_back`. Cambiar
el icono es decir que ese movimiento es otro.

## Decision

### Las duraciones y las curvas son tokens del sistema de diseno

Viven en `:core:designsystem`, en `GastonomoMotion`, y se leen por `GastonomoTheme.motion`. La
regla del sistema de diseno pasa a ser que **un color, un tamano o una duracion no se escriben a
mano** fuera de ese modulo. Las animaciones de componentes (bottom sheets, modales, el indicador
de los tabs) salen de los mismos tokens, asi que todo lo que se mueve en la aplicacion se mueve
con los mismos tiempos.

Los valores:

| Token             | ms  | Para que                                                   |
| ----------------- | --- | ---------------------------------------------------------- |
| `enterMillis`     | 240 | Lo que entra: una pantalla nueva, un bottom sheet que sube |
| `exitMillis`      | 180 | Lo que sale                                                |
| `selectionMillis` | 170 | Lo que se mueve dentro de un control sin entrar ni salir   |

Lo que sale va mas rapido que lo que entra, porque lo que se va no merece atencion. Y ninguna pasa
de 300 ms: por encima de eso la aplicacion empieza a parecer lenta, que es exactamente lo que se
estaba intentando arreglar. Hay un test que lo comprueba, porque es un limite que se sube sin
darse cuenta.

Las curvas son las de Material 3 escritas por valor (`CubicBezierEasing`): emphasized decelerate
para lo que entra, emphasized accelerate para lo que sale y standard para lo que se mueve dentro
de la pantalla. No se usa `MotionScheme` de Material 3 porque sigue siendo experimental y ataria
el sistema de diseno a una API que puede cambiar de firma; los numeros de la especificacion, en
cambio, no se mueven.

### Tres movimientos, y cada uno significa una cosa

La transicion depende de **las dos** pantallas, no de una, asi que se decide en un solo sitio
(`GastonomoTransitions`, en `:app`) en vez de `composable` por `composable` en los siete grafos de
feature. Repartirla garantizaba que un dia cambiar de tab se animara igual que entrar en un
detalle.

- **Apilar y volver: desplazamiento horizontal.** La que entra viene de la derecha, la que se
  queda detras se va un tercio hacia la izquierda con un poco de fundido. Al volver, al reves.
  Comunica "he entrado en algo" y "he salido de algo".
- **Cambiar de tab: fundido, sin desplazamiento.** Las cuatro principales son hermanas y no hay
  jerarquia entre ellas. Esto no es estetica: si cambiar de tab se viera igual que entrar en un
  detalle, el gesto de volver dejaria de ser predecible, porque el usuario habria visto la misma
  animacion en un caso donde volver no existe.
- **Formularios de creacion: de abajo arriba.** Es la pista del icono `close`. La pantalla que
  tapan no se mueve ni se desvanece: se queda quieta debajo, como debajo de un modal. Al cerrar, el
  formulario baja y la de debajo reaparece.

El desplazamiento de la que se queda detras es un tercio del de la que entra. Que las dos
recorran lo mismo hace que parezca que la pila entera se desliza de lado, que es otro efecto
distinto.

### La clasificacion sale de la ruta, no de una lista aparte

`screenKindOf` mira el nombre cualificado de la clase de la ruta, igual que ya hacia
`selectedTabIndex`. Los tabs salen de `TopLevelDestination.entries`, asi que anadir una quinta
pestana no obliga a acordarse de este fichero. Los formularios si son una lista explicita de dos
rutas, porque no hay nada en el tipo que diga que son formularios; si aparece un tercero hay que
anadirlo, y el test que comprueba cual es cual esta al lado.

### Apagar las animaciones las apaga de verdad

Se lee `Settings.Global.TRANSITION_ANIMATION_SCALE`, que es lo que pone a cero tanto "Quitar
animaciones" de accesibilidad como la escala de las opciones de desarrollador. Con las animaciones
apagadas el `NavHost` usa `EnterTransition.None` y `ExitTransition.None`: corte seco, que es lo
que el usuario pidio.

La preferencia se lee **una vez, en el tema**, y deja todas las duraciones de `GastonomoMotion` en
cero. Asi un composable nuevo respeta la preferencia por el solo hecho de leer el token, sin que
quien lo escriba tenga que acordarse de comprobar nada. Es la misma idea que el resto del
enforcement de este repositorio: que cumplir sea lo facil.

## Consecuencias

A favor:

- El hueco de la carga deja de estar vacio, y el retardo que ya era pequeno pasa a ser tolerable.
- Los tres movimientos son distinguibles, asi que la animacion informa en vez de solo decorar.
- Anadir una pantalla no obliga a decidir su animacion: sale de su papel en la navegacion.
- La preferencia de accesibilidad se respeta en toda la aplicacion, no solo donde alguien se
  acordo.

En contra, y hay que saberlo:

- **La animacion no arregla la lentitud, la disimula.** La causa es que cada pantalla vuelve a
  pedir datos. Eso lo ataca el [ADR 0016](0016-cache-en-memoria-en-el-cliente.md) y, de fondo, el
  trabajo sin conexion. Con la animacion sola, el deslizamiento acaba en un indicador de carga.
- **No se puede asertar que una animacion se ve bien.** Lo que si esta probado es la decision
  (que movimiento toca entre que pantallas), que las duraciones no se pasan del limite, que
  apagar las animaciones las pone a cero y que con las cuatro transiciones cableadas la
  navegacion sigue llegando a su destino ida y vuelta. Lo que se ve hay que mirarlo en un
  dispositivo, y en esta maquina no hay emulador.
- **La preferencia del sistema se lee al componer el tema.** Cambiarla con la aplicacion abierta
  no se nota hasta que el tema se vuelve a componer. Es el comportamiento habitual y no merece un
  observador.
- **`GastonomoTransitions` vive en `:app` y no en `:core:navigation`.** Necesita los tokens de
  movimiento, y las rutas no pueden depender del sistema de diseno (ver el KDoc de
  `TopLevelDestination`). La alternativa era meter tipos de navegacion en el sistema de diseno,
  que es peor. La consecuencia es que un grafo de feature montado solo, como en los tests de
  captura, no lleva animaciones, lo cual ademas conviene: las capturas de Roborazzi siguen siendo
  deterministas.
- **Las capturas de Roborazzi no cubren esto.** Son estaticas y se toman sobre pantallas, no sobre
  el `NavHost`, asi que una animacion que dejara un estado intermedio pintado no saldria en ellas.

## Los overlays: bottom sheets y segmented control

Los mismos tokens, dentro de la pantalla.

### Un sheet no puede animar su salida si quien lo usa lo quita con un `if`

Este era el problema de verdad, y no es de animacion sino de composicion: las siete pantallas que
abren un sheet lo hacian con `state.x?.let { Sheet(...) }`. En cuanto el estado se pone a null la
hoja ya no esta compuesta, asi que no hay nada que animar.

Se resuelve con `GastonomoSheetHost`, que envuelve el sheet, se queda compuesto el tiempo que dura
la salida y **retiene el ultimo valor no nulo** mientras baja. Sin la retencion lo que bajaria
seria una hoja vacia: el sheet de abono, por ejemplo, pierde el prestamo al cerrarse, y con el el
nombre y el saldo que pinta.

El velo y el panel se animan a la vez, y esa es la parte que se nota si se hace mal: el velo funde
con el `AnimatedVisibility` del host y el panel sube con su propio `animateEnterExit`. Si el velo
apareciera de golpe se veria un parpadeo oscuro antes de que suba la hoja.

El ambito de animacion llega al panel por un CompositionLocal y no por parametro, porque entre el
host y `GastonomoBottomSheet` hay siempre un componente de por medio (`CurrencySheet`,
`DateRangeSheet`, el sheet de categoria...) y pasarlo a mano obligaria a anadir el mismo parametro
a los seis. Cuando no hay host el local es null y la hoja se pinta sin animacion, que es justo lo
que hacen las capturas.

La primera composicion no anima: es lo que hace `AnimatedVisibility` con un `visible` que ya llega
a true. Por eso una captura con la hoja abierta sigue saliendo con la hoja entera.

### Arrastrar hacia abajo cierra

Es lo que el handle de 40x4 del diseno promete y no cumplia. Cierra por recorrido (120 dp) **o**
por velocidad (900 px/s): exigir solo recorrido hace que un lanzamiento corto y rapido no cierre, y
entonces parece que el gesto no existe. Tirar hacia arriba no hace nada, la hoja no se estira.

### El fondo del segmento activo se desliza

Antes cada celda pintaba su propio fondo y se encendia y se apagaba. Ahora hay **una sola capa
detras de las celdas** que se desplaza hasta la elegida, y el color del texto funde. Ademas de
verse como un movimiento y no como un salto, quita el problema de que el fondo apareciera en el
destino antes de desaparecer del origen.

Para animar su posicion hay que medir: los segmentos se reparten el ancho con `weight`, asi que su
tamano no se conoce hasta que el contenedor esta medido. De ahi el `BoxWithConstraints`.

### Lo que esto cuesta

- **La celda ya no escala junto con su fondo al pulsarla.** El `pressScale` sigue en la celda, pero
  el fondo es ahora una capa aparte que no se entera. Se nota poco, y arreglarlo obligaria a que la
  capa supiera cual es la celda pulsada.
- **El fondo y las celdas se reparten el ancho por caminos distintos**, `maxWidth / n` frente a
  `weight`. Con un ancho que no sea divisible pueden quedar a un pixel. A los anchos reales de un
  telefono no se aprecia, pero la diferencia existe.
- **Que la hoja se vea bien bajando no esta probado, solo que baja.** Lo que hay son tests de que
  sigue compuesta mientras se va, de que acaba desapareciendo, de que el contenido retenido
  sobrevive al valor y de que arrastrar cierra pero un arrastre corto no.
