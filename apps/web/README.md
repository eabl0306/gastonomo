# Web de Gastonomo

React 19 sobre Vite, en TypeScript, con arquitectura hexagonal de verdad: las capas
no son solo carpetas, el linter rompe el build si una capa importa lo que no debe.

## Arrancar

```bash
pnpm install                          # desde la raiz del repo
pnpm --filter @gastonomo/web dev      # http://localhost:5173
```

La URL de la API se lee de `VITE_API_URL`. Vacia significa **el mismo origen**, que es como
se sirve desplegado: Caddy pone la API bajo el dominio de la web. En desarrollo lo consigue
el proxy de Vite, que manda `/v1`, `/health` y `/openapi.json` a `VITE_DEV_API_TARGET`
(por defecto `http://127.0.0.1:8080`).

```bash
VITE_DEV_API_TARGET=http://127.0.0.1:8090 pnpm --filter @gastonomo/web dev
```

## Capas

```
src/domain/          Modelos y reglas puras. No importa nada: ni React, ni fetch, ni zod.
src/application/     Puertos (interfaces) y casos de uso. Solo depende de domain.
src/infrastructure/  Adaptadores: HTTP con zod, y uno en memoria para tests y demos.
src/ui/              Componentes y hooks. Reciben el puerto por contexto, nunca el adaptador.
src/app/             Raiz de composicion y arranque. El unico sitio donde ui e infra se ven.
```

La direccion de dependencias esta escrita una sola vez, en `eslint.config.js`, y la
fuerza `eslint-plugin-boundaries`. Si `src/domain` importa de `src/infrastructure`,
el lint falla con un mensaje que lo dice tal cual. Los ficheros de test son una
categoria aparte: pueden montar cualquier adaptador, porque ese es su trabajo.

Para anadir algo nuevo: modelo en `domain`, puerto y caso de uso en `application`, adaptador
en `infrastructure`, pantalla en `ui` y cableado en `src/app/composition-root.ts`. En ese
orden, y sin tocar nada mas.

## Pantallas

Entrar, crear cuenta, recuperar y restablecer la contrasena; Inicio, Movimientos, Prestamos
y su detalle; los formularios de movimiento, prestamo y abono; Ajustes, Categorias y
Exportar; y el dictado. `/estado` es una pantalla de diagnostico que se queda fuera del
producto: no lleva armazon ni pide sesion.

Tres cosas que no son evidentes al leer el codigo:

- **El periodo lo comparten Inicio y Movimientos**, y por eso vive en un contexto por encima
  de las dos. Los prestamos **no** dependen de el: son saldos de hoy.
- **Los ajustes, el catalogo de monedas y las tasas se cargan una vez** por sesion, en
  `AccountProvider`. De ahi salen la moneda con la que se convierte todo y el idioma con el
  que se formatea, asi que dos pantallas con dos copias distintas no cuadrarian entre si.
- **El dictado es una ruta y no una hoja**, porque lo abren tres pantallas. Se navega con la
  pantalla actual en el estado para poder pintarla de fondo, y el modal lo pinta el armazon,
  que es quien esta dentro de los proveedores.

## Dinero

Todo importe es decimal exacto sobre `BigInt` (`src/domain/money/decimal.ts`) y viaja como
cadena. Nunca pasa por coma flotante, ni siquiera para formatear: a `Intl.NumberFormat` se le
pasa la cadena, que la entiende desde ES2023.

Cada registro lleva cuatro campos: lo que se teclo y su equivalencia en la moneda principal.
**La equivalencia se guarda tal como quede en el formulario**, con la tasa que el usuario pudo
editar, y la API no recalcula nada. Convertir para mostrar es del cliente
(`src/domain/money/displayed-amount.ts`).

Los tres POST que crean dinero mandan `Idempotency-Key`. La clave se genera **una vez por
formulario y no se regenera al reintentar**: el historico no se puede borrar, asi que un
reintento con clave nueva dejaria el gasto dos veces.

## Formato

`src/ui/format/` es el espejo de `:core:designsystem/format` de Android: mismos nombres de
mes y dia, mismos simbolos de moneda y las mismas tres funciones de signo. Los nombres de
fecha van en tablas propias y no salen de `Intl.DateTimeFormat`, que en castellano los
devuelve con punto ("sept.") y en ingles cambia el orden.

Ningun componente formatea por su cuenta: se lee del contexto con `useFormats()`.

## Comandos

| Comando                                      | Que hace                                          |
| -------------------------------------------- | ------------------------------------------------- |
| `pnpm --filter @gastonomo/web dev`           | Servidor de desarrollo en el 5173                 |
| `pnpm --filter @gastonomo/web build`         | Typecheck y build de produccion                   |
| `pnpm --filter @gastonomo/web preview`       | Sirve `dist` en el 4173                           |
| `pnpm --filter @gastonomo/web lint`          | ESLint: tipos, complejidad, tamano y arquitectura |
| `pnpm --filter @gastonomo/web typecheck`     | Solo TypeScript                                   |
| `pnpm --filter @gastonomo/web test`          | Vitest en modo watch                              |
| `pnpm --filter @gastonomo/web test:coverage` | Cobertura con umbrales que rompen el build        |
| `pnpm --filter @gastonomo/web e2e`           | Playwright (levanta el preview solo)              |
| `pnpm --filter @gastonomo/web knip`          | Codigo y dependencias muertas                     |

## Tests

- Unitarios de dominio y casos de uso, sin React.
- De componente con Testing Library y el adaptador en memoria.
- De adaptador HTTP con un `fetch` falso, incluido el 503 que la API usa para decir
  que esta degradada.
- E2E con Playwright. La red se intercepta con `page.route`, asi que no hace falta
  tener la API levantada. Para probar contra la API real:

```bash
E2E_USE_REAL_API=1 pnpm --filter @gastonomo/web e2e
```

Umbrales de cobertura: 60% global, 80% en `src/domain` y `src/application`. Por debajo,
el comando falla.

La primera vez hay que bajar el navegador: `pnpm --filter @gastonomo/web e2e:install`.

## Docker

El contexto de build es la raiz del repo, porque el lockfile de pnpm vive alli.

```bash
docker build -f apps/web/Dockerfile -t gastonomo-web .
docker run --rm -p 8080:8080 gastonomo-web        # nginx, usuario sin privilegios
docker build -f apps/web/Dockerfile.dev -t gastonomo-web-dev .   # servidor de desarrollo
```

La imagen de produccion sirve con nginx en el puerto 8080, cachea los assets con hash
para siempre y nunca cachea `index.html`. Responde a `/healthz`.
