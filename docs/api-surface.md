# La API de Gastonomo: lo que hay

Este documento describe la superficie HTTP que existe hoy en `apps/api`, endpoint por
endpoint, y por que cada cosa es como es. Nacio como analisis del rediseno (el handoff
`Gastonomo — rediseno completo` mas el prototipo `Gastonomo.dc.html`, que el propio
handoff declara fuente de verdad) y ahora describe lo construido. Donde el analisis y
el codigo se separaron, manda el codigo y se dice en que se separaron.

Es un documento tecnico: aqui se entra en detalle a proposito. Quien solo quiera
arrancar y probar tiene el [README](../README.md) y el
[README de la API](../apps/api/README.md).

La referencia ejecutable es `GET /openapi.json` del servicio en marcha. Si este
documento y ese JSON no coinciden, el JSON tiene razon.

## Estado

Las 14 pantallas del rediseno se pueden pintar con lo que existe. Los 25 endpoints, la
tarea de tasas y el esquema completo estan implementados y probados contra un
PostgreSQL de verdad. Lo que sigue fuera de alcance esta en
[Lo que no existe](#lo-que-no-existe-todavia), al final.

La decision anterior de "solo health, sin dominio" (ADR 0001 y siguientes) queda
**superada** por este rediseno.

## Las cuatro decisiones que condicionan todo

### 1. El dinero no es un float

El prototipo usa `number` de JavaScript porque es un prototipo. En produccion no vale:
`0.1 + 0.2` no es `0.3`, y esto es una app de finanzas donde el usuario compara saldos
con su banco.

- PostgreSQL: `NUMERIC(18,4)`. Cuatro decimales porque las equivalencias entre monedas
  los necesitan aunque la moneda mostrada tenga cero: un peso colombiano convertido
  desde dolares no es un entero.
- Rust: `rust_decimal::Decimal`, que SQLx mapea a `NUMERIC` directamente. El tipo del
  dominio es `gastonomo_domain::Money` y no expone el `Decimal` de dentro.
- JSON: se serializa como **string**, no como number, para que ningun cliente lo
  degrade a double al parsear. Los importes salen con los cuatro decimales de la base,
  `"amount": "186400.0000"`, y el cliente decide como se muestran.

El detalle esta en el [ADR 0009](adr/0009-dinero-con-decimal.md).

### 2. Cada registro guarda dos importes, y la tasa es del usuario

```
amount + currency        lo que el usuario tecleo, en la moneda que eligio
base   + base_currency   la equivalencia en su moneda principal al registrar
```

Si el usuario edito la conversion sugerida, lo que se guarda en `base` es **su** tasa,
no la de Frankfurter. La API guarda los cuatro campos tal cual y no recalcula nada.

Cambiar la moneda principal en Ajustes **no toca ningun registro**. Solo cambia como se
muestra y que moneda se propone la proxima vez. El valor que se ensena lo calcula el
cliente con las tasas actuales, y por eso la API sirve tasas pero nunca importes ya
convertidos: el mismo registro se ve distinto segun la moneda principal del momento, y
eso es presentacion.

El detalle esta en el [ADR 0011](adr/0011-multimoneda-de-cuatro-campos.md).

### 3. Crear un prestamo o un abono son dos escrituras atomicas

- `POST /v1/loans` crea el prestamo **y** un movimiento de categoria `Préstamo` (gasto
  si preste, ingreso si me prestaron), con titulo `Préstamo · {persona}`.
- `POST /v1/loans/{id}/payments` crea el abono **y** un movimiento de categoria
  `Abono préstamo` (ingreso si preste, gasto si me prestaron), con titulo
  `Abono · {persona}`.

Las dos escrituras van en una transaccion de PostgreSQL. Si falla la segunda no queda
el prestamo: el balance de Inicio saldria mal y el usuario no tiene pantalla para
arreglarlo. El movimiento generado lleva `loanId` y, en el caso del abono, tambien
`loanPaymentId`.

El titulo del abono es `Abono · {persona}` sin el concepto, aunque la semilla escrita a
mano del prototipo ensene `Abono · Rosa (intereses)`. La funcion `saveAbono` del
prototipo vivo genera el titulo sin concepto, y gana el prototipo vivo. El concepto se
guarda aparte, en `label` del abono.

### 4. Todo esta acotado al usuario del token

Cada tabla de dominio lleva `user_id` y cada consulta filtra por el usuario del token.
No hay endpoints de administracion ni recursos compartidos. Un recurso de otra cuenta
responde **404 y no 403**: un 403 confirmaria que existe. Lo mismo cuando el
identificador del camino no es un UUID.

## Modelo de datos

Once migraciones en `apps/api/migrations`, que la API aplica sola al arrancar.

```
users
  id uuid pk, name text, email citext unique, password_hash text,
  created_at timestamptz, updated_at timestamptz

refresh_tokens
  id uuid pk, user_id fk, token_hash text unique, expires_at timestamptz,
  revoked_at timestamptz null, replaced_by uuid null,
  user_agent text null, created_at timestamptz

password_reset_tokens
  id uuid pk, user_id fk, token_hash text unique, expires_at timestamptz,
  used_at timestamptz null, created_at timestamptz

user_settings
  user_id pk fk, display_currency char(3), language text, updated_at timestamptz

categories
  id uuid pk, user_id fk, name text, kind text ('in'|'out'|'both'),
  icon text, color text, is_default bool, created_at, updated_at,
  archived_at timestamptz null
  unique (user_id, lower(name))
  check (archived_at is null or is_default = false)

movements
  id uuid pk, user_id fk, occurred_on date, kind text ('in'|'out'),
  category_id fk, title text,
  amount numeric(18,4), currency char(3),
  base numeric(18,4), base_currency char(3),
  loan_id uuid null fk, loan_payment_id uuid null fk,
  created_at, updated_at

loans
  id uuid pk, user_id fk, person text, note text,
  direction text ('lent'|'borrowed'), opened_on date,
  amount numeric(18,4), currency char(3),
  base numeric(18,4), base_currency char(3),
  created_at, updated_at

loan_payments
  id uuid pk, loan_id fk, user_id fk, paid_on date, label text null,
  amount numeric(18,4), currency char(3),
  base numeric(18,4), base_currency char(3),
  created_at

currencies
  code char(3) pk, name text, symbol text, offered bool, updated_at timestamptz

exchange_rates
  rate_date date, base char(3), quote char(3), rate numeric(20,10),
  fetched_at timestamptz, pk (rate_date, base, quote)

idempotency_keys
  user_id fk, endpoint text, key text, fingerprint text,
  response_status smallint null, response_body bytea null,
  created_at timestamptz, completed_at timestamptz null,
  pk (user_id, endpoint, key)

dictation_allowlist
  user_id pk fk, granted_at timestamptz
```

Indices que el diseno pide de forma directa: `movements (user_id, occurred_on desc, id
desc)` para el filtro de periodo, la lista agrupada por dia y el cursor;
`movements (category_id)` para el conteo de uso;
`loans (user_id, direction)` para las pills; `loan_payments (loan_id, paid_on desc)`
para el historial del detalle.

Las fechas de registro son `DATE`, sin hora ni zona: un gasto del 22 de septiembre es
del 22 en Bogota y en Madrid. Los instantes tecnicos (`created_at`, `fetched_at`) si son
`timestamptz`.

### Categorias: por id y con tres tipos

El prototipo referencia la categoria por su nombre y el propio handoff anota
"idealmente categoryId en prod". Se hace por id, porque el diseno exige dos cosas que
con nombres se vuelven fragiles: renombrar una categoria no debe tocar los movimientos,
y borrarla tiene que poder reasignarlos con un `UPDATE` acotado.

Las 12 predeterminadas se siembran por usuario al registrarse, con `is_default = true`,
y no se pueden editar ni borrar. Son: Alimentación, Educación, Entretenimiento, Salud,
Servicios, Transporte, Vivienda, Salario, Freelance, Préstamo, Abono préstamo y Otros.

La unicidad del nombre es **entre todas las categorias del usuario, sin distinguir tipo
ni mayusculas**, porque el diseno dice "no puede repetirse con ninguna categoria".

**Correccion sobre `kind`, encontrada al implementar el esquema.** El analisis decia
`kind text ('in'|'out')`, y con eso las 12 predeterminadas no caben: el prototipo ensena
Préstamo, Abono préstamo y Otros en el grid de gasto **y** en el de ingreso, asi que con
dos valores esas tres tendrian que existir dos veces y el nombre se repetiria. Gana el
prototipo: `kind` admite `'in'`, `'out'` y `'both'`, y `'both'` esta reservado a las
predeterminadas, lo fuerza un CHECK. El cliente ofrece una categoria cuando
`kind = 'both'` o `kind` coincide con el tipo del movimiento; el sheet de crear
categoria sigue teniendo solo Gasto e Ingreso. En Rust es
`gastonomo_domain::CategoryKind`, con `accepts(kind)` e `is_selectable_by_users()`.

## Convenciones del contrato

Prefijo `/v1`. Todo JSON. Errores con `application/problem+json` (RFC 9457).

- **Toda lista va envuelta en `items`**: `/v1/movements`, `/v1/loans`, `/v1/categories`
  y `/v1/currencies`. `/v1/movements` anade `nextCursor` al lado.
- **Los mensajes de `errors[].message` vienen en castellano**, listos para pintar junto
  al campo sin traducir nada. No son el `Display` de los errores de dominio, que sigue
  en ingles porque habla con quien lee los logs. La traduccion vive en un solo sitio,
  `crates/application/src/use_case/validation_message.rs`, para que el mismo error no se
  vea distinto en dos formularios.
- **Los importes viajan como string** y las fechas de registro como `YYYY-MM-DD`.
- **Cada respuesta lleva `x-request-id`**, propagando el de la peticion si venia.

### Cuerpo de error

```json
{
  "type": "https://gastonomo.app/problems/validation",
  "title": "Datos invalidos",
  "status": 422,
  "detail": "Revisa los campos marcados.",
  "code": "category_name_taken",
  "errors": [{ "field": "amount", "message": "El importe tiene que ser mayor que cero." }]
}
```

`code` solo aparece en los conflictos (409) y `errors` solo en las validaciones (422).
El `detail` de **todos** los 401 es el mismo texto fijo, a proposito: distinguir un token
que no existe de uno caducado o ya usado diria si se acerto con uno que existe. El motivo
real se queda en la traza del servidor.

Los `type` que existen: `validation`, `invalid-credentials`, `unauthorized`,
`not-found`, `conflict`, `too-many-requests`, `service-unavailable`, `internal-error`,
`method-not-allowed`, `unsupported-media-type`, `payload-too-large` y `bad-request`.

**Los 404 y 405 del transporte tambien son `problem+json`.** Salvo los resolvia con su
pagina por defecto, que negocia el `Accept` y salia en HTML; un cliente de esta API
siempre espera JSON. Lo arregla un catcher en `crates/http/src/catcher.rs`, enganchado
en `build_service`. Solo actua cuando la respuesta ya tiene estado de error y el cuerpo
esta vacio, asi que no puede pisar lo que escriba un handler.

## Los 29 endpoints

```
GET    /health/live                  200 siempre, sin tocar dependencias
GET    /health/ready                 200 si ok, 503 si degraded
GET    /openapi.json                 el contrato completo
GET    /swagger-ui                   visor del contrato

POST   /v1/auth/register             201  limitado por frecuencia
POST   /v1/auth/login                200  limitado por frecuencia
POST   /v1/auth/refresh              200
POST   /v1/auth/logout               204
POST   /v1/auth/password/forgot      202  limitado por frecuencia
POST   /v1/auth/password/reset       204  limitado por frecuencia
POST   /v1/auth/password/change      204  con access token
GET    /v1/me                        200  con access token

GET    /v1/movements                 200  ?from=&to=&kind=&cursor=&limit=
POST   /v1/movements                 201  acepta Idempotency-Key

GET    /v1/loans                     200  ?direction=
POST   /v1/loans                     201  acepta Idempotency-Key
GET    /v1/loans/{id}                200
POST   /v1/loans/{id}/payments       201  acepta Idempotency-Key

GET    /v1/categories                200  ?includeArchived=
POST   /v1/categories                201
PATCH  /v1/categories/{id}           200
DELETE /v1/categories/{id}           200  archiva si tiene movimientos
POST   /v1/categories/{id}/restore   200

GET    /v1/settings                  200
PATCH  /v1/settings                  200

GET    /v1/changes                   200  ?since=&limit=

GET    /v1/currencies                200
GET    /v1/exchange-rates            200, o 503 sin ninguna pasada guardada

POST   /v1/dictation/parse           200  devuelve un borrador, no crea nada
```

Todo lo de `/v1` menos `auth/register`, `auth/login`, `auth/refresh`, `auth/logout`,
`auth/password/forgot` y `auth/password/reset` exige `Authorization: Bearer {accessToken}`.

### Salud

```
GET /health/live    200 siempre, sin tocar dependencias
GET /health/ready   200 si status=ok, 503 si status=degraded

{"status":"ok"|"degraded",
 "checks":[{"name":"database","status":"ok"|"error","latencyMs":0}],
 "version":"0.1.0"}
```

### Autenticacion

| Metodo | Ruta                       | Para que                                                       |
| ------ | -------------------------- | -------------------------------------------------------------- |
| POST   | `/v1/auth/register`        | Registro. Siembra las 12 categorias y los ajustes por defecto. |
| POST   | `/v1/auth/login`           | Login.                                                         |
| POST   | `/v1/auth/refresh`         | Rota el refresh token y devuelve un par nuevo.                 |
| POST   | `/v1/auth/logout`          | Revoca el refresh token recibido.                              |
| POST   | `/v1/auth/password/forgot` | Manda el enlace de recuperacion.                               |
| POST   | `/v1/auth/password/reset`  | Cambia la contrasena con el token del correo.                  |
| POST   | `/v1/auth/password/change` | Cambia la contrasena estando dentro (Ajustes).                 |
| GET    | `/v1/me`                   | Perfil: nombre y correo, para el saludo y el avatar.           |

`register` y `login` responden una sesion:

```json
{
  "accessToken": "eyJ...",
  "expiresIn": 900,
  "refreshToken": "9f3c...",
  "user": {
    "id": "uuid",
    "name": "Camilo Rojas",
    "email": "camilo@correo.com",
    "createdAt": "2026-09-25T10:00:00Z"
  }
}
```

El access token es un JWT HS256 de 15 minutos y no se guarda en la base. El refresh
token es opaco, de 30 dias, y en la base solo esta su hash (SHA-256). `refresh` rota: el
token usado se marca revocado y apunta al que lo sustituye. Presentar un token ya rotado
**revoca la familia entera de la cuenta**: un refresh token se usa una vez, asi que verlo
dos veces significa que alguien tiene una copia, y se echa a los dos. El detalle esta en el [ADR 0008](adr/0008-autenticacion-con-tokens.md).

Detalles que el diseno impone y que estan implementados:

- **Registro**: nombre, correo, contrasena de 8 caracteres o mas y `acceptedTerms` en
  `true`. El medidor de fuerza es puramente visual y vive en el cliente; la API solo
  exige el minimo de 8.
- **Hash**: Argon2id con los parametros de OWASP. No bcrypt. El calculo sale del hilo
  del runtime con `spawn_blocking`, porque son decenas de milisegundos de CPU pura y
  dejarlos en un hilo de trabajo para todas las peticiones que ese hilo llevaba a medias.
- **`forgot` devuelve 202 siempre** y con el cuerpo vacio, exista o no el correo. Si
  respondiera distinto seria un oraculo para saber quien tiene cuenta. El envio del
  correo sale de la peticion a una tarea de fondo, porque con el driver SMTP real el
  tiempo de respuesta delataba la cuenta: 50 ms si existia frente a 2 ms si no. Ahora la
  diferencia es de 1,5 ms.
- **Caducidad del enlace**: 30 minutos, que es lo que dice la pantalla. El token se
  guarda hasheado y es de un solo uso.
- **Pedir el enlace otra vez invalida los anteriores**, asi que cada cuenta tiene como
  mucho uno vivo y guardar un token para reintentar no sirve. Con tres reenvios habia
  tres enlaces abriendo la misma cuenta media hora cada uno, y el usuario no tenia
  ninguna pantalla donde verlos ni revocarlos. El correo lo avisa.
- **Reenviar** usa el mismo endpoint, esta limitado por frecuencia y mata el enlace
  anterior. Dos peticiones muy seguidas **no** matan nada: la segunda no emite y no manda
  correo, para que nadie pueda dejar a otro sin recuperar la cuenta pidiendo el enlace en
  bucle con su correo. Sigue respondiendo 202.
- Cambiar la contrasena revoca todos los refresh tokens del usuario menos el que venga
  en `refreshToken`, que es el del dispositivo desde el que se hace el cambio.
- **Limite de peticiones** en `register`, `login`, `password/forgot` y `password/reset`:
  los cuatro endpoints publicos que tocan credenciales. 10 peticiones por
  minuto y cliente, configurable. El cliente se identifica por la **ultima** entrada de
  `X-Forwarded-For`, que es la que pone el proxy de confianza, y por la IP del par si la
  cabecera no viene. Leer la primera entrada dejaba saltarse el limite por completo
  mandando una cabecera falsa, y pasaba a traves del nginx del propio repositorio,
  porque usa `$proxy_add_x_forwarded_for` y anade la IP real al final.

El PIN y la huella **no tocan la API**. Son un cerrojo local que protege el refresh
token guardado en el dispositivo. Por eso tampoco se sincronizan.

### Movimientos

| Metodo | Ruta                                           | Para que           |
| ------ | ---------------------------------------------- | ------------------ |
| GET    | `/v1/movements?from=&to=&kind=&cursor=&limit=` | Lista del periodo. |
| POST   | `/v1/movements`                                | Nuevo movimiento.  |

`from` y `to` son fechas inclusive y salen del filtro de periodo (mes o rango). `kind`
es `in`, `out` o se omite, y corresponde a las pills Todos / Ingresos / Gastos.

```json
{
  "items": [
    {
      "id": "uuid",
      "occurredOn": "2026-09-15",
      "kind": "out",
      "title": "Mercado",
      "amount": "186400.0000",
      "currency": "COP",
      "base": "186400.0000",
      "baseCurrency": "COP",
      "category": {
        "id": "uuid",
        "name": "Alimentación",
        "kind": "out",
        "icon": "restaurant",
        "color": "#cf5f35"
      },
      "loanId": null,
      "loanPaymentId": null,
      "createdAt": "2026-09-15T14:02:00Z"
    }
  ],
  "nextCursor": "2026-09-15_0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34"
}
```

La categoria viaja completa (id, nombre, tipo, icono y color) porque la fila de la
pantalla pinta el tile con ella, y el diseno pide la fila completa de una sola vez.

Paginacion por cursor sobre `(occurred_on desc, id desc)`. `limit` por defecto 100 y
como maximo 500; un valor mayor se acota en silencio. La pantalla de Movimientos agrupa
por dia y la de Inicio pide `limit=4`. Cuando `nextCursor` es `null` no hay mas paginas.
El cursor **no es opaco de verdad**: es `YYYY-MM-DD_uuid` en claro. No filtra nada que
el cliente no tenga ya en la ultima fila de la pagina, pero el nombre promete mas de lo
que hay.

En el `POST`, `base` y `baseCurrency` se pueden omitir los dos juntos, que es el caso
del usuario que teclea en su moneda principal: entonces la equivalencia es el propio
importe. Mandar uno solo de los dos es un 422 sobre `base`. Si no hay `note`, el titulo
es el nombre de la categoria.

### Prestamos

| Metodo | Ruta                      | Para que                                    |
| ------ | ------------------------- | ------------------------------------------- |
| GET    | `/v1/loans?direction=`    | Lista con sus abonos embebidos.             |
| GET    | `/v1/loans/{id}`          | Detalle.                                    |
| POST   | `/v1/loans`               | Nuevo prestamo. Crea tambien su movimiento. |
| POST   | `/v1/loans/{id}/payments` | Nuevo abono. Crea tambien su movimiento.    |

```json
{
  "id": "uuid",
  "person": "Rosa",
  "note": "Teléfono",
  "direction": "lent",
  "openedOn": "2026-06-28",
  "amount": "600000.0000",
  "currency": "COP",
  "base": "600000.0000",
  "baseCurrency": "COP",
  "payments": [
    {
      "id": "uuid",
      "loanId": "uuid",
      "paidOn": "2026-07-29",
      "label": "Intereses",
      "amount": "200000.0000",
      "currency": "COP",
      "base": "200000.0000",
      "baseCurrency": "COP",
      "createdAt": "2026-07-29T10:00:00Z"
    }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

Los abonos vienen embebidos porque la tarjeta de prestamo ya necesita el saldo, y el
saldo es el prestamo menos la suma de los abonos con las tasas actuales: sin los abonos
el cliente no puede pintar la lista. Un usuario normal tiene pocos prestamos con pocos
abonos.

`direction` es `lent`, `borrowed` o se omite. El estado (Pagado, Parcial, Pendiente), el
porcentaje, los "Intereses ganados" y el orden (abiertos primero, pagados al final)
**los deriva el cliente**, porque dependen de la moneda principal del momento. La API no
los guarda ni los calcula.

Un abono puede pasarse del saldo y no se rechaza: el diseno lo contempla y ensena el
exceso como intereses ganados.

En los dos `POST`, igual que en movimientos, `base` y `baseCurrency` son opcionales los
dos juntos, y `note` y `label` tambien. Antes eran obligatorios aqui y opcionales en
movimientos; esa asimetria no la pedia el diseno y se ha igualado.

### Categorias

| Metodo | Ruta                          | Para que                                            |
| ------ | ----------------------------- | --------------------------------------------------- |
| GET    | `/v1/categories`              | Las activas, con `isDefault` y el conteo.           |
| POST   | `/v1/categories`              | Crear.                                              |
| PATCH  | `/v1/categories/{id}`         | Editar nombre, icono, color y tipo.                 |
| DELETE | `/v1/categories/{id}`         | Archiva, o borra si no la usa nadie. Dice cual fue. |
| POST   | `/v1/categories/{id}/restore` | Devolver una archivada a la lista.                  |

`GET /v1/categories?includeArchived=true` trae tambien las archivadas. Solo cuenta el
valor exacto `true`; cualquier otra cosa se lee como que no.

```json
{
  "items": [
    {
      "id": "uuid",
      "name": "Alimentación",
      "kind": "out",
      "icon": "restaurant",
      "color": "#cf5f35",
      "isDefault": true,
      "movementCount": 12,
      "archived": false
    }
  ]
}
```

`movementCount` alimenta el texto "Sin movimientos / 1 movimiento / N movimientos" de la
pantalla de Categorias, asi que viene en el `GET` y no en un endpoint aparte. Es un
`COUNT` por categoria en cada carga; con los volumenes de una app personal no es
problema, y si algun dia lo es ya existe el indice sobre `movements (category_id)`.

**Correccion sobre los codigos de estado, hecha al implementar.** El analisis decia que
devolvian 409 el nombre vacio, el nombre largo, el nombre repetido y cualquier intento
de tocar una predeterminada. Los dos primeros son errores de forma del cuerpo y en el
resto de la API eso es un 422 con `errors: [{field, message}]`, que es justo lo que la
pantalla necesita para marcar el campo. Quedan asi:

- **422**: nombre vacio o de mas de 22 caracteres; tipo distinto de `in` u `out`; icono
  fuera de los 16 del sheet; color fuera de los 8 del sheet. Vienen todos los campos
  malos en la misma respuesta, no solo el primero.
- **409** con `code: category_name_taken`: el nombre ya existe en la cuenta, sin
  distinguir tipo ni mayusculas.
- **409** con `code: category_is_default`: se intento editar, archivar o recuperar una
  predeterminada.
- **404**: la categoria no existe, es de otra cuenta, o el identificador del camino no
  es un UUID.

#### El `DELETE` archiva, no destruye

Antes reasignaba los movimientos de la categoria a `Otros` y despues borraba la fila.
Eso escribia en movimientos ya registrados: un gasto de hace dos anos dejaba de leerse
con la categoria que tenia el dia que se creo. Es exactamente lo que la regla del
historico de solo anadir existe para impedir, por el mismo camino de siempre pero desde
otro endpoint.

Ahora:

- Si **algun movimiento la usa**, se archiva. Deja de ofrecerse al registrar y sale de la
  lista, pero la fila se queda y los movimientos siguen apuntando a ella. Archivar una ya
  archivada responde igual, y se recupera con `POST /v1/categories/{id}/restore`, que
  devuelve 200 con la categoria.
- Si **no la usa nadie**, se borra de verdad. Una categoria que nunca se uso no tiene
  historico que proteger, y guardarlas archivadas llenaria la lista de ruido.

La respuesta es **200** con `{"outcome": "archived"}` o `{"outcome": "deleted"}`, no un 204
vacio. El cliente tiene que decirle al usuario cual de las dos cosas paso, y deducirlo de su
`movementCount` significaria deducirlo de un conteo de la ultima carga, que puede estar viejo.

La condicion del conteo va tambien en el `WHERE` del `DELETE`, no solo en el caso de uso:
entre comprobar y borrar puede entrar un movimiento, y entonces quedaria apuntando a una
fila que ya no esta.

La recuperacion no es un lujo: el nombre sigue siendo unico incluyendo las archivadas, asi
que sin ella archivar por error dejaria el nombre cogido para siempre.

### Cambios incrementales

| Metodo | Ruta          | Para que                                   |
| ------ | ------------- | ------------------------------------------ |
| GET    | `/v1/changes` | Lo que ha cambiado desde el ultimo cursor. |

```json
{
  "cursor": "djE6NDI",
  "hasMore": false,
  "movements": [],
  "loans": [],
  "loanPayments": [],
  "categories": [],
  "settings": { "displayCurrency": "COP", "language": "es" }
}
```

Existe para que un arranque no tenga que bajarse el historico entero. Con cien
movimientos da igual; con tres anos de uso, no.

Como el historico es de solo anadir, "cambios" es casi siempre "cosas creadas despues
de esta marca": no hay conflictos que resolver, ni tombstones, ni versiones que
comparar. Lo unico que muta son las **categorias** (renombrar, archivar y recuperar) y
los **ajustes**. Las tasas de cambio no entran aqui: tienen su propio endpoint y
cambian a diario para todos.

Las colecciones reutilizan los DTO de siempre. Un movimiento del feed es el mismo
movimiento que devuelve `GET /v1/movements`, asi que el cliente tiene un solo mapeo por
tipo. `settings` solo aparece si cambio.

#### El cursor, que es donde se falla

**No es una marca de tiempo.** Es un numero de cambio por cuenta. Dos filas creadas en
el mismo milisegundo, o un reloj que se ajusta hacia atras, harian que el cliente se
saltara registros sin enterarse, y saltarse un movimiento es un balance que no cuadra y
ninguna pista de por que.

El numero lo reparte la base de datos con un contador por usuario, y el reparto toma un
cerrojo de fila. De ahi sale la propiedad que hace falta: **cuando el numero N es
visible, ninguno menor que N esta todavia por confirmar**, asi que un cliente que va por
N no puede perderse nada.

Viaja **opaco**: se guarda tal cual y se devuelve tal cual. Un cursor que no sea de esta
API se responde con 422, no se lee como "desde el principio": eso haria que el cliente se
bajara el historico entero sin saber por que. El `nextCursor` de movimientos si es
legible (`YYYY-MM-DD_uuid`), y es precisamente lo que no se quiere repetir.

**El cliente avanza su cursor despues de guardar el lote**, nunca al recibirlo. Si la
escritura local falla y el cursor ya subio, ese lote se pierde para siempre.

`since` ausente es el arranque en frio. `limit` cuenta las cinco colecciones juntas: 200
por defecto, 1000 como tope, y por encima se acota sin fallar.

Un abono puede llegar dos veces en el mismo lote, dentro de su prestamo y en
`loanPayments`, si los dos cambiaron en el rango. Todo se guarda por identificador, asi
que guardarlo dos veces deja lo mismo que guardarlo una.

### Ajustes

| Metodo | Ruta           | Para que                   |
| ------ | -------------- | -------------------------- |
| GET    | `/v1/settings` | Moneda principal e idioma. |
| PATCH  | `/v1/settings` | Cambiar una o las dos.     |

```json
{ "displayCurrency": "COP", "language": "es" }
```

**Solo se sincronizan `displayCurrency` y `language`.** El toggle de huella o PIN y el
ojo que oculta los montos se quedan en el dispositivo: el bloqueo biometrico protege un
secreto que vive en ese telefono, y `hideAmounts` es estado efimero de la tarjeta de
balance. El indicador "Sincronizacion en la nube: Al dia / Pausada" es estado de
conectividad del cliente, no un campo de la API.

`PATCH` exige que la moneda exista en `currencies` con `offered = true`, con dos
excepciones que se anadieron despues de ver el arranque en frio:

- La moneda que la cuenta **ya tiene puesta** se acepta sin mirar el catalogo. El sheet
  ensena la moneda actual marcada y volver a confirmarla no puede fallar.
- Si el catalogo esta vacio del todo, la respuesta es **503** y no 422: no es que la
  moneda no valga, es que la tarea de tasas no ha conseguido su primera pasada, y un 422
  culpaba al cliente de algo que no puede arreglar.

### Monedas y tasas

| Metodo | Ruta                 | Para que                          |
| ------ | -------------------- | --------------------------------- |
| GET    | `/v1/currencies`     | Catalogo para el sheet de moneda. |
| GET    | `/v1/exchange-rates` | Tasas vigentes con base USD.      |

`GET /v1/currencies` devuelve solo las monedas que tienen tasa y se siguen ofreciendo,
ya filtradas: fuera los metales (XAU, XAG, XPT, XPD) y los XDR, tal como hace el
prototipo. Asi el cliente no repite ese filtro. Hoy son 161 monedas.

```json
{ "items": [{ "code": "COP", "name": "Colombian Peso", "symbol": "$" }] }
```

```json
{
  "base": "USD",
  "date": "2026-09-25",
  "fetchedAt": "2026-09-26T03:49:15Z",
  "status": "ok",
  "rates": { "USD": "1", "COP": "3316.44", "EUR": "0.87732" }
}
```

`date` es la fecha de las tasas y es lo que Ajustes muestra en "Tasas de cambio:
Frankfurter · {fecha}". `status` es `ok` o `stale`: si el ultimo refresco fallo y los
datos tienen mas de 48 horas, se sirven igualmente pero marcados, y el cliente pinta
"Sin conexion".

Sin ninguna pasada guardada, `GET /v1/exchange-rates` responde **503** con
`type: service-unavailable`. Nada ha fallado: falta un dato que llena una tarea de
fondo, y un 500 invitaria al cliente a tratarlo como un fallo del servidor.

### Dictado

| Metodo | Ruta                  | Para que                                                 |
| ------ | --------------------- | -------------------------------------------------------- |
| POST   | `/v1/dictation/parse` | Convierte una frase dictada en un borrador para revisar. |

La voz la pasa a texto el cliente, con el reconocimiento del sistema: ya sabe el idioma,
no cuesta nada y no hay que subir audio a ninguna parte. Aqui solo llega el texto.

```json
{ "text": "gasté 45 mil en el almuerzo", "today": "2026-09-29" }
```

`today` es opcional y lo manda el cliente porque **la zona horaria es suya**: el servidor
no sabe si para quien dicta ya es manana. Solo se usa para resolver "ayer" y para cortar
las fechas futuras. Una fecha ilegible se ignora y se usa el dia del servidor, en vez de
perder lo que el usuario acaba de decir por el formato de un campo auxiliar.

La respuesta es un borrador, y **no se ha creado nada**:

```json
{
  "kind": "movement",
  "movement": {
    "kind": "out",
    "category": { "id": "0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34", "name": "Alimentación" },
    "note": "Almuerzo"
  },
  "loan": null,
  "amount": "45000.0000",
  "currency": "COP",
  "occurredOn": "2026-09-29",
  "missing": []
}
```

`kind` dice cual de los dos objetos viene relleno; el otro es `null`. Para un prestamo
`kind` es `loan`, `movement` es `null` y `loan` trae el sentido, con quien es y la nota:

```json
{ "direction": "lent", "person": "Rosa", "note": "" }
```

**Confirmar es una llamada aparte** a `POST /v1/movements` o `POST /v1/loans`, con su
`Idempotency-Key`. Por eso este endpoint no acepta esa cabecera: no crea nada, y repetirlo
no puede duplicar dinero. El historico es de solo anadir y no puede depender de que un
modelo acierte a la primera.

**El importe viaja como string**, igual que en todo el contrato, y es `null` si no se
entendio. Al modelo tambien se le pide como texto, para que no pase por coma flotante ni
un instante.

#### La normalizacion la hace la API, no el cliente

El borrador sale ya comprobado contra la cuenta, y **ninguna regla rechaza la peticion**:
lo que no encaja se sustituye por algo que si encaja, y lo que falta se avisa en `missing`.
Un borrador a medias que el usuario completa es mejor que un error sin nada que revisar.

- Una **categoria** que no existe cae en "Otros". Se busca por nombre, sin distinguir
  mayusculas, entre las del tipo del movimiento.
- Las **reservadas de prestamos** (`Préstamo` y `Abono préstamo`) no se le ofrecen al
  modelo y tampoco se le pueden asignar: un movimiento suelto no puede colgarse de ellas.
- Una **moneda** que hoy no se puede equivaler cae en la moneda principal de la cuenta.
  Guardar un importe cuya equivalencia nadie puede calcular rompe la regla de los cuatro
  campos, y el usuario no tiene forma de arreglarlo despues.
- Una **fecha** futura o ilegible cae en hoy.
- Un **importe** que no es positivo se devuelve como `null`.

Vive en la API a proposito. En los clientes habria que escribirlo dos veces, en Kotlin y
en TypeScript, y conseguir que no se separaran nunca.

`missing` lista lo que el usuario tiene que completar antes de poder guardar: `"amount"`,
`"person"`, o los dos. El cliente deja el boton de guardar apagado mientras no este vacio.
Que lo diga la API es lo que evita que el movil y la web no coincidan.

#### Solo las cuentas aprobadas

Cada peticion sale a un modelo de lenguaje que se paga por palabra, y quien la paga es el
dueno de la instancia. Por eso el acceso se concede cuenta a cuenta, y la lista es una
tabla:

```sql
INSERT INTO dictation_allowlist (user_id) VALUES ('<uuid>');
```

Quitar el acceso es borrar la fila. Quien no esta recibe **404, no 403**, por la misma
razon que un recurso de otra cuenta: un 403 confirmaria que la capacidad existe. La
comprobacion es lo primero que se hace, antes de leer nada de la cuenta y antes de llamar
al modelo.

#### Codigos

| Codigo | Cuando                                                             |
| ------ | ------------------------------------------------------------------ |
| 200    | Borrador listo para revisar. No se ha creado nada.                 |
| 401    | Falta el access token o no vale.                                   |
| 404    | La cuenta no tiene el dictado aprobado.                            |
| 422    | El texto viene vacio, o pasa de 500 caracteres.                    |
| 503    | La instancia no tiene clave configurada y el dictado esta apagado. |
| 500    | Fallo interno, o el interprete no responde.                        |

El 503 sigue el mismo criterio que `GET /v1/exchange-rates` sin ninguna pasada guardada:
no ha fallado nada, falta un dato de configuracion, y un 500 invitaria al cliente a
tratarlo como un fallo del servidor. Sin clave la API arranca igual y el resto del
contrato no se entera.

El limite de 500 caracteres no es del modelo: un registro dictado cabe en una frase, y el
tope evita que alguien mande un libro a una capacidad que se paga por palabra.

Las cuatro variables de configuracion estan en [.env.example](../.env.example). El porque
de todo esto, y las alternativas que se descartaron, en el
[ADR 0024](adr/0024-dictar-un-registro.md).

## La tarea de refresco de tasas

Los clientes **no llaman a Frankfurter**. Solo llama nuestra API, y guardada la
respuesta la sirve desde PostgreSQL. El detalle esta en el
[ADR 0010](adr/0010-cacheo-de-tasas-de-cambio.md).

- **Fuente**: `GET https://api.frankfurter.dev/v2/rates?base=usd` y
  `GET https://api.frankfurter.dev/v2/currencies`.
- **Frecuencia**: dos veces al dia, a las 06:00 y a las 18:00 UTC. Frankfurter publica
  una vez al dia sobre las 16:00 CET tomando las referencias del BCE, asi que dos
  pasadas bastan de sobra y dejan margen si una falla.
- **Al arrancar**: refresca ya si no hay tasas o la ultima tiene mas de 24 horas. Sin
  esto un despliegue en limpio arranca sin poder convertir nada.
- **Reintentos**: tres intentos con espera creciente (2 s, 4 s, 8 s). Si fallan los tres
  se deja lo que haya y se marca `stale`; nunca se borra lo anterior.
- **Escrituras**: un `INSERT ... ON CONFLICT DO UPDATE` por `(rate_date, base, quote)`,
  todo en una transaccion. Si Frankfurter devuelve la misma fecha que ya tenemos, la
  operacion no cambia nada.
- **Con varias replicas** solo refresca una: `pg_try_advisory_xact_lock`. Las demas
  siguen. Es una linea de SQL y ahorra montar un planificador aparte.
- **El catalogo no se borra nunca.** Una moneda que desaparece de la fuente se marca
  `offered = false` y deja de ofrecerse, porque los registros que la usan siguen
  existiendo.
- Se apaga con `GASTONOMO__EXCHANGE_RATES__ENABLED=false`, que es lo que hacen los tests
  y la CI para no salir a internet.

En la arquitectura hexagonal esto encaja asi: el caso de uso `RefreshExchangeRates` vive
en `application` y depende de dos puertos, `ExchangeRateProvider` (lo implementa el
adaptador de Frankfurter con `reqwest`) y `ExchangeRateRepository` (lo implementa el de
PostgreSQL). El planificador es **otro adaptador driving**, igual que el router HTTP: un
bucle de tokio que despierta y llama al caso de uso. Eso deja el caso de uso testeable
con un proveedor falso, sin red y sin esperar doce horas.

## Idempotencia de los tres POST que crean dinero

El riesgo es concreto: el usuario guarda un gasto, pierde cobertura antes de ver la
respuesta y la aplicacion reintenta. Sin proteccion quedan dos gastos, y como el diseno
no tiene pantalla de edicion el usuario no puede arreglarlo desde la app.

`POST /v1/movements`, `POST /v1/loans` y `POST /v1/loans/{id}/payments` aceptan una
cabecera **`Idempotency-Key`**:

- **Es opcional.** Sin ella la peticion se comporta como siempre y no se toca la tabla.
  El cliente deberia mandarla siempre.
- El cliente genera un valor por intento del usuario y lo repite en todos los reintentos
  de ese intento. Hasta 200 caracteres.
- El primer intento ejecuta y guarda su respuesta. El reintento recibe **la misma
  respuesta con el mismo identificador**, marcada con la cabecera
  `Idempotent-Replay: true`, y no se crea un segundo registro.
- La clave se guarda con el SHA-256 del cuerpo. La misma clave con otro cuerpo es
  **409** con `code: idempotency_key_reused`: no es un reintento, y perder uno de los
  dos registros en silencio seria peor.
- Dos intentos con la misma clave a la vez: el segundo recibe **409** con
  `code: idempotency_key_in_progress`. La reserva es un
  `INSERT ... ON CONFLICT DO NOTHING ... RETURNING`, asi que solo uno puede ejecutar.
- Un cuerpo rechazado (422) **no quema la clave**: se suelta la reserva, porque el
  cliente tiene que poder corregir un importe y reintentar.
- La clave vale 24 horas. La purga la hace el propio adaptador al reservar una clave
  nueva, con un `DELETE` acotado por cuenta y por indice.
- La misma clave en dos operaciones distintas son dos reservas distintas, porque el
  endpoint entra en la clave.

Esto vive en el adaptador driving (`crates/http/src/idempotency.rs`) y no en un caso de
uso: repetir una respuesta ya emitida es una propiedad del transporte, no una regla de
negocio. El almacen si es un puerto de `application` (`IdempotencyStore`), asi que el
nucleo no sabe que hay PostgreSQL detras. El detalle, las alternativas descartadas y el
hueco que queda estan en el [ADR 0012](adr/0012-idempotencia-con-cabecera.md).

## Lo que el cliente deriva, y no pide

Cuatro vistas del rediseno se pintan **todas** con la lista de movimientos del periodo,
sin ningun endpoint mas: la tarjeta de balance, el grafico de tendencia, el de gastos por
categoria y el resumen de tres columnas. Se comprobo derivandolas igual que el
prototipo:

- La tendencia acumula desde cero el dia `from`, no arrastra saldo anterior, asi que no
  hace falta un endpoint de saldo previo.
- Cada movimiento ya trae su categoria completa, asi que el grafico por categoria no
  necesita cruzar nada.

Con una condicion: esas cuatro vistas necesitan el periodo **completo**, y la lista
pagina a 500 como maximo. Para "Este ano" o un rango largo el cliente tiene que agotar
el cursor antes de dibujar el primer grafico. No hay endpoint de agregados, y es
aceptable para una app personal; si algun dia molesta, un `GET /v1/movements/summary` con
ingresos, gastos y el acumulado por dia lo resuelve en una llamada.

Y con un detalle del resumen de tres columnas de Movimientos: el prototipo lo calcula
sobre el periodo **sin filtrar**, mientras que las pills Todos / Ingresos / Gastos solo
filtran la lista de abajo. Si el cliente pide `kind=out` no puede calcular la columna de
Ingresos. Tiene que pedir sin filtrar y filtrar en local, o hacer dos llamadas.

## Lo que no existe todavia

- **Login con Google.** Pedido omitir. La pantalla mantiene el hueco.
- **Editar y borrar movimientos, prestamos y abonos. Y no es un hueco: es la decision
  central del producto.** El historico es de solo anadir, a proposito y para siempre. No
  habra `PATCH` ni `DELETE` de estos recursos.

  El motivo es de integridad: que alguien que coja el movil o robe la cuenta no pueda
  alterar el historial financiero sin que el dueno lo note. Un registro que no se puede
  modificar es lo que hace que lo que se ve sea lo que paso. La version anterior de la
  aplicacion, que era completamente local, ya funcionaba asi.

  Un error se corrige **registrando otro movimiento que lo compense**, no tachando el
  anterior. Es la logica de un libro contable. Por eso cada movimiento lleva nota: para
  poder explicar por que se registra.

  Dos consecuencias que se derivan de esto y conviene no perder de vista:

  - **La idempotencia pasa a ser imprescindible, no un lujo.** Un movimiento duplicado por
    un reintento es permanente, porque no hay forma de borrarlo. Ver el ADR 0012.
  - **Sincronizar es trivial.** Si nada se edita, dos dispositivos no pueden tocar el mismo
    registro, asi que no hay conflictos que resolver: solo subir lo pendiente y bajar lo
    nuevo.

- **Exportacion a CSV y Excel.** La genera el cliente; la API solo tiene que dejar leer
  todo el historial, y para "Todo el historial" el cliente pagina hasta el final.
- **PIN y huella**, por lo dicho en Autenticacion.
- **`GET /v1/categories/{id}`, `GET /v1/movements/{id}` y
  `GET /v1/loans/{id}/payments`.** No existen porque ninguna pantalla del rediseno los
  necesita: las categorias y los movimientos se leen en lista, y los abonos viajan
  dentro del prestamo.

## Lo que hay que vigilar

- **El access token no se invalida al cerrar sesion ni al cambiar la contrasena.** Los
  refresh tokens si se revocan, pero el access sigue valido hasta 15 minutos. Es el
  compromiso habitual de un JWT sin estado y esta decidido a proposito; el
  [ADR 0008](adr/0008-autenticacion-con-tokens.md) explica que costaria cambiarlo.
- **El limite de peticiones es una ventana en memoria del proceso.** Con varias replicas
  el limite efectivo se multiplica por el numero de replicas. Un limite compartido
  necesita Redis o equivalente.
- **Arranque en frio.** Si el primer refresco de tasas falla, `currencies` esta vacia:
  `GET /v1/currencies` devuelve una lista vacia, `GET /v1/exchange-rates` responde 503 y
  `PATCH /v1/settings` solo acepta la moneda que la cuenta ya tenia. Registrar y guardar
  movimientos funciona igual, pero el sheet de moneda sale vacio. La tabla la llena la
  tarea a proposito, para no tener un catalogo escrito a mano que se quede viejo.
- **`GASTONOMO__AUTH__ACCESS_TOKEN_SECRET` tiene valor de desarrollo por defecto** y el
  arranque solo deja un aviso en el log. Hay que fijarlo de verdad antes de cualquier
  despliegue.
- **Argon2id** usa los parametros de OWASP, que son caros a proposito. Con la carga de
  una app personal no molesta; el calculo ya sale del hilo del runtime.
- **Zonas horarias.** Las fechas de los registros son `DATE`. Meter `timestamptz` aqui
  haria que un movimiento saltara de dia segun donde este el usuario.
- **El dictado es la unica parte del contrato que cuesta dinero por peticion**, y sale a
  internet cada vez. No tiene cache, porque dos frases iguales pueden ser dos registros
  distintos. Lo que lo acota son tres topes: la lista de aprobados, los 500 caracteres de
  texto y el tope de palabras de la respuesta. Si la clave no esta puesta el endpoint
  responde 503 y nada mas del contrato se entera.
- **`jsonwebtoken` va sin su feature `rust_crypto`** porque arrastra el crate `rsa` con
  RUSTSEC-2023-0071 sin parche, y tumbaria `cargo deny`. El proveedor HS256 esta hecho a
  mano con `hmac` y `sha2` en `crates/infrastructure/src/security/hs256.rs`. Solo
  soporta HS256.
