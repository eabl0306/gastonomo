# ADR 0003: Rust con Salvo para la API

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

La API tiene que ser rapida de arrancar, barata de desplegar y dificil de romper en
tiempo de ejecucion. Rust da tipos fuertes, ausencia de nulls y un binario unico sin
runtime que instalar en la imagen. Queda elegir el framework HTTP.

Salvo era requisito del proyecto. Aun asi conviene dejar escrito que aporta y que no.

## Decision

Salvo 0.96 sobre Tokio, con esta estructura:

- `crates/http` contiene los handlers, los DTO, el router y el middleware. Es un
  adaptador driving y es el unico crate que conoce Salvo.
- Los handlers son structs con dependencias inyectadas, no funciones libres. Reciben los
  casos de uso ya construidos.
- El documento OpenAPI se genera con los builders de utoipa 5.5 y se sirve en
  `/openapi.json`. No se usa `utoipa-swagger-ui`, que solo trae adaptadores para actix,
  axum y rocket. `/swagger-ui` sirve una pagina minima que carga los assets de un CDN.
- Edition 2024 y toolchain fijado en `rust-toolchain.toml`.

Se descarta anotar los handlers con `#[utoipa::path]` porque esa macro espera funciones
libres y aqui los handlers son structs con estado inyectado.

## Consecuencias

A favor:

- Imagen de produccion de 152 MB sobre `debian:trixie-slim`, con el binario de 7,3 MB,
  usuario no root (uid 10001) y HEALTHCHECK propio.
- Apagado limpio: SIGTERM y SIGINT llaman a `stop_graceful` con 10 segundos de margen.
- El framework esta confinado a un crate. Sustituirlo no toca el nucleo.

En contra:

- Salvo tiene menos ejemplos y menos integraciones de terceros que axum. Eso ya se noto
  con utoipa-swagger-ui.
- El documento OpenAPI se escribe a mano. Si alguien anade una ruta y olvida
  documentarla, nada lo detecta todavia. Un test que compare las rutas del router con las
  del documento cerraria el agujero.
- Sin salida a internet, `/swagger-ui` responde 200 pero no pinta la interfaz.
