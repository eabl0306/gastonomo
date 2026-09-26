# ADR 0012: Los tres POST que crean dinero aceptan `Idempotency-Key`

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

Este era el riesgo mas concreto que quedaba abierto, y llevaba cuatro revisiones seguidas
esperando una decision.

El escenario es este, y no es teorico:

1. El usuario apunta un gasto en el movil y pulsa Guardar.
2. La peticion llega a la API, se escribe el movimiento y la respuesta se pierde porque el
   telefono entro en un tunel.
3. La aplicacion, que no ha visto respuesta, reintenta.
4. Quedan dos gastos iguales.

Y no tiene arreglo desde la aplicacion: el rediseno no tiene pantalla de editar ni de
borrar movimientos, asi que el usuario ve el gasto duplicado y no puede hacer nada. El
balance queda mal hasta que alguien entre en la base de datos.

Con prestamos y abonos es peor, porque cada uno escribe dos filas: el prestamo y su
movimiento. Un reintento deja el prestamo duplicado y dos movimientos de mas.

El analisis del rediseno ya lo habia identificado y lo habia dejado fuera de la primera
version ("no es imprescindible"). Cuatro agentes lo dejaron abierto esperando que alguien
decidiera. Se decide aqui.

Las alternativas consideradas:

**No hacer nada y anadir `PATCH` y `DELETE`.** Deja al usuario arreglar el duplicado a
mano, pero son pantallas que el diseno no tiene, y de todas formas hay que disenarlas
algun dia. No resuelve el problema, lo traslada.

**Deduplicar por contenido**, rechazando un movimiento identico al ultimo. Es adivinar:
dos cafes de 5000 el mismo dia son dos gastos legitimos, y la API no puede distinguirlos
de un reintento.

**Que el cliente genere el identificador** del movimiento y lo mande en el `POST`. Funciona
y es menos codigo, pero cambia el contrato (el id deja de ser de la API), no cubre el caso
de los prestamos, donde hay dos filas con dos identificadores, y no da forma de devolver la
misma respuesta: un segundo `POST` con el mismo id seria un 409, y el cliente no sabria si
el primero llego a guardarse.

## Decision

`POST /v1/movements`, `POST /v1/loans` y `POST /v1/loans/{id}/payments` aceptan una cabecera
`Idempotency-Key`. Es el mecanismo estandar y hace exactamente lo que hace falta: distinguir
un reintento de un registro nuevo, y devolver la respuesta original.

### El protocolo

El cliente genera un valor por **intento del usuario** (un UUID sirve) y lo repite en todos
los reintentos de ese intento. Hasta 200 caracteres.

- **Primer intento**: se reserva la clave, se ejecuta la operacion y se guarda su respuesta.
- **Reintento con el mismo cuerpo**: se devuelve la respuesta guardada tal cual, con el mismo
  identificador, marcada con la cabecera `Idempotent-Replay: true`. No se escribe nada.
- **Misma clave con otro cuerpo**: **409** con `code: idempotency_key_reused`. No es un
  reintento, es reutilizacion, y guardar uno de los dos registros en silencio seria peor que
  avisar.
- **Dos intentos con la misma clave a la vez**: el segundo recibe **409** con
  `code: idempotency_key_in_progress`.
- **Cuerpo rechazado (422)**: la reserva se suelta. Un rechazo es determinista, y no
  soltarla obligaria al cliente a inventarse una clave nueva para corregir un importe mal
  tecleado.
- La clave vale **24 horas**. Un reintento razonable pasa en segundos o minutos; un dia deja
  margen de sobra para un movil que estuvo sin cobertura.
- La clave incluye el **endpoint**, asi que la misma `Idempotency-Key` en dos operaciones
  distintas son dos reservas distintas y un cliente que reutilice el valor entre pantallas no
  se bloquea a si mismo.

### Es opcional, a proposito

Sin la cabecera la peticion se comporta exactamente como antes y no se toca la tabla. Es lo
que hace que esta decision sea puramente aditiva: ningun cliente existente cambia de
comportamiento, y el que quiera la proteccion la pide. Los tres clientes de este repositorio
deberian mandarla siempre.

