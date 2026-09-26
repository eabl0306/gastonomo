# ADR 0010: Las tasas de cambio se cachean en PostgreSQL y las refresca una tarea

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

El rediseno es multimoneda: el usuario elige una moneda principal, puede apuntar un gasto
en otra moneda y la aplicacion le propone la conversion. Ademas el saldo de un prestamo y
todos los totales se calculan con las tasas del momento, asi que **cada pantalla que
ensena una cifra necesita tasas**.

La fuente es [Frankfurter](https://frankfurter.dev), que es gratuita, sin clave y sin
limite publicado, y sirve las referencias del Banco Central Europeo.

La peticion explicita fue esta: los clientes no deben llamar a Frankfurter constantemente;
los datos tienen que estar en nuestra API y una tarea los refresca cada cierto tiempo
razonable, por ejemplo dos veces al dia.

Hay tres razones de fondo para hacerlo asi, mas alla de no abusar de un servicio gratuito:

- **La aplicacion tiene que funcionar con Frankfurter caido.** Si cada cliente llama a la
  fuente, un corte de la fuente es un corte de la aplicacion.
- **Frankfurter publica una vez al dia.** Llamar mas es trabajo tirado: las referencias del
  BCE salen sobre las 16:00 CET y no cambian hasta el dia siguiente.
- **Tres clientes multiplican las llamadas por tres**, y cada uno tendria que implementar
  su propio cacheo, su propio filtro de monedas y su propia idea de "datos viejos".

## Decision

La API es el unico proceso que sale a Frankfurter. Guarda cada pasada en PostgreSQL y
sirve `GET /v1/currencies` y `GET /v1/exchange-rates` desde ahi.

### Donde encaja en la arquitectura

El caso de uso `RefreshExchangeRates` vive en `application` y depende de dos puertos:

- `ExchangeRateProvider`, que implementa `FrankfurterExchangeRateProvider` con `reqwest`.
- `ExchangeRateRepository`, que implementa `PostgresExchangeRateRepository`.

**El planificador es otro adaptador driving**, igual que el router HTTP: un bucle de tokio
(`ExchangeRateScheduler`) que despierta, llama al caso de uso y vuelve a dormir. Se arranca
en el composition root, junto al router.

Eso es lo que hace el caso de uso testeable: los tests usan un proveedor falso, no tocan la
red y no esperan doce horas. Y significa que el nucleo no sabe que la fuente es Frankfurter
ni que hay un reloj detras.

### Que se guarda

```
currencies       code pk, name, symbol, offered, updated_at
exchange_rates   pk (rate_date, base, quote), rate numeric(20,10), fetched_at
```

La base es siempre USD (`RATES_BASE`). Una sola base y no una matriz: convertir de COP a
EUR es dividir por la tasa de COP y multiplicar por la de EUR, y guardar la matriz completa
seria 161 por 161 filas por dia para no ahorrar nada.

`rate` es `NUMERIC(20,10)`. Diez decimales porque una tasa como `1 USD = 0.00002 XYZ`
necesita precision por el otro lado.

La clave primaria por `(rate_date, base, quote)` deja el historico. Hoy solo se consulta la
ultima pasada, pero guardar por fecha no cuesta nada y abre la puerta a "cuanto valia el
dolar cuando registre este gasto" sin migrar nada.

### Cuando refresca

- **A las 06:00 y 18:00 UTC**, configurable con
  `GASTONOMO__EXCHANGE_RATES__REFRESH_HOURS_UTC=6,18`. Dos pasadas bastan de sobra para una
  fuente que publica una vez al dia, y dejan margen si una falla. Una hora mal escrita se
  ignora y, si no queda ninguna valida, se cae a las dos por defecto: una variable con un
  dedazo no puede dejar la aplicacion sin tasas.
- **Al arrancar**, si no hay tasas o la ultima tiene mas de 24 horas. Sin esto un
  despliegue en limpio arranca sin poder convertir nada.
- **Tres intentos con espera creciente** (2 s, 4 s, 8 s). Si fallan los tres se deja lo que
  haya y ya esta; **nunca se borra lo anterior**. Unas tasas de ayer sirven; ninguna, no.
- `GASTONOMO__EXCHANGE_RATES__ENABLED=false` la apaga. Es lo que usan los tests y la CI
  para no salir a internet.

### Como se escribe

Toda la pasada va en una transaccion: o queda entera, o no queda nada. Las tasas entran con
un `INSERT ... ON CONFLICT (rate_date, base, quote) DO UPDATE`, con las 161 monedas en una
sola sentencia usando `UNNEST` de dos arrays en vez de 161 sentencias. Si Frankfurter
devuelve la misma fecha que ya tenemos, la operacion no cambia nada.

**Con varias replicas solo refresca una**: `pg_try_advisory_xact_lock`. La que consigue el
cerrojo refresca y las demas siguen su camino. Es de transaccion, asi que se suelta en el
commit y no vuelve al pool puesto. Una linea de SQL en lugar de montar un planificador
aparte o un lider elegido.

### Como se sirve

`GET /v1/exchange-rates` devuelve la ultima pasada con un campo `status`:

- `ok`, o
- `stale` si los datos tienen mas de 48 horas. Se sirven igualmente, marcados, y el cliente
  pinta "Sin conexion". 48 horas y no 24 porque una pasada fallida no es un problema: entre
  dos publicaciones del BCE no ha cambiado nada.

`GET /v1/currencies` devuelve solo lo que tiene tasa y sigue ofreciendose, ya filtrado:
fuera los metales (XAU, XAG, XPT, XPD) y los XDR, igual que hace el prototipo. El filtro se
hace una vez en el servidor para que no lo repita cada cliente.

**El catalogo no se borra nunca.** Si una moneda desaparece de la fuente se marca
`offered = false` y deja de ofrecerse en el sheet, porque los registros que la usan siguen
existiendo y hay que poder seguir mostrandolos.

Sin ninguna pasada guardada, `GET /v1/exchange-rates` responde **503** con
`type: service-unavailable`, no 500. Nada ha fallado: falta un dato que llena una tarea de
fondo, y un 500 invitaria al cliente a tratarlo como un fallo del servidor en vez de
reintentar. Esa distincion es una variante propia del error de aplicacion,
`AppError::Unavailable`.

## Consecuencias

A favor:

- Frankfurter recibe dos peticiones al dia en total, no dos por usuario y pantalla.
- Un corte de la fuente no es un corte de la aplicacion: se sirve lo ultimo, marcado.
- El filtro de monedas y la idea de "datos viejos" viven en un sitio, no en tres clientes.
- El caso de uso se prueba con un proveedor falso, sin red.

En contra, y hay que saberlo:

- **El arranque en frio con el primer refresco fallido deja la aplicacion a medias.** La
  tabla `currencies` solo la llena la tarea: no hay semilla en ninguna migracion. En ese
  estado `GET /v1/currencies` devuelve lista vacia, `GET /v1/exchange-rates` responde 503 y
  el sheet de moneda sale vacio. Registrar y guardar movimientos funciona, y `PATCH
/v1/settings` acepta la moneda que la cuenta ya tenia puesta, asi que la aplicacion no
  queda muerta.

  Se decidio **no** sembrar el catalogo en una migracion. Una lista de 161 monedas escrita
  a mano en un `.sql` se queda vieja, se separa de lo que la fuente publica de verdad y
  crea un segundo sitio donde mirar cuando algo no cuadra. Lo que si se hizo es que el
  estado se distinga: 503 en lugar de 500 y 503 en lugar de 422 en Ajustes, para que el
  cliente sepa que tiene que reintentar y no que ha mandado algo mal.

- Las tasas pueden tener hasta 12 horas, o 48 si algo falla. Para apuntar gastos personales
  es de sobra; para operar divisas no lo seria, pero esta aplicacion no hace eso.
- La tabla `exchange_rates` crece unas 161 filas al dia y nadie las purga. Son unos 60 000
  filas al ano, que es nada, pero conviene saber que crece.
- La conversion de COP a EUR pasa por USD, asi que arrastra dos redondeos en vez de uno.
  Con diez decimales en la tasa el error queda muy por debajo de lo que cualquier pantalla
  muestra.
