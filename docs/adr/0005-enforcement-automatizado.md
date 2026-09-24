# ADR 0005: El enforcement de calidad es automatico y bloqueante

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

Las reglas de estilo que solo viven en un documento se incumplen. Las que solo comprueba
un reviewer humano se incumplen mas despacio. La unica regla que se cumple siempre es la
que impide compilar, commitear o mergear.

El proyecto fija estos limites: ficheros de menos de 300 lineas (400 en tests),
complejidad ciclomatica menor o igual a 10, cognitiva menor o igual a 15, funciones de
hasta 50 lineas, hasta 5 parametros, anidamiento de hasta 4 niveles, nada de codigo
muerto ni imports sin usar, nada de TODO sin ticket, nada de tests apagados sin motivo.

## Decision

Cada limite se traduce a la herramienta nativa de cada lenguaje, y lo que ninguna cubre
se cubre con tres scripts de Node compartidos.

| Regla                   | Rust                           | TypeScript                     | Kotlin                                     |
| ----------------------- | ------------------------------ | ------------------------------ | ------------------------------------------ |
| Formato                 | `cargo fmt --check`            | prettier                       | detekt-formatting (ktlint)                 |
| Complejidad ciclomatica | no se fuerza (ver abajo)       | `complexity`                   | `CyclomaticComplexMethod`                  |
| Complejidad cognitiva   | `clippy::cognitive_complexity` | `sonarjs/cognitive-complexity` | `CognitiveComplexMethod`                   |
| Funcion de 50 lineas    | `clippy::too_many_lines`       | `max-lines-per-function`       | `LongMethod`                               |
| 5 parametros            | `clippy::too_many_arguments`   | `max-params`                   | `LongParameterList`                        |
| Anidamiento             | `excessive-nesting-threshold`  | `max-depth`                    | `NestedBlockDepth`                         |
| Codigo muerto           | `-D warnings`                  | `no-unused-vars` y knip        | `UnusedPrivateMember`                      |
| Capas hexagonales       | test sobre `Cargo.lock`        | `eslint-plugin-boundaries`     | Konsist, `moduleGraphGuard` y el classpath |
| `@Suppress` sin motivo  | no aplica                      | no aplica                      | tarea `sourceGuard`                        |

Lo comun a los tres esta en `tools/checks`: tamano de fichero, tests enfocados o saltados
sin motivo, y marcadores de trabajo pendiente sin ticket con formato `TODO(GAS-123)`.

ESLint de la raiz (`eslint.config.js`) aplica la capa de calidad a `packages/` y `tools/`,
que son JavaScript puro. La web tiene la suya con las reglas type-aware, de React y de
capas hexagonales.

### La ciclomatica no se fuerza en Rust

clippy no tiene lint de complejidad ciclomatica: el unico que existe es
`clippy::cognitive_complexity`, que mide la cognitiva. Se ha comprobado que una funcion
con ciclomatica 32 (un `match` plano de 31 ramas) pasa `cargo clippy -- -D warnings`
limpia.

Lo que si la acota en la practica es la combinacion de `too_many_lines` (50),
`too_many_arguments` (5), `cognitive_complexity` (15) y `excessive-nesting-threshold` (4):
un `match` gigante se cuela, pero casi nada mas. Forzarla de verdad pediria una
herramienta externa (rust-code-analysis o lizard) y meter otro toolchain en el repo, asi
que de momento se asume el hueco y se documenta en vez de afirmar lo contrario.

Se aplica en tres momentos:

1. En local, `just check` corre el mismo enforcement que la CI salvo lo caro: los e2e,
   `cargo deny`, los builds de release y los tests de integracion contra PostgreSQL van
   en `just ci`. Antes de abrir un PR se corre `just ci`.
2. En cada commit, lefthook bloquea el commit: formato con autofix, ESLint sobre lo
   staged, `cargo fmt`, detekt si hay Kotlin, los tres scripts comunes y un escaner de
   secretos y ficheros enormes. `commit-msg` valida el mensaje con commitlint.
3. En cada push y cada PR, GitHub Actions repite todo y deja un unico check requerido,
   `all-green`.

Los avisos son errores: `-D warnings` en Rust, `--max-warnings 0` en ESLint,
`warningsAsErrors` en Android Lint y `maxIssues: 0` en detekt.

## Consecuencias

A favor:

- El codigo que llega a main cumple, sin depender de la memoria de nadie.
- El reviewer habla de diseno, no de estilo.
- Las reglas estan escritas una vez y ejecutadas en tres sitios (local, hook, CI).

En contra:

- Los hooks alargan el commit. Por eso pre-commit solo mira lo staged y lo caro vive en
  pre-push, que se puede saltar con `LEFTHOOK=0` cuando de verdad hace falta.
- Un toolchain estricto puede romper la CI sin que nadie toque el codigo: clippy estrena
  lints en cada version estable de Rust. Se acepta a cambio de no tener que actualizar el
  fichero de toolchain a mano.
- Las reglas de tamano se miden en lineas fisicas en los scripts comunes y saltando
  blancos y comentarios en ESLint. El script es el techo comun.
