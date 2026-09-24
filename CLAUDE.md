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
- **Android**: un modulo por capa. `core:domain`, `core:application` y `core:common` son
  `kotlin("jvm")` puros, asi que el SDK de Android no esta en su classpath. `feature:*`
  nunca depende de `data:*`. Lo vigilan dos cosas, y hacen falta las dos: ocho reglas de
  Konsist en
  `app/src/test/kotlin/com/bahoque/gastonomo/architecture/HexagonalArchitectureTest.kt`,
  que miran los imports, y la tarea `moduleGraphGuard`, que lee el grafo real de Gradle.
  Sin la segunda, declarar la dependencia prohibida en un `build.gradle.kts` y todavia no
  usarla pasaba limpio.

Para anadir algo nuevo: dominio, puerto, caso de uso con un fake, adaptador, adaptador
driving y cableado en el composition root. En ese orden.

## Contrato compartido

```
GET /health/live    200 siempre, sin tocar dependencias
GET /health/ready   200 si status=ok, 503 si status=degraded

{"status":"ok"|"degraded",
 "checks":[{"name":"database","status":"ok"|"error","latencyMs":0}],
 "version":"0.1.0"}
```

Configuracion de la API por entorno, con `__` como separador de nivel:
`GASTONOMO__SERVER__HOST`, `GASTONOMO__SERVER__PORT`, `GASTONOMO__DATABASE__URL`,
`GASTONOMO__TELEMETRY__LOG_LEVEL`. Puerto 8080 por defecto.

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
just api-test       # cargo test --workspace
just web-cov        # vitest con los umbrales de cobertura
just android-check  # detekt + sourceGuard + lint + tests + kover
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
- **PostgreSQL 18** guarda los datos en un subdirectorio por version. El volumen se monta
  en `/var/lib/postgresql`, no en `/var/lib/postgresql/data`, o el contenedor no arranca.
- **La web y la API son origenes distintos** y la API no manda cabeceras CORS. Por eso la
  pila de Docker entra por un gateway de nginx (`docker/gateway.dev.conf`) que las sirve
  bajo el mismo origen. Si arrancas la web fuera de Docker contra la API en el 8080, el
  navegador bloqueara las llamadas hasta que la API tenga CORS o Vite tenga un proxy.
- **La imagen de desarrollo de la API** compila con `cargo watch`, y el directorio de
  compilacion esta excluido del watch. Si lo quitas, entra en bucle de recompilacion.
- **Los tests de PostgreSQL de la API** se saltan solos si no hay
  `GASTONOMO_TEST_DATABASE_URL`. Para correrlos: `just api-test-db`.
- **Los tests instrumentados de Android** necesitan emulador. `just android-connected`.

## CI

`.github/workflows/ci.yml` es el unico punto de entrada. Mira que ha cambiado con
`dorny/paths-filter` y llama a `quality.yml`, `api.yml`, `web.yml` y `android.yml` como
workflows reutilizables. El job `all-green` es el unico check que hay que marcar como
required en la proteccion de rama.
