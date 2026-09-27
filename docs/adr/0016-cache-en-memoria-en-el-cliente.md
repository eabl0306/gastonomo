# ADR 0016: Cache en memoria en el cliente Android, y por que es provisional

- Estado: **reemplazado** por el [ADR 0019](0019-leer-de-la-base-local.md) el 2026-09-27
- Fecha: 2026-09-27

> Este ADR se cumplio y se retiro el mismo dia. La cache en memoria era provisional a
> proposito y el modulo `:data:cache` ya no existe: la base local de Room es ahora la unica
> fuente de verdad. Se conserva porque explica por que se hizo, que decisiones se tomaron y
> cuales de ellas sobrevivieron al cambio.

## Contexto

Las cuatro pantallas de nivel superior de la aplicacion (Inicio, Movimientos, Prestamos y
Ajustes) piden sus datos a la API cada vez que se montan, y se montan cada vez que se entra en
ellas. El motivo es la navegacion: cada pantalla crea su ViewModel dentro de su
`composable<...>`, asi que el ViewModel esta atado al `NavBackStackEntry`. Al cambiar de
pestana la entrada se destruye y el ViewModel con ella; al volver se construye otro y su bloque
`init` vuelve a llamar a la API.

El efecto que ve el usuario es el que motivo el issue 10: entras en Movimientos y tarda; pasas a
Prestamos y tarda; vuelves a Movimientos y **vuelve a tardar lo mismo**, aunque esos datos se
trajeron hace cinco segundos.

Inicio es el caso peor: una sola carga son **cinco peticiones en paralelo** (ajustes, tasas, el
periodo completo, los cuatro ultimos y el perfil) mas los prestamos. Ir a Prestamos y volver son
once peticiones para pintar exactamente lo mismo.

No habia ninguna capa de cache en el proyecto: ni base de datos local, ni almacen de datos, ni
repositorio con memoria. Cada `Gateway` de `:data:*` iba directo a Ktor.

La solucion de fondo es el trabajo sin conexion: una base de datos local como unica fuente de
verdad, con la red solo refrescando. Eso es la epica del issue 13 y son varios dias de trabajo.
Esta decision es sobre lo que se hace **mientras** eso no existe.

## Decision

Una cache **en memoria, viva mientras viva el proceso**, entre los casos de uso y los
adaptadores de red. No es persistencia: al cerrar la aplicacion no queda nada. Lo unico que
hace es no repetir una peticion que se acaba de hacer.

### Decora puertos, no adaptadores

Vive en un modulo nuevo, `:data:cache`, que es Kotlin JVM puro y **solo depende de
`:core:application`**. No conoce Ktor, ni el SDK de Android, ni los DTO. Cada decorador
implementa el mismo puerto que decora y recibe el real por constructor, y la unica que los une
es la raiz de composicion:

```kotlin
movements = cachedMovementGateway(createMovementGateway(apiClient), caches)
```

Dos consecuencias que importan. La primera es que **ninguna feature ni ningun caso de uso se
entera**: reciben el mismo puerto de antes. La segunda es que retirar la cache el dia que exista
la base de datos local es borrar seis llamadas de un fichero.

### Dos plazos, no uno

- Antes de **45 segundos** la entrada se sirve tal cual y no se toca la red.
- Entre 45 segundos y **5 minutos** se sirve igual, al instante, y se lanza una revalidacion en
  segundo plano que deja la lectura siguiente ya actualizada.
- Pasados 5 minutos la entrada ya no se sirve: se espera a la red.

El tercer plazo es el que evita el fallo mas feo de una cache: volver a la aplicacion despues de
tres horas con el proceso todavia vivo y ver los saldos de hace tres horas como si fueran de
ahora. Un indicador de carga despues de una ausencia larga es correcto; un balance viejo sin
aviso, no.

El catalogo de monedas y las tasas tienen plazos propios, de 30 minutos y 6 horas: la API los
refresca a las 06:00 y a las 18:00 UTC (ADR 0010), asi que pedirlos mas a menudo no cambia ni un
numero.

### La invalidacion es explicita y cruza recursos

Cada escritura tira lo que dejo desfasado, y por eso las caches estan todas en un mismo objeto
(`GatewayCaches`) en vez de una privada por decorador:

| Escritura                          | Tira                               |
| ---------------------------------- | ---------------------------------- |
| `POST /v1/movements`               | movimientos, prestamos, categorias |
| `POST /v1/loans`                   | movimientos, prestamos, categorias |
| `POST /v1/loans/{id}/payments`     | movimientos, prestamos, categorias |
| `POST PATCH DELETE /v1/categories` | categorias, movimientos            |
| `PATCH /v1/settings`               | ajustes, y **nada mas**            |
| `register` `login` `logout`        | todo                               |

Tres filas merecen explicacion:

- **Las categorias entran en toda escritura de dinero** porque `GET /v1/categories` devuelve
  `movementCount`: un movimiento nuevo deja la lista de categorias desfasada aunque ninguna
  categoria haya cambiado.
- **Borrar una categoria tira los movimientos** porque la API reasigna los suyos a Otros. Una
  lista ya cargada pintaria el tile de una categoria que ya no existe.
- **Cambiar la moneda principal no tira los registros.** Los importes guardados son los cuatro
  campos que devolvio la API y la conversion la hace el cliente al pintar (reglas 2 y 3 de
  `CLAUDE.md`), asi que lo que hay en memoria sigue siendo valido: cambia como se muestra, no lo
  que se guardo. Este es el caso que obliga a **no cachear importes convertidos**.

