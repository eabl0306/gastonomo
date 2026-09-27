# Gastonomo

Monorepo con tres aplicaciones que comparten el mismo contrato: una API en Rust, una web
en React y una app nativa de Android.

La API ya tiene el dominio completo: cuentas con contrasena, movimientos, prestamos con
abonos, categorias, ajustes y tasas de cambio multimoneda. Son 24 endpoints bajo `/v1` mas
los de salud, y estan descritos uno por uno en
[docs/api-surface.md](docs/api-surface.md).

La app de Android ya implementa el rediseno completo: las 14 pantallas, multimoneda, sesion con
refresco de tokens y cerrojo local con PIN o huella. La web todavia solo tiene el caso de uso de
salud.

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

## Probar la API

Con la pila arriba, el contrato entero esta en http://localhost:8090/openapi.json y se
puede ojear en http://localhost:8090/swagger-ui. Un recorrido minimo con `curl`:

```bash
# Registro: devuelve el par de tokens y siembra las 12 categorias por defecto
curl -s -X POST http://localhost:8090/v1/auth/register \
  -H 'content-type: application/json' \
  -d '{"name":"Camilo","email":"camilo@correo.com",
       "password":"contrasena-larga","acceptedTerms":true}'

TOKEN=...   # el accessToken de la respuesta

# Las categorias sembradas, con su conteo de uso
curl -s http://localhost:8090/v1/categories -H "authorization: Bearer $TOKEN"

# Un gasto. Los importes van como string, y base/baseCurrency se pueden omitir
# si el importe ya esta en la moneda principal. Idempotency-Key evita que un
# reintento lo guarde dos veces.
curl -s -X POST http://localhost:8090/v1/movements \
  -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -H 'Idempotency-Key: mi-primer-gasto' \
  -d '{"occurredOn":"2026-09-15","kind":"out","categoryId":"<id>",
       "note":"Mercado","amount":"186400","currency":"COP"}'

# Las tasas que la API cachea de Frankfurter, con base USD
curl -s http://localhost:8090/v1/exchange-rates -H "authorization: Bearer $TOKEN"
```

Cosas que conviene saber antes de escribir un cliente:

- Los importes viajan **como string** y nunca como number. La API no devuelve importes
  convertidos: sirve los cuatro campos del registro y las tasas, y el cliente convierte.
- Los errores son `application/problem+json`, y `errors[].message` viene en castellano
  listo para pintar junto al campo.
- Un recurso de otra cuenta responde 404, nunca 403.

Los tests de integracion contra un PostgreSQL de verdad se corren con `just api-test-db`,
que levanta la base y falla si no la encuentra en vez de saltarse los tests.

## Que hay dentro

```
apps/api         API en Rust (Salvo + SQLx). Cinco crates, uno por capa.
apps/web         Web en React 19 sobre Vite. Cinco carpetas, una por capa.
apps/android     App nativa en Kotlin y Compose. Veinte modulos de Gradle.
packages/        Configuracion compartida de JS: tsconfig y eslint-config.
tools/checks     Reglas de calidad comunes a los tres lenguajes (Node puro).
tools/scripts    Utilidades de shell que usan el justfile y los hooks.
docker/          Configuracion del gateway de nginx.
docs/            Arquitectura, contrato de la API y decisiones (ADR).
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
| `just smoke`     | Comprueba que por el gateway se llega a la API y a la web        |
| `just down`      | La tumba y borra los volumenes                                   |
| `just logs`      | Sigue los logs. `just logs api` para uno solo                    |
| `just db-up`     | Levanta solo PostgreSQL                                          |
| `just psql`      | Abre una consola de PostgreSQL                                   |
| `just migrate`   | Aplica las migraciones a mano                                    |

Y dos de la API que se usan mucho: `just api-check` corre todo su enforcement y
`just api-test-db` los tests de integracion contra PostgreSQL.

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

## Donde vive

La web esta en https://gastonomo.bahoque.com y la API en
https://api.gastonomo.bahoque.com. Corren en `apps-01`, una instancia de Lightsail
compartida con otra aplicacion, detras de un Caddy que reparte por dominio y se ocupa de
los certificados.

Un push verde a `main` construye las dos imagenes en GitHub Actions, las publica en GHCR
etiquetadas con el SHA del commit y las despliega. Nada se construye en el servidor, y
volver atras es un comando.

La app de Android no se despliega: se compila. El build de release apunta a la API de
produccion y el de debug al emulador.

El detalle esta en [docs/despliegue.md](docs/despliegue.md). Lo que es del servidor, Caddy
incluido, vive en su propio repositorio de infraestructura.

## Documentacion

- [docs/architecture.md](docs/architecture.md): la arquitectura hexagonal y como se
  aplica en las tres apps.
- [docs/api-surface.md](docs/api-surface.md): el contrato de la API, endpoint por
  endpoint, y por que cada cosa es como es.
- [docs/despliegue.md](docs/despliegue.md): como se despliega, que hay en la
  instancia y que hacer cuando falla.
- [docs/adr/](docs/adr/): por que se decidio cada cosa.
- [CLAUDE.md](CLAUDE.md): convenciones del repositorio, para humanos y para agentes.
- Cada app tiene su propio README con el detalle: [apps/api](apps/api/README.md),
  [apps/web](apps/web/README.md), [apps/android](apps/android/README.md).
