# Convenciones del repositorio

Este fichero es para quien llegue a trabajar aqui, persona o agente. Es preciso a
proposito. El README es el resumen; esto es el detalle.

## Que es este repositorio

Monorepo poliglota con tres aplicaciones que comparten un contrato HTTP:

| Ruta           | Que es               | Toolchain                                    | Comando de verificacion |
| -------------- | -------------------- | -------------------------------------------- | ----------------------- |
| `apps/api`     | API HTTP             | Cargo, Rust estable, ed. 2024                | `just api-check`        |
| `apps/web`     | SPA                  | pnpm 11.18, Node 24, Vite                    | `just web-check`        |
| `apps/android` | App nativa           | Gradle 9.6, Kotlin 2.2, JDK 25 (bytecode 17) | `just android-check`    |
| `packages/*`   | Config compartida JS | pnpm workspace                               | `just root-lint`        |
| `tools/checks` | Reglas comunes       | Node puro, sin dependencias                  | `just guard`            |

Usa **pnpm**, nunca npm ni yarn. La version esta fijada en `packageManager`.

## Arquitectura: la regla que no se negocia

Arquitectura hexagonal en las tres. El nucleo (dominio mas casos de uso) no conoce
framework, ni HTTP, ni SQL, ni React, ni Android. Los detalles estan en
[docs/architecture.md](docs/architecture.md). Lo minimo que hay que saber:

- **API**: un crate por capa. `domain` y `application` no pueden depender de nada de
  infraestructura; lo verifica `apps/api/crates/api/tests/architecture.rs` leyendo
  `Cargo.lock`. El unico sitio donde se construyen adaptadores es
  `crates/api/src/bootstrap.rs`.
- **Web**: una carpeta por capa en `apps/web/src` (`domain`, `application`,
  `infrastructure`, `ui`, `app`). Lo verifica `eslint-plugin-boundaries` con denegacion
  por defecto. `src/domain` no puede importar ni siquiera un paquete de npm. La raiz de
  composicion es `src/app/composition-root.ts`.
- **Android**: un modulo por capa, veinte en total. `core:domain`, `core:application` y
  `core:common` son `kotlin("jvm")` puros, asi que el SDK de Android no esta en su classpath.
  `feature:*` nunca depende de `data:*` ni de otra `feature:*`: las une `:app`, con un fichero
  de cableado por feature en `app/src/main/kotlin/com/bahoque/gastonomo/wiring/`. Lo vigilan
  dos cosas, y hacen falta las dos: las reglas de Konsist en
  `app/src/test/kotlin/com/bahoque/gastonomo/architecture/HexagonalArchitectureTest.kt`,
  que miran los imports, y la tarea `moduleGraphGuard`, que lee el grafo real de Gradle.
  Sin la segunda, declarar la dependencia prohibida en un `build.gradle.kts` y todavia no
  usarla pasaba limpio.

  Las 14 pantallas del rediseno estan implementadas. Ademas de las capas hay tres modulos de
  presentacion compartida y dos de utilidades de test: `:core:designsystem` (tokens,
  componentes y formato de dinero y fechas), `:core:navigation` (las rutas tipadas de las 14
  pantallas), `:core:testing` (dobles de los puertos, JVM puro) y `:core:screenshot` (captura
  de composables a PNG desde la JVM). El detalle esta en
  [apps/android/README.md](apps/android/README.md).

  Dos reglas que rompen el build y conviene saber antes de escribir una pantalla: **un color o
  un tamano no se escriben a mano fuera de `:core:designsystem`** (salen de
  `GastonomoTheme.colors`, `.typography`, `.radii`, `.dimens`, `.shadows`, `.formats`), y
  **un texto visible no se escribe en un composable**: va al `strings.xml` de su feature, o al
  de `:core:designsystem` si lo comparten varias, y se lee por `GastonomoStrings`. Espanol en
  `values`, ingles en `values-en`, las mismas claves en las dos salvo las `translatable="false"`.
  Las decisiones estan en el [ADR 0013](docs/adr/0013-sistema-de-diseno-en-android.md) y el
  [ADR 0014](docs/adr/0014-sesion-en-el-cliente-android.md).

Para anadir algo nuevo: dominio, puerto, caso de uso con un fake, adaptador, adaptador
driving y cableado en el composition root. En ese orden.

## Contrato compartido

El contrato completo, endpoint por endpoint y con el porque de cada decision, esta en
[docs/api-surface.md](docs/api-surface.md). La referencia ejecutable es
`GET /openapi.json` del servicio en marcha, y `GET /swagger-ui` para verlo. Esto es lo
minimo que hay que saber antes de tocar cualquiera de las tres apps.

### Salud

