# ADR 0019: La base local es la fuente de verdad y la red solo la rellena

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

Ajustes tiene una fila que dice "Sincronizacion en la nube: al dia". Esa frase promete que los
datos viven en el dispositivo y se sincronizan, no que se piden a un servidor cada vez. Hasta
ahora la aplicacion hacia lo contrario: cada pantalla llamaba a la API y sin conexion no funcionaba
nada. La fila mentia.

No es un detalle de rendimiento. Una aplicacion de finanzas personales se abre en un supermercado,
en un sotano, en el metro; justo donde no hay cobertura.

El [ADR 0016](0016-cache-en-memoria-en-el-cliente.md) puso una cache en memoria como parche de
percepcion, y dijo que cuando existiera la base local habria que retirarla. El
[ADR 0018](0018-datos-locales-en-reposo.md) monto esa base. Esto es el paso que las une.

## Decision

### Las pantallas leen de la base local, siempre

Un modulo nuevo, `:data:offline`, envuelve los seis adaptadores de red. Cada uno hace lo mismo:

1. Lee de la base local.
2. Si hay algo, lo devuelve **ya**, y lanza un refresco en segundo plano.
3. Si no hay nada y nunca se pidio, entonces si espera a la red, guarda lo que traiga y responde.

De ahi sale lo que se buscaba: **si la red falla y hay datos guardados, la pantalla no se entera**,
porque nunca dependio de la red para pintar.

### Los puertos no cambian

`:core:application` se queda exactamente igual. El nucleo sigue sin saber si detras hay una base de
datos, una red o las dos, y las features reciben los mismos casos de uso de siempre. Todo el cambio
cabe en `:data` y en una linea por gateway de la raiz de composicion.

Eso tiene un limite que conviene decir en voz alta: **los puertos son funciones `suspend` que
devuelven un valor, no flujos**, asi que la pantalla no se actualiza sola cuando el refresco de
segundo plano trae algo distinto; lo ve en la lectura siguiente. Con un solo dispositivo, y con las
escrituras propias guardandose al momento, la diferencia no se aprecia. Deja de ser cierto en
cuanto haya dos dispositivos, y entonces habra que pasar los puertos a `Flow`, que es un cambio
mucho mayor porque toca los casos de uso y los ViewModel.

### El plazo de refresco no guarda datos

`RefreshGate` recuerda **cuando** se refresco cada cosa, nunca **que**. Los datos estan en la base.
Lo unico que evita es la rafaga: volver a una pestana cinco veces seguidas no dispara cinco
peticiones identicas.

Dos reglas suyas:

- **Un refresco que falla no cuenta como refresco.** Asi la lectura siguiente reintenta en vez de
  esperar al plazo, que es justo lo que hace falta cuando vuelve la red.
- **Una escritura lo olvida todo.** Registrar un movimiento cambia tambien el contador de uso de
  las categorias y el saldo de un prestamo. Dejar cualquiera de esas cosas marcada como fresca es
  quedarse ensenando el numero de antes.

### El limite nunca decide lo que se guarda

La paginacion la resuelve el adaptador, no el caso de uso: agota el cursor, guarda el periodo
entero y devuelve una sola pagina sin cursor. `limit` solo recorta lo que se devuelve.

Es una trampa que costaria datos: Inicio pide los cuatro ultimos movimientos del mes, y si ese
`limit` llegara a decidir lo que se guarda, abrir Inicio dejaria el mes con cuatro movimientos.

### Cambiar la moneda principal no toca la base

Los importes se guardan como los devuelve la API, con sus cuatro campos, y la conversion se hace al
pintar con las tasas del momento (reglas 2 y 3 de `CLAUDE.md`). Si se guardara convertido, cambiar
la moneda principal obligaria a reescribir la base entera y el registro dejaria de reflejar lo que
el usuario teclo.

### La cache en memoria se retira

`:data:cache` se borra en el mismo cambio, no se deja "por si acaso". Dos caches encima de la misma
cosa es la forma de acabar ensenando datos distintos segun por donde entres a la pantalla.

Lo que si sobrevive de aquel trabajo es su forma: decoradores sobre los puertos, la invalidacion
que cruza recursos y el vaciado en el adaptador de sesion. Cambia el almacen, no la arquitectura.

## Consecuencias

A favor:

- En modo avion, despues de haber abierto la aplicacion con red una vez, Inicio, Movimientos,
  Prestamos, Categorias y Ajustes se ven con datos.
- Un corte de red deja de ser una pantalla de error y pasa a no notarse.
- Los numeros derivados no cambian por haber pasado por la base: se calculan sobre los mismos
  cuatro campos crudos, y hay un test que lo comprueba.
- Cerrar sesion borra la base entera, asi que quien entre despues en el mismo telefono no ve nada
  del anterior.

En contra, y hay que saberlo:

- **Registrar sin conexion todavia falla.** Leer ya funciona sin red; escribir no. Eso es la cola
  de escrituras pendientes del [issue 16](https://github.com/eabl0306/gastonomo/issues/16), y
  hasta que exista, la mitad de la promesa de la epica esta a medias.
- **La fila de Ajustes sigue diciendo "al dia" sin mirar nada.** Eso es el
  [issue 17](https://github.com/eabl0306/gastonomo/issues/17).
- **La pantalla no se actualiza sola** con lo que trae el refresco, por lo dicho arriba.
- **Sigue habiendo un parpadeo de carga de uno o dos fotogramas** al cambiar de pestana. Lo que
  muere al cambiar de pestana es el ViewModel, y con el el estado, no solo los datos. Leer de
  disco quita la espera de red, no la reconstruccion del estado.
- **Un periodo vacio de verdad se pide a la red una vez por plazo**, porque "no hay nada guardado"
  y "el mes no tuvo movimientos" se ven igual desde la base. Lo resuelve el plazo del
  `RefreshGate`, que marca el periodo como refrescado aunque viniera vacio, pero es una sutileza
  que conviene recordar si algun dia se toca esa logica.