Lo que invalida al cambiar de cuenta esta en el decorador de `AuthGateway` y no en un caso de
uso, porque entrar, registrarse y salir pasan los tres por ese puerto: asi no hay forma de
anadir un camino nuevo de sesion que se olvide de limpiar. Y limpia **antes** de la llamada, no
despues, para que ni un intento fallido deje nada de la cuenta anterior en memoria.

### Cuatro reglas de la propia cache

- **Un fallo no se guarda.** Asi el boton de reintentar siempre sale a la red.
- **Un fallo tampoco borra lo que habia.** Un corte de red no puede dejar la cache peor de como
  estaba: la entrada se queda con su edad y la lectura siguiente lo reintenta.
- **La peticion corre en el contexto de quien lee.** Asi `collectLatest` sigue cancelando la
  carga del mes anterior cuando el usuario pulsa la flecha del selector dos veces seguidas. La
  unica excepcion es la revalidacion, que es a proposito independiente de quien la disparo.
- **Una respuesta que llega despues de una invalidacion se tira.** Hay un contador de version
  que sube en cada invalidacion; si no coincide al guardar, la respuesta se descarta. Sin esto,
  una revalidacion lanzada justo antes de registrar un movimiento volveria a guardar la lista de
  antes, y la pantalla tardaria un plazo entero en ensenar lo que el usuario acaba de escribir.

### Recargar al volver a primer plano

La cache sola no basta, y esto salio al implementarla. Los formularios guardan y **vuelven
atras**, y la entrada de la pila de la pantalla de destino nunca se destruyo: su ViewModel sigue
vivo con la lista que cargo antes y nada le dice que ya no esta completa. El sintoma es que
registrar un gasto ensena el toast "Gasto guardado" y la lista de abajo no cambia.

Prestamos ya lo resolvia con una senal propia (`LoanCreatedSignal`). Inicio y Movimientos no lo
resolvian de ninguna manera.

Se arregla recargando cuando la pantalla vuelve a primer plano
(`LifecycleEventEffect(ON_RESUME)`). Es barato precisamente por la cache: si nada se escribio,
la recarga se sirve de memoria y no sale ni una peticion; si se escribio, la escritura ya
invalido lo que toca y la recarga trae lo nuevo. La primera vez no hace nada, porque el bloque
de inicializacion ya esta cargando y dos cargas a la vez en arranque frio son dos peticiones de
todo.

### Lo que se descarto

- **Coalescer peticiones en vuelo.** Dos lecturas de la misma clave a la vez podrian compartir
  una sola peticion, pero para eso la peticion tiene que correr en un ambito propio, y entonces
  cancelar al lector deja de cancelar la peticion. Se pierde la cancelacion de `collectLatest`,
  que hoy funciona, a cambio de ahorrar una peticion en una ventana de milisegundos. No vale la
  pena. Las revalidaciones si estan protegidas: dos lecturas rancias de la misma clave lanzan
  una sola, y el reclamo se hace dentro del mismo cerrojo que la lectura.
- **Subir los ViewModel de las pestanas por encima de su entrada de la pila.** Arreglaria
  tambien el estado de la interfaz (la posicion del scroll, la pill elegida), no solo los datos.
  Pero obliga a que el grafo de navegacion conozca el `ViewModelStoreOwner` de cada feature, y
  el mismo fichero lo van a tocar las transiciones del issue 11. Va con ese trabajo, no con
  este.
- **Cachear en disco.** Es la base de datos local del issue 14. Hacerla a medias aqui seria
  escribir dos veces la misma cosa.

## Consecuencias

A favor:

- Volver a una pantalla ya visitada no pide nada a la red dentro de los 45 segundos. El caso de
  Inicio pasa de once peticiones a cero.
- La cache es invisible para el resto del proyecto, y quitarla es un cambio de un fichero.
- Se puede probar entera sin Robolectric y sin red: el modulo es JVM puro, el tiempo es un
  puerto (`CacheTicker`) y los tests cuentan llamadas a espias.
- Registrar un movimiento ya se ve en Inicio y en Movimientos al volver, que antes no pasaba.

En contra, y hay que saberlo:

- **La vista no se actualiza sola cuando la revalidacion trae algo distinto.** Los puertos son
  funciones `suspend` que devuelven un valor, no flujos, asi que el segundo valor no tiene por
  donde llegar a la pantalla que ya pinto: llega a la lectura siguiente. Aqui apenas importa
  porque el unico que escribe es el propio usuario en su dispositivo y esas escrituras invalidan
  a mano, pero deja de ser cierto en cuanto haya dos dispositivos. Lo arregla el issue 15, donde
  los repositorios leen de local y exponen flujos.
- **Sigue habiendo un indicador de carga al volver a una pestana**, de uno o dos fotogramas: lo
  que muere con la entrada de la pila es el ViewModel, y con el el estado, no solo los datos. La
  cache quita la espera de red, no la reconstruccion del estado. Eso lo arregla subir el
  ViewModel, que va con el issue 11.
- **La cache no tiene tope de memoria por tamano, solo por numero de entradas** (32 para las
  listas). La clave de los movimientos lleva el periodo, asi que quien recorra un ano mes a mes
  llega a doce entradas de hasta 500 registros. Es el orden de magnitud correcto para una cuenta
  personal, pero es un numero elegido a ojo, no medido.
- **Recargar al volver a primer plano recarga tambien cuando no hacia falta.** Dentro del plazo
  no cuesta red, pero si cuesta una vuelta de estado y de recomposicion. Es el precio de no
  tener una senal de "se escribio dinero" en el nucleo, que es lo que traera el issue 16 de la
  cola de escrituras pendientes.
- **Es codigo que nace para morir.** Esta marcado como provisional en su KDoc y en este ADR, y
  el riesgo real es que sobreviva a la base de datos local y queden dos caches. Cuando se cierre
  el issue 15 hay que borrar `:data:cache`.