```
GET /health/live    200 siempre, sin tocar dependencias
GET /health/ready   200 si status=ok, 503 si status=degraded

{"status":"ok"|"degraded",
 "checks":[{"name":"database","status":"ok"|"error","latencyMs":0}],
 "version":"0.1.0"}
```

### El dominio, bajo `/v1`

```
POST   /v1/auth/register  login  refresh  logout
POST   /v1/auth/password/forgot  reset  change
GET    /v1/me

GET    /v1/movements?from=&to=&kind=&cursor=&limit=
POST   /v1/movements

GET    /v1/loans?direction=          GET /v1/loans/{id}
POST   /v1/loans                     POST /v1/loans/{id}/payments

GET    /v1/categories                POST /v1/categories
PATCH  /v1/categories/{id}           DELETE /v1/categories/{id}

GET    /v1/settings                  PATCH /v1/settings
GET    /v1/currencies                GET /v1/exchange-rates
```

Todo `/v1` exige `Authorization: Bearer {accessToken}` menos `auth/register`,
`auth/login`, `auth/refresh`, `auth/logout`, `auth/password/forgot` y
`auth/password/reset`.

### Las siete reglas que no se pueden romper

1. **El dinero es decimal exacto y viaja como string.** `NUMERIC(18,4)` en PostgreSQL,
   `rust_decimal::Decimal` en Rust, `"amount": "186400.0000"` en JSON. Nunca un float,
   en ninguna capa ni en ningun cliente. Ver [ADR 0009](docs/adr/0009-dinero-con-decimal.md).
2. **Cada registro lleva cuatro campos de importe**: `amount` y `currency` (lo que el
   usuario tecleo) mas `base` y `baseCurrency` (su equivalencia al registrar, con la
   tasa que el usuario pudo editar). La API los guarda tal cual y no recalcula nada.
   `base` y `baseCurrency` se pueden omitir los dos juntos, y entonces la equivalencia es
   el propio importe. Ver [ADR 0011](docs/adr/0011-multimoneda-de-cuatro-campos.md).
3. **Convertir para mostrar es del cliente.** La API sirve tasas, nunca importes
   convertidos. El estado de un prestamo, su porcentaje, los intereses, el orden de la
   lista y todos los totales y graficos los deriva el cliente.
4. **Toda lista va envuelta en `items`**; `/v1/movements` anade `nextCursor` al lado.
5. **Los errores son `application/problem+json`** (RFC 9457), incluidos los 404 y 405 de
   ruta desconocida. Los mensajes de `errors[].message` van **en castellano**, listos
   para pintar junto al campo. El `Display` de los errores de dominio se queda en ingles,
   que es para los logs.
6. **Un recurso de otra cuenta responde 404, nunca 403.** Un 403 confirmaria que existe.
   Lo mismo si el identificador del camino no es un UUID.
7. **Las fechas de registro son `DATE`** (`YYYY-MM-DD`), sin hora ni zona. Los instantes
   tecnicos si son `timestamptz`.

### Idempotencia

Los tres POST que crean dinero (`/v1/movements`, `/v1/loans`, `/v1/loans/{id}/payments`)
aceptan la cabecera `Idempotency-Key`. Es opcional, pero **los clientes deberian mandarla
siempre**: un reintento sin ella duplica el gasto, y no hay pantalla para borrarlo. El
reintento recibe la misma respuesta con `Idempotent-Replay: true`. La misma clave con
otro cuerpo es 409. Ver [ADR 0012](docs/adr/0012-idempotencia-con-cabecera.md).

### Tasas de cambio

Los clientes **no llaman a Frankfurter**. Solo llama la API, que guarda las pasadas en
PostgreSQL y las sirve en `/v1/exchange-rates` con base USD y un `status` de `ok` o
`stale`. Una tarea refresca a las 06:00 y 18:00 UTC, y al arrancar si hace falta. Ver
[ADR 0010](docs/adr/0010-cacheo-de-tasas-de-cambio.md).

### Configuracion

Por entorno, con el prefijo `GASTONOMO__` y `__` como separador de nivel. La lista
completa esta en [.env.example](.env.example). Las que hay que conocer:

```
GASTONOMO__SERVER__HOST  __PORT              8080 por defecto
GASTONOMO__DATABASE__URL
GASTONOMO__TELEMETRY__LOG_LEVEL
GASTONOMO__AUTH__ACCESS_TOKEN_SECRET        obligatorio de verdad al desplegar
GASTONOMO__MAIL__DRIVER                     log o smtp
GASTONOMO__EXCHANGE_RATES__ENABLED          false en tests y CI
```

La web lee `VITE_API_URL` (dev 5173, preview 4173). Android lee
`BuildConfig.API_BASE_URL` (debug `http://10.0.2.2:8080`).

## Limites de calidad

Se aplican a los tres lenguajes y rompen el build:

