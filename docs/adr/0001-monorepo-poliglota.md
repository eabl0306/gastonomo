# ADR 0001: Estructura de monorepo poliglota

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

Gastonomo necesita tres piezas que evolucionan juntas: una API en Rust, una web en React y
una app nativa de Android. Las tres comparten el mismo contrato HTTP y el mismo modelo
mental del dominio. Cada una trae su propio gestor de dependencias (cargo, pnpm, gradle),
y ninguno sabe nada de los otros.

Las opciones eran tres repositorios separados, un monorepo con un orquestador unico
(Bazel, Nx, Turborepo) o un monorepo sin orquestador donde cada app conserva su toolchain.

## Decision

Un solo repositorio git con esta forma:

```
apps/api        workspace de Cargo con cinco crates
apps/web        paquete de pnpm dentro del workspace JS
apps/android    build de Gradle con build incluido y seis modulos
packages/       configuracion compartida de JS (tsconfig, eslint-config)
tools/checks    tres scripts de Node que valen para los tres lenguajes
tools/scripts   utilidades de shell que usan el justfile y los hooks
docs/           arquitectura y estas decisiones
```

Cada app mantiene su toolchain nativo. No hay orquestador que lo envuelva todo. El
pegamento es un `justfile` con recetas finas que delegan, mas los hooks de git y la CI.

Se descarta Bazel y companía: para tres apps que ya tienen builds incrementales y cache
propios, el coste de aprender y mantener un orquestador es mayor que lo que ahorra.

## Consecuencias

A favor:

- Un cambio que toca el contrato HTTP se ve en un solo commit y en un solo PR.
- Cada desarrollador sigue usando los comandos de su ecosistema. `cargo test` funciona
  dentro de `apps/api` sin saber que existe un monorepo.
- Las reglas de calidad comunes viven en un sitio y se aplican a los tres.

En contra:

- La CI tiene que filtrar por rutas para no compilar Android cuando solo cambia el CSS.
  Se resuelve con `dorny/paths-filter` en `.github/workflows/ci.yml`.
- Hay tres ficheros de bloqueo (`Cargo.lock`, `pnpm-lock.yaml`, `gradle/libs.versions.toml`)
  y tres flujos de Dependabot.
- Quien clona se baja todo, aunque solo vaya a tocar la web.
