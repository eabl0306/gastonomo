# Gastonomo

Monorepo con tres aplicaciones que comparten el mismo contrato: una API en Rust, una web
en React y una app nativa de Android.

Ahora mismo solo esta implementado un caso de uso, comprobar la salud del servicio, de
punta a punta en las tres. No hay dominio de negocio todavia: el esqueleto esta montado y
los huecos estan a la vista.

## Arrancar en cinco minutos

Necesitas: Node 24, pnpm 11.18, Rust estable, Docker, [just](https://just.systems) y un
JDK 21 o superior si vas a tocar Android (vale el que trae Android Studio).

```bash
cp .env.example .env
just setup     # instala dependencias y engancha los hooks de git
just up        # levanta base de datos, API, web y el gateway en Docker
```

Cuando los contenedores esten sanos:

- http://localhost:8090 la aplicacion entera, web y API bajo el mismo origen
- http://localhost:8090/health/ready el estado del servicio en JSON
- http://localhost:5173 la web sola, con recarga en caliente
- http://localhost:8080 la API sola

Para tumbarlo todo, `just down`.

Si prefieres trabajar sin Docker: `just db-up` levanta solo Postgres, `just api-run`
arranca la API y `just web-dev` el servidor de desarrollo de la web.

## Que hay dentro

```
apps/api         API en Rust (Salvo + SQLx). Cinco crates, uno por capa.
apps/web         Web en React 19 sobre Vite. Cinco carpetas, una por capa.
apps/android     App nativa en Kotlin y Compose. Seis modulos de Gradle.
packages/        Configuracion compartida de JS: tsconfig y eslint-config.
tools/checks     Reglas de calidad comunes a los tres lenguajes (Node puro).
tools/scripts    Utilidades de shell que usan el justfile y los hooks.
docker/          Configuracion del gateway de nginx.
docs/            Arquitectura y decisiones (ADR).
```

## Comandos

Todo pasa por `just`. Escribe `just` a secas para ver la lista completa.

| Comando          | Que hace                                                         |
| ---------------- | ---------------------------------------------------------------- |
| `just setup`     | Instala dependencias, engancha los hooks y revisa los toolchains |
| `just check`     | Lint, tipos, tests y cobertura de los tres                       |
| `just ci`        | `check` mas lo que la CI corre aparte: e2e, auditoria y builds   |
| `just fmt`       | Formatea los tres lenguajes                                      |
| `just lint`      | Lint de los tres                                                 |
| `just test`      | Tests unitarios de los tres                                      |
| `just cov`       | Cobertura de los tres, con sus umbrales                          |
| `just e2e`       | Playwright en la web y tests de integracion de la API            |
| `just e2e-stack` | E2E contra la pila de Docker entera, sin mockear la red          |
| `just build`     | Build de produccion de los tres                                  |
| `just guard`     | Tamano de fichero, tests apagados y TODO sin ticket              |
| `just root-lint` | ESLint sobre `packages/` y `tools/`                              |
| `just up`        | Levanta la pila de desarrollo en Docker                          |
| `just down`      | La tumba y borra los volumenes                                   |
| `just logs`      | Sigue los logs. `just logs api` para uno solo                    |
| `just db-up`     | Levanta solo PostgreSQL                                          |
| `just psql`      | Abre una consola de PostgreSQL                                   |
| `just migrate`   | Aplica las migraciones a mano                                    |

Hay recetas por app con los prefijos `api-`, `web-` y `android-`. Por ejemplo
`just api-test`, `just web-lint` o `just android-check`.

## Como se fuerza la calidad

Las reglas no estan solo escritas: rompen el build.

- Ningun fichero de codigo pasa de 300 lineas, ni de 400 si es de test.
- Complejidad cognitiva hasta 15, funciones de hasta 50 lineas, hasta 5 parametros y 4
  niveles de anidamiento.
- Complejidad ciclomatica hasta 10 en TypeScript y Kotlin. En Rust no se fuerza: clippy
  no tiene lint de ciclomatica. El detalle esta en el
  [ADR 0005](docs/adr/0005-enforcement-automatizado.md).
- Nada de codigo muerto, imports sin usar, TODO sin ticket ni tests apagados sin motivo.
- Cobertura minima del 80% en el nucleo de cada app y del 60% en el total.
- El dominio no puede depender de infraestructura, y hay un test que lo comprueba.

Se aplica en tres sitios: `just check` en local, los hooks de git en cada commit y
GitHub Actions en cada push. El check requerido se llama `all-green`.

`just check` no es toda la CI: los e2e de Playwright, la auditoria de dependencias y los
builds de release van en `just ci`. Eso es lo que hay que correr antes de abrir un PR si
no quieres sorpresas.

Los hooks pueden saltarse con `LEFTHOOK=0 git commit ...` cuando de verdad hace falta.
La CI no se puede saltar.

## Documentacion

- [docs/architecture.md](docs/architecture.md): la arquitectura hexagonal y como se
  aplica en las tres apps.
- [docs/adr/](docs/adr/): por que se decidio cada cosa.
- [CLAUDE.md](CLAUDE.md): convenciones del repositorio, para humanos y para agentes.
- Cada app tiene su propio README con el detalle: [apps/api](apps/api/README.md),
  [apps/web](apps/web/README.md), [apps/android](apps/android/README.md).