- Fichero de codigo: 300 lineas. De test: 400.
- Complejidad cognitiva 15. Ciclomatica 10 en TypeScript y Kotlin; en Rust **no** hay
  lint de ciclomatica y no se fuerza (clippy solo trae la cognitiva). Ver ADR 0005.
- Funcion: 50 lineas y 5 parametros. Anidamiento: 4 niveles.
- Prohibido: codigo muerto, imports sin usar, variables sin usar.
- Marcadores de trabajo pendiente solo con ticket: `TODO(GAS-123)`.
- Tests apagados solo con motivo escrito. Nada de `.only`.
- Cobertura: 80% en el nucleo, 60% global.

Los avisos son errores en todas partes: `-D warnings` en Rust, `--max-warnings 0` en
ESLint, `warningsAsErrors` en Android Lint, `maxIssues: 0` en detekt.

## Comandos que se usan de verdad

```bash
just                # lista todas las recetas
just check          # lint, tipos, tests y cobertura de los tres
just ci             # check + e2e, cargo deny y builds de release: lo que corre la CI
just api-test       # cargo test --workspace, sin base de datos
just api-test-db    # las 7 suites de integracion contra el Postgres de compose
just web-cov        # vitest con los umbrales de cobertura
just android-check  # detekt + sourceGuard + lint + tests (con capturas) + kover
just up / just down # pila de Docker de desarrollo
just guard          # tamano de fichero, tests apagados, TODO sin ticket
just root-lint      # ESLint sobre packages/ y tools/ (la web tiene el suyo)
```

Antes de abrir un PR: `just ci`. `just check` se queda corto a proposito, para que sea
rapido: no corre los e2e, ni `cargo deny`, ni los builds de release, y la CI si.

## Commits

Conventional commits, validados por commitlint en el hook `commit-msg`. Los scopes
permitidos estan en `commitlint.config.mjs`: `api`, `web`, `android`, `tools`, `ci`,
`docs`, `deps`, `repo`.

```
feat(api): anadir el endpoint de recetas
fix(web): corregir el timeout del gateway de salud
chore(deps): subir vitest a 5.0.1
```

Los hooks se saltan con `LEFTHOOK=0` y solo en emergencias.

## Lo que NO hay que hacer

- No metas logica de negocio en un handler, en un componente o en un ViewModel. Va en un
  caso de uso.
- No importes infraestructura desde el nucleo. No compila, o el linter lo rechaza.
- No uses `npm install` ni `yarn`: rompen el lockfile de pnpm.
- No bajes un umbral de cobertura para que pase la CI. Escribe el test.
- No anadas una baseline a detekt ni a Android Lint para esconder avisos.
- No pongas `#[allow(...)]`, `eslint-disable` ni `@Suppress` sin un comentario que diga
  por que.
- No toques `pnpm-workspace.yaml` a mano salvo que sepas exactamente que estas haciendo:
  pnpm lo reescribe.
- No escribas `unwrap()` ni `expect()` en codigo de produccion de la API: clippy los
  rechaza.

## Detalles del entorno que sorprenden

- **Java no esta en el PATH** en la maquina de desarrollo original. Los comandos de
  Android van por `tools/scripts/gradle.sh`, que busca un JDK (incluido el de Android
  Studio) antes de llamar al wrapper. Si tienes `java` en el PATH, `./gradlew` directo
  tambien vale.
- **El id del paquete del SDK de Android lleva la revision.** El platform que usa este
  proyecto se llama `platforms;android-37.0`, no `platforms;android-37`, y el directorio
  local tambien (`~/Android/Sdk/platforms/android-37.0`). Con el nombre corto sdkmanager no
  encuentra nada y falla despues de volcar las siete licencias completas, unas 900 lineas
  donde el error de verdad no se ve. Los ids reales estan en
  https://dl.google.com/android/repository/repository2-3.xml
- **Code Scanning puede devolver 403** en este repositorio por ser privado sin Advanced
  Security. El paso que sube el SARIF de detekt lleva `continue-on-error` por eso. No es una
  barrera de calidad: detekt ya rompe el build con `maxIssues: 0`.
- **PostgreSQL 18** guarda los datos en un subdirectorio por version. El volumen se monta
  en `/var/lib/postgresql`, no en `/var/lib/postgresql/data`, o el contenedor no arranca.
- **La web y la API son origenes distintos** y la API no manda cabeceras CORS. Por eso la
  pila de Docker entra por un gateway de nginx (`docker/gateway.dev.conf`) que las sirve
  bajo el mismo origen. Si arrancas la web fuera de Docker contra la API en el 8080, el
  navegador bloqueara las llamadas hasta que la API tenga CORS o Vite tenga un proxy.
