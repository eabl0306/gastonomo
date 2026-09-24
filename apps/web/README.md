# Web de Gastonomo

React 19 sobre Vite, en TypeScript, con arquitectura hexagonal de verdad: las capas
no son solo carpetas, el linter rompe el build si una capa importa lo que no debe.

## Arrancar

```bash
pnpm install                          # desde la raiz del repo
pnpm --filter @gastonomo/web dev      # http://localhost:5173
```

La URL de la API se lee de `VITE_API_URL` (por defecto `http://localhost:8080`).

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

El caso de uso implementado es uno solo, `checkApiHealth`, de punta a punta. Sirve de
plantilla: cuando llegue el dominio real, se anaden modelos en `domain`, puertos y casos
de uso en `application`, y adaptadores en `infrastructure`, sin tocar nada mas.

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
