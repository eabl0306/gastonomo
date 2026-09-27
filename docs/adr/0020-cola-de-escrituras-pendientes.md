# ADR 0020: Registrar sin conexion: la cola de escrituras pendientes

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

El [ADR 0019](0019-leer-de-la-base-local.md) dejo la aplicacion leyendose sin conexion, pero
escribir seguia necesitando red: registrar un gasto en el metro fallaba. La mitad de la promesa de
la epica quedaba sin cumplir, y es la mitad que mas duele, porque una aplicacion de finanzas
personales se usa justo donde no hay cobertura.

Hay una restriccion que lo hace mas facil y otra que lo hace mas peligroso.

La que lo hace mas facil: **el historico es de solo anadir**. Nada se edita ni se borra nunca, asi
que dos dispositivos no pueden tocar el mismo registro y no hay conflictos que resolver.
Sincronizar es subir lo pendiente.

La que lo hace mas peligroso: por eso mismo, **un duplicado es permanente**. No hay pantalla para
borrar un movimiento. Si un reintento crea el gasto dos veces, ahi se queda.

## Decision

### Se escribe en la base primero y se encola la subida

Sin red, el registro se guarda marcado como pendiente y se anade una fila a la cola. El usuario lo
ve en la lista y **cuenta en el balance**: es dinero que ya gasto, lo unico que falta es que el
servidor se entere.

La marca en la lista es deliberadamente discreta, una nota y no un aviso, por lo mismo.

### La clave de idempotencia se guarda con la fila y no se regenera nunca

Es la decision central de todo esto. La API acepta `Idempotency-Key` en los tres POST que crean
dinero (ADR 0012) y esto es exactamente para lo que existe.

La clave se genera al encolar, vive en la fila y se reutiliza en **todos** los reintentos. Si se
regenerara, un reintento despues de un timeout que en realidad si llego al servidor crearia el
gasto dos veces. Hay dos tests para esto: uno comprueba que las tres llamadas a la API llevan la
misma clave, y otro simula el timeout que si llego y comprueba que no se duplica.

Los tres casos del ADR 0012 se tratan distinto:

| Respuesta                         | Que se hace                                                                   |
| --------------------------------- | ----------------------------------------------------------------------------- |
| `409 idempotency_key_in_progress` | Esperar: hay otro intento en vuelo                                            |
| `409 idempotency_key_reused`      | Dejar de reintentar: es un fallo nuestro, no del usuario                      |
| `422`                             | Marcar como rota. **No quema la clave**: se corrige y se reenvia con la misma |

### WorkManager, no una corrutina propia

Por dos cosas que no hay que escribir: **sobrevive a que se cierre la aplicacion** y reintenta con
espera creciente. Sin lo primero, registrar un gasto en el metro y cerrar la aplicacion dejaria el
gasto sin subir hasta la proxima vez que el usuario la abriera, que puede ser dias.

El trabajo es unico y se pide con `KEEP`, no con `REPLACE`: reemplazarlo reiniciaria la espera
creciente en cada registro nuevo, y una racha de registros sin red dejaria el reintento siempre
empezando de cero.

Devuelve `retry()` y nunca `failure()` mientras quede cola. `failure` es definitivo para
WorkManager, y lo que se perderia es lo que el usuario registro.

### Un abono espera a su prestamo

La cola tiene orden de insercion y dependencia explicita, no dos colas independientes. Mientras el
prestamo no exista en el servidor, su abono no tiene a que engancharse. Cuando el prestamo sube,
sus abonos pasan a apuntar al identificador de verdad en una sola sentencia, para que no exista un
instante en el que el abono este suelto.

Crear un prestamo o un abono apunta **tambien** su movimiento, para que el balance cuadre en el
acto. Al subir se manda **un solo POST**: el movimiento lo crea la API en la misma transaccion, y
el provisional se borra cuando llega el de verdad.

### Un refresco no puede borrar lo que no se ha subido

Es la regla que mas cuidado necesita y la mas facil de romper sin darse cuenta. Bajar el periodo
de la API reemplaza lo guardado; si ese reemplazo alcanzara a las filas pendientes, se perderia lo
que el usuario acaba de registrar, que es **el peor fallo posible de esta aplicacion**.

Todos los borrados de refresco llevan `AND pending = 0`, y hay un test por cada uno de los tres
registros.

### Lo que la API rechaza se ve y se puede arreglar

Ajustes deja de decir "al dia" cuando hay algo sin enviar: dice cuantos son y lleva a una pantalla
que los lista. Un registro rechazado no puede desaparecer en silencio; si desapareciera, el usuario
creeria que ese gasto esta y no estaria en ninguna parte.

Lo que se corrige ahi es **el importe**, que es lo que la API rechaza cuando rechaza algo. Cambiar
lo demas seria escribir otro registro, no corregir este. La equivalencia se recalcula con la tasa
que tenia el registro, no con la de hoy: es el mismo gasto del mismo dia con otro numero.

Descartar pregunta antes, y se lleva tambien el registro provisional. Sin eso quedaria un
movimiento fantasma contando en el balance para siempre, sin forma de quitarlo.

## Consecuencias

A favor:

- Registrar un gasto ya no depende de tener cobertura, que era el caso de uso real.
- Un duplicado permanente, que es el fallo que no se puede deshacer, esta cubierto por la clave de
  idempotencia y por dos tests que lo atacan de frente.
- Lo que la API rechaza tiene salida: se corrige o se descarta, pero no se pierde solo.

En contra, y hay que saberlo:

- **Registrar sin conexion necesita haber abierto la aplicacion con red antes.** El movimiento se
  guarda con su categoria completa, y si el catalogo de categorias no esta en la base no hay con
  que pintarlo. En ese caso el registro falla como antes.
- **Cerrar sesion con cosas pendientes las pierde.** La base se vacia entera al cambiar de cuenta,
  que es lo correcto por privacidad, pero se lleva la cola. Hoy no se avisa.
- **La multimoneda sin conexion sigue sin degradarse.** Registrar en otra moneda usa la ultima tasa
  bajada sin decirlo. Eso es el [issue 17](https://github.com/eabl0306/gastonomo/issues/17).
- **Nada de esto se ha visto en un telefono.** No hay emulador en esta maquina, asi que lo que hay
  son tests: de la cola, del que sube, del worker y de la pantalla. El comportamiento real de
  WorkManager con la pantalla apagada y la bateria baja hay que mirarlo en un dispositivo.
- **La correccion se limita al importe.** Si algun dia la API rechaza por otro campo, hara falta
  abrir el formulario de verdad con el borrador cargado, que es un cambio mayor.