- **El gateway necesita un `location` por prefijo de la API, y el orden importa.** Los
  bloques de `/health/`, `/openapi.json` y `/v1/` van ANTES de `location /`, que manda
  todo lo demas a la web. Cuando `/v1/` falto, las 24 rutas de la API acababan en el nginx
  de la SPA: los GET devolvian el `index.html` con 200 y los POST un 405 en HTML. Los jobs
  de `api.yml` y `web.yml` pasaban igual, porque cada uno prueba su imagen por separado.
  Por eso existe `just smoke` (y el job `stack` de la CI): levanta la pila entera y entra
  por la puerta. Si anades un prefijo nuevo a la API, anade su `location` y su
  comprobacion en `tools/scripts/smoke-stack.sh`.
- **El limite de peticiones lee la ULTIMA entrada de `X-Forwarded-For`**, que es la que
  anade nginx con `$proxy_add_x_forwarded_for`. Todo `location` que apunte a la API tiene
  que mandar esa cabecera o todas las peticiones compartiran el cupo del gateway. Y si
  algun dia la API se expone a internet sin proxy delante, el borde tiene que sobreescribir
  la cabecera: sin proxy, cualquiera se salta el limite mandandola a mano.
- **La imagen de desarrollo de la API** compila con `cargo watch`, y el directorio de
  compilacion esta excluido del watch. Si lo quitas, entra en bucle de recompilacion.
- **Los tests de PostgreSQL de la API** se saltan solos si no hay
  `GASTONOMO_TEST_DATABASE_URL`. Para correrlos: `just api-test-db`, que levanta la base y
  exporta tambien `GASTONOMO_TEST_REQUIRE_DATABASE=1`, que convierte el salto en fallo. No
  vacian la base entre tests: cada uno crea su propia cuenta con un correo unico, asi que
  el Postgres de desarrollo acumula filas de prueba en cada pasada.
- **Hay dos pilas de Docker y no hay que mezclarlas.** La de verdad es `compose.yaml` de la
  raiz, que es la que usan todas las recetas de `just`. `apps/api/docker-compose.yml` es un
  atajo para trabajar dentro de `apps/api` sin la web, y publica otros puertos a proposito
  (5433, 8081, 8026 y 1026) para que levantar las dos no rompa nada. Cuando las dos usaban
  el 5432, levantar la segunda dejaba un contenedor muerto y los tests apuntando a la base
  equivocada.
- **La tarea de tasas sale a internet al arrancar.** En tests y en CI va apagada con
  `GASTONOMO__EXCHANGE_RATES__ENABLED=false`. Si la dejas encendida en un entorno sin
  salida, la aplicacion arranca igual pero `/v1/exchange-rates` responde 503 y el catalogo
  de monedas sale vacio.
- **Los tests instrumentados de Android** necesitan emulador. `just android-connected`.

## CI

`.github/workflows/ci.yml` es el unico punto de entrada. Mira que ha cambiado con
`dorny/paths-filter` y llama a `quality.yml`, `api.yml`, `web.yml` y `android.yml` como
workflows reutilizables. El job `all-green` es el unico check que hay que marcar como
required en la proteccion de rama.

## Despliegue

La web y la API viven en `apps-01`, una instancia de Lightsail compartida con Grimdex, en
`https://gastonomo.bahoque.com` y `https://api.gastonomo.bahoque.com`. Delante hay un Caddy
que es el unico contenedor que publica puertos. El detalle completo esta en
[docs/despliegue.md](docs/despliegue.md) y el porque en el
[ADR 0015](docs/adr/0015-despliegue-en-instancia-compartida.md).

Lo minimo que hay que saber antes de tocar algo de esto:

- Un push verde a `main` dispara `release.yml`, que publica las dos imagenes en GHCR, y
  `deploy.yml`, que las pone en la maquina. No hay despliegue manual en el camino normal.
- **Las imagenes se construyen en CI, nunca en el servidor.** La maquina no tiene toolchain.
- **La etiqueta que se despliega es el SHA del commit, nunca `latest` ni `main`.** Con una
  etiqueta movil no se puede decir que esta corriendo y el rollback deja de existir.
- **Lo que es del servidor esta en otro repositorio**, `infra-apps-01`: Caddy y su tabla de
  enrutado, la red `edge`, el aprovisionamiento y las copias de seguridad. Aqui se queda lo
  de esta aplicacion: los dos Dockerfile, `compose.prod.yaml` y los tres workflows de
  release, despliegue y healthcheck.
- **Nada en `compose.prod.yaml` publica puertos** y la base de datos no entra en la red
  `edge`. Se entra siempre por Caddy.
- Volver atras es `./rollback.sh` en `/srv/apps/gastonomo`, y no toca la base de datos: las
  migraciones son solo hacia delante.
- Android apunta a `https://api.gastonomo.bahoque.com` en release y sigue en
  `http://10.0.2.2:8080` en debug. Cada buildType tiene su `network_security_config.xml`, y
  el texto claro solo esta permitido en debug y solo para los hosts locales.