### Donde vive

**En el adaptador driving, no en un caso de uso.** Repetir una respuesta ya emitida es una
propiedad del transporte: el caso de uso no sabe que existe HTTP, y "devuelve el mismo 201
que la vez anterior" no es una regla de negocio. La implementacion es un decorador de
`Handler` (`crates/http/src/idempotency.rs`) que envuelve el handler de creacion; el router
lo monta alrededor de los tres `POST` y de nada mas, porque un `GET` ya es idempotente.

El almacen si es un puerto de `application`, `IdempotencyStore`, con tres operaciones:
`reserve`, `complete` y `release`. Asi el decorador no sabe que hay PostgreSQL detras, y hay
un doble en memoria para los tests del router.

### Como se guarda

```
idempotency_keys
  user_id fk, endpoint text, key text, fingerprint text,
  response_status smallint null, response_body bytea null,
  created_at, completed_at null,
  pk (user_id, endpoint, key)
```

- La reserva es **una sola sentencia atomica**:
  `INSERT ... ON CONFLICT (user_id, endpoint, key) DO NOTHING ... RETURNING key`. Si devuelve
  fila, la clave era nueva y esta peticion ejecuta; si no, se lee lo guardado. Dos reintentos
  simultaneos no pueden recibir los dos "era nueva", y eso lo garantiza la clave primaria,
  no el codigo de Rust. Hay un test que lanza las dos reservas con `tokio::join!` contra
  PostgreSQL y comprueba que solo una sale `Fresh`.
- `fingerprint` es el **SHA-256 del cuerpo**, no el cuerpo. Solo hay que poder comparar, no
  reconstruir, y asi no se duplican importes y notas en una segunda tabla.
- Un CHECK exige que `response_status` y `response_body` esten los dos puestos o los dos
  nulos. Una fila a medias haria que un reintento repitiera un cuerpo vacio.
- La purga la hace el propio adaptador al reservar una clave nueva: un `DELETE` de las filas
  de esa cuenta mas viejas que 24 horas, acotado por cuenta y por indice. Dos lineas de SQL
  en vez de otro planificador.
- `ON DELETE CASCADE` desde `users`: borrar una cuenta se lleva sus claves.

## Consecuencias

A favor:

- Desaparece la unica forma en que la aplicacion podia duplicar dinero sola.
- El cliente distingue "ya estaba guardado" de "acabo de guardarlo" por la cabecera
  `Idempotent-Replay`, que sirve para depurar y para no volver a pintar una animacion de
  exito.
- Reutilizar una clave con otros datos avisa en vez de perder un registro.
- No cambia nada para quien no manda la cabecera.

En contra, y hay que saberlo:

- **Solo se guardan las respuestas de exito.** Un 409 o un 500 no quedan cacheados, asi que
  un reintento tras un 500 vuelve a ejecutar. Es lo correcto (un 500 puede haber ocurrido
  antes o despues de escribir), pero significa que la proteccion no es absoluta: si la
  escritura ocurrio y el fallo fue despues, el reintento puede duplicar. Cerrar ese hueco
  pediria guardar la reserva en la misma transaccion que la escritura del dominio, y eso
  mete el detalle de transporte dentro del repositorio.
- **Un cuerpo en streaming no se cachearia.** Ninguno de los tres endpoints usa uno; si
  algun dia lo usara, el decorador no guarda nada en vez de guardar algo a medias.
- **La tabla crece con el uso y se purga de forma oportunista.** Si una cuenta deja de
  mandar peticiones, sus filas se quedan hasta que vuelva. Son unas pocas filas por cuenta.
- **Esto no arregla el dedazo.** Si el usuario teclea 1 860 000 en vez de 186 400, la
  idempotencia no hace nada: sigue sin haber `PATCH` ni `DELETE`, y eso sigue pendiente.
- El contrato tiene un mecanismo mas que documentar y que cada cliente tiene que
  implementar, aunque sea generar un UUID y repetirlo.
