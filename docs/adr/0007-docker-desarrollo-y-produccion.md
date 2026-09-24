# ADR 0007: Docker para desarrollo y para produccion

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

Hacen falta dos cosas distintas y conviene no confundirlas. En desarrollo se quiere
arrancar la pila entera con un comando, con recarga en caliente y con los puertos a mano.
En produccion se quiere una imagen pequena, sin toolchain dentro, sin codigo montado, que
no corra como root y que sepa decir si esta sana.

La app de Android queda fuera: se compila con Gradle, no se dockeriza.

## Decision

Dos ficheros de compose en la raiz, con dos Dockerfile por app.

`compose.yaml` (desarrollo):

- `db`: `postgres:18-alpine` con healthcheck `pg_isready` y volumen con nombre. El
  volumen va montado en `/var/lib/postgresql`, que es lo que espera la imagen 18.
- `api`: `apps/api/Dockerfile.dev`, con `cargo watch` y el codigo montado. El directorio
  de compilacion vive en un volumen y esta excluido del watch, o cada escritura de cargo
  dispara otra recompilacion.
- `web`: `apps/web/Dockerfile.dev`, servidor de Vite en 0.0.0.0:5173 con HMR. Solo se
  monta `src` y el `index.html`: `node_modules` se queda el del contenedor.
- `gateway`: nginx con `docker/gateway.dev.conf` en el 8090. Sirve la web y reenvia
  `/health/`, `/openapi.json` y `/swagger-ui` a la API, asi que las dos quedan bajo el
  mismo origen y el navegador no bloquea las llamadas. La API no manda cabeceras CORS: sin
  este servicio habria que anadirselas o poner un proxy en Vite. Es la puerta de entrada
  recomendada; los puertos sueltos de la API y de la web siguen abiertos para depurar.
- Todos los puertos y credenciales salen de `.env` con valores por defecto.

`compose.prod.yaml` (produccion):

- Imagenes de produccion, sin volumenes de codigo.
- La base de datos no publica puertos al host: solo se llega por la red interna.
- `restart: unless-stopped`, healthchecks y limites de CPU y memoria por servicio.
- `VITE_API_URL` se pasa como argumento de build, porque se compila dentro del bundle.
- El `gateway` de nginx (`docker/gateway.prod.conf`) es el UNICO servicio con puerto
  publicado. La web y la API no publican ninguno: solo se llega a ellas por la red
  interna de compose.

Detalles de las imagenes de produccion:

- API: multi-stage con `cargo-chef` sobre `debian:trixie-slim`. Usuario no root
  (uid 10001) y HEALTHCHECK contra `/health/live`.
- Web: build con pnpm y nginx alpine sirviendo en el 8080 como usuario `nginx` (uid 101),
  con fallback de SPA, `no-store` para `index.html` e `immutable` para los assets.
  El contexto de build es la raiz del repo, porque el lockfile de pnpm vive alli.

## Consecuencias

A favor:

- `just up` levanta base de datos, API, web y gateway. `just down` lo borra todo.
- La imagen de produccion no lleva ni cargo ni node: menos superficie y menos peso.
- Los healthchecks hacen que `depends_on: service_healthy` funcione de verdad.

En contra:

- La primera compilacion de la imagen de desarrollo de la API tarda varios minutos:
  instala `cargo-watch` y compila todas las dependencias. Despues queda en cache.
- El bind mount del codigo en Linux es rapido, pero en macOS y Windows es lento. Quien
  desarrolle ahi seguramente prefiera `cargo run` en local contra `just db-up`.
- `VITE_API_URL` se congela en tiempo de build. Cambiar de entorno obliga a reconstruir
  la imagen de la web o a servir la configuracion de otra forma.
