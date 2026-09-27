# Despliegue

Este documento cuenta como llega un commit de `main` a produccion, que hay en la maquina,
que se comparte con la otra aplicacion que vive alli y que hacer cuando algo se rompe.

Es un documento tecnico: esta escrito para que alguien que no ha desplegado esto nunca
pueda hacerlo, o pueda arreglarlo a las tres de la manana.

## Donde vive la aplicacion

|                      |                                                                             |
| -------------------- | --------------------------------------------------------------------------- |
| Web                  | https://gastonomo.bahoque.com                                               |
| API                  | https://api.gastonomo.bahoque.com                                           |
| Salud publica        | https://api.gastonomo.bahoque.com/health/ready                              |
| Contrato             | https://api.gastonomo.bahoque.com/openapi.json                              |
| Maquina              | `apps-01`, Lightsail `us-east-1`, bundle `medium_3_0` (2 vCPU, 4 GB, 80 GB) |
| Registro de imagenes | `ghcr.io/eabl0306/gastonomo-api` y `ghcr.io/eabl0306/gastonomo-web`         |
| Zona DNS             | `bahoque.com`, en Route 53                                                  |

La instancia no es solo nuestra. Ahi vive tambien Grimdex, y se eligio ese tamano
precisamente para alojar varias aplicaciones. Las dos comparten maquina y proxy, y nada
mas.

La app de Android no se despliega: se compila y se publica como APK o AAB. Lo unico que
cambia por el despliegue es la URL a la que apunta, ver [Android contra
produccion](#android-contra-produccion).

## Que es de este repositorio y que no

La frontera importa, porque lo que se documenta dos veces envejece mal en uno de los dos
sitios.

**De este repositorio:**

- `compose.prod.yaml`: los tres servicios de gastonomo (`db`, `api`, `web`), con las
  imagenes fijadas por etiqueta. Es el fichero que se copia a la maquina.
- `.github/workflows/release.yml`: construye y publica las dos imagenes en GHCR.
- `.github/workflows/deploy.yml`: entra por SSH, cambia la etiqueta, reinicia y comprueba.
- `.github/workflows/healthcheck.yml`: cada hora pregunta desde fuera si produccion
  contesta y si los certificados siguen vivos.
- Los dos `Dockerfile` de produccion, en `apps/api` y `apps/web`.
- `.env.production.example`: la plantilla del `.env` del servidor, con lo que hay que
  generar y lo que hay que pedirle al proveedor de correo. Se rellena en la maquina y la
  copia rellena no vuelve nunca al repositorio.
- `infra/compose.smoke.yaml`: la pila local que imita produccion, con el gateway de nginx y
  un puerto publicado. Es la que prueba `just stack-smoke` y el job `stack` de la CI, y no
  se despliega en ningun sitio.

**Del repositorio de infraestructura** (`infra-apps-01`, fuera de este proyecto):

- El aprovisionamiento de la maquina: Docker, cortafuegos, usuario `deploy`, swap,
  rotacion de logs, parches automaticos.
- Caddy, que es el unico proceso que escucha en los puertos 80 y 443, y su tabla de
  enrutado. El bloque de gastonomo vive ahi, en `caddy/conf/apps/`.
- La red Docker compartida `edge`.
- Las copias de seguridad de PostgreSQL y el runbook de la maquina.
- `server/rollback.sh`, el script que devuelve una aplicacion a una etiqueta anterior. Es
  del servidor y no de esta aplicacion porque es el mismo para todas: saca el nombre de la
  app del directorio donde esta, la variable de etiqueta del nombre (`gastonomo` ->
  `GASTONOMO_TAG`) y las imagenes que tiene que mover del `compose.yaml` que hay al lado.
  La copia que se ejecuta vive en `/srv/apps/gastonomo/`.

Antes vivia dentro de Grimdex, y eso significaba que el repositorio de una aplicacion
decidia el enrutado de las demas. Salio a su propio repositorio por eso. El porque esta en
el [ADR 0015](adr/0015-despliegue-en-instancia-compartida.md).

## Que hay corriendo en la maquina

| Contenedor      | De quien        | En la red `edge` | Publica puertos   |
| --------------- | --------------- | ---------------- | ----------------- |
| `caddy`         | infraestructura | si               | 80, 443 y 443/udp |
| `gastonomo-web` | este repo       | si               | no                |
| `gastonomo-api` | este repo       | si               | no                |
| `gastonomo-db`  | este repo       | no               | no                |
| `grimdex-web`   | Grimdex         | si               | no                |
| `grimdex-api`   | Grimdex         | si               | no                |
| `grimdex-db`    | Grimdex         | no               | no                |

Dos reglas que explican esa tabla y no se negocian:

1. **Solo Caddy publica puertos al host.** Las aplicaciones no publican ninguno y se
   alcanzan por nombre de contenedor sobre la red `edge`. Un puerto publicado por accidente
   se salta el cortafuegos de ufw, porque Docker enruta en la tabla `nat` y el trafico se
   evalua en `FORWARD` y no en `INPUT`.
2. **La base de datos no entra en `edge`.** Se queda en la red privada del compose de
   gastonomo, donde solo la alcanza nuestra API. Si estuviera en `edge`, cualquier
   contenedor de cualquier aplicacion de la maquina podria abrirle una conexion.

Los ficheros de cada aplicacion viven en `/srv/apps/<app>/`, propiedad del usuario
`deploy`. En el nuestro hay cuatro cosas:

```
/srv/apps/gastonomo/compose.yaml      copia de compose.prod.yaml de este repo
/srv/apps/gastonomo/.env              los secretos y la etiqueta que corre ahora, en 0600
/srv/apps/gastonomo/rollback.sh       vuelve a una etiqueta anterior
/srv/apps/gastonomo/.previous-tag     la etiqueta anterior, la escribe el despliegue
```

## Lo que se comparte con Grimdex, y lo que no

Se comparte: la maquina, el kernel, la RAM, el disco, Caddy, los certificados, la red
`edge`, el cortafuegos y el runbook.

No se comparte nada de datos: cada aplicacion tiene su propio PostgreSQL, su propio volumen,
su propio `.env` y su propio proyecto de compose. Un `docker compose down` en gastonomo no
toca a Grimdex.

Las consecuencias practicas de compartir maquina:

- **La RAM es finita.** Cada servicio tiene un limite declarado en `compose.prod.yaml`
  (512 MB la API, 512 MB la base de datos, 128 MB la web). Sin limites, una fuga en una
  aplicacion se lleva por delante a la otra.
- **Caddy es un punto unico de fallo compartido.** Si se cae, se caen las dos
  aplicaciones. Por eso tiene su propio healthcheck en un puerto interno y su propio
  limite de memoria.
- **Los nombres de contenedor son globales en la maquina.** Los nuestros llevan el prefijo
  `gastonomo-` por eso.
- **Las imagenes se construyen en CI, nunca aqui.** La maquina no tiene toolchain y no
  quiere tenerlo: compilar Rust en 4 GB compartidos con dos bases de datos es la forma mas
  rapida de tumbar lo que ya funciona.

## De un commit a produccion

Todo entra por `.github/workflows/ci.yml`, que es el unico punto de entrada. En un push a
`main` la cadena es esta:

```
ci.yml
  changes        que ha cambiado (paths-filter)
  quality / api / web / android / stack
  all-green      el unico check obligatorio de la rama
  release.yml    construye y publica las dos imagenes en GHCR   (solo main)
  deploy.yml     las pone en la maquina                          (solo main)
```

`release.yml` publica cada imagen con dos etiquetas: el SHA del commit y `main`. La que se
despliega es **siempre el SHA**. `main` es solo un puntero a "lo ultimo que salio verde",
comodo para mirar, nunca para desplegar: con una etiqueta movil nadie puede decir que esta
corriendo, y el rollback deja de existir.

`deploy.yml` hace, en este orden:

1. Prepara la sesion SSH y fija la clave del host con `ssh-keyscan`, en lugar de aceptar
   lo que conteste.
2. Entra como el usuario `deploy` a `/srv/apps/gastonomo`.
3. Se autentica en GHCR con el token efimero del propio job, en un `DOCKER_CONFIG`
   temporal que se borra al terminar. La maquina no guarda credenciales del despliegue.
4. Apunta la etiqueta que estaba corriendo en `.previous-tag`.
5. Escribe la nueva en `.env` (`GASTONOMO_TAG`), hace `docker compose pull` y
   `docker compose up -d --wait`. El `--wait` espera a los healthchecks: un contenedor que
   arranca roto falla el despliegue en vez de darse por bueno.
6. Comprueba desde internet, no desde dentro de la maquina, que `/health/live`,
   `/health/ready` y la raiz de la web contestan 200. Es la unica comprobacion que cubre
   DNS, Caddy, TLS y la aplicacion a la vez.
7. Si algo de lo anterior falla, vuelve a `.previous-tag` y reinicia.

El despliegue tiene `concurrency` propio y no se cancela a medias: un despliegue
interrumpido deja la maquina entre dos etiquetas.

## Configuracion de produccion

El `.env` de `/srv/apps/gastonomo` no esta en git y no debe estarlo. La plantilla vacia si:
es `.env.production.example`, y lleva comentado que hay que generar y como. Las variables que
hay que conocer, con el nombre exacto que espera `compose.prod.yaml`:

| Variable                                                                    | Que es                                                                           |
| --------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `GASTONOMO_TAG`                                                             | el SHA del commit que corre ahora. Lo reescribe el despliegue                    |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB`                       | credenciales de la base de datos, aleatorias                                     |
| `GASTONOMO__AUTH__ACCESS_TOKEN_SECRET`                                      | secreto de firma de los tokens, `openssl rand -hex 32`                           |
| `PUBLIC_WEB_ORIGIN`                                                         | `https://gastonomo.bahoque.com`. De ahi sale el enlace de restablecer contrasena |
| `GASTONOMO__MAIL__DRIVER`                                                   | `smtp` en produccion. `log` solo sirve para desarrollo                           |
| `GASTONOMO__MAIL__HOST` / `__PORT` / `__USERNAME` / `__PASSWORD` / `__FROM` | el SMTP del proveedor de correo                                                  |
| `GASTONOMO__EXCHANGE_RATES__ENABLED`                                        | `true` en produccion, `false` en tests y CI                                      |

Tres detalles que muerden:

- **El correo necesita un proveedor de verdad.** Con `driver=log` el correo de restablecer
  contrasena se queda en el log del contenedor y el usuario nunca lo recibe. En produccion
  va por Resend o Brevo, con SMTP y la clave API como contrasena. El remitente tiene que
  estar verificado en el proveedor, con sus registros SPF y DKIM en la zona, o el correo
  acaba en spam cuando no se rechaza.
- **El limite de peticiones lee la ultima entrada de `X-Forwarded-For`.** Caddy la escribe
  e ignora lo que mande el cliente, asi que la API puede confiar en ella. Si algun dia la
  API se expone sin proxy delante, cualquiera se salta el limite mandando la cabecera a
  mano.
- **La tarea de tasas sale a internet al arrancar.** Si la maquina no tiene salida,
  `/v1/exchange-rates` responde 503 y el catalogo de monedas sale vacio, pero la
  aplicacion arranca igual.

La web es distinta: `VITE_API_URL` se hornea en la imagen al construirla, no se lee en la
maquina. Cambiarla significa volver a construir, que es para lo que existe el `build-args`
de `release.yml`.

## Los dos dominios

Los dos son registros A en Route 53 apuntando a la IP de la instancia, y los dos los
atiende Caddy, que pide el certificado a Let's Encrypt solo y lo renueva solo.

```
gastonomo.bahoque.com       -> gastonomo-web:8080
api.gastonomo.bahoque.com   -> gastonomo-api:8080
```

El registro DNS tiene que existir **antes** de que Caddy arranque con ese nombre. Si no, la
peticion de certificado falla y gasta cupo: Let's Encrypt permite cinco fallos por nombre y
hora antes de bloquear.

Bajo el dominio de la web, Caddy enruta ademas `/v1/`, `/health/` y `/openapi.json` a la
API, antes del fallback de la SPA. Son los mismos tres prefijos que enruta
`docker/gateway.prod.conf` en la pila local, y las dos tablas tienen que decir lo mismo: un
prefijo que este en una y no en la otra no da 404, cae al fallback de la SPA y vuelve como
`index.html` con 200. Eso no es un detalle de comodidad: **la API no manda cabeceras CORS, a
proposito**, asi que la web y la API tienen que compartir origen o el navegador bloquea cada
llamada en el preflight. Es la misma forma que ya tiene la pila de desarrollo con
`docker/gateway.dev.conf`, y el mismo error que se pago una vez: sin el bloque de `/v1/`
antes del de la SPA, un GET recibe el `index.html` con 200 y un POST un 405 en HTML.

De ahi sale una regla facil de romper: **la imagen de la web se construye con su PROPIO
origen en `VITE_API_URL`**, es decir `https://gastonomo.bahoque.com`, no el dominio de la
API. Si se hornea el dominio de la API, el navegador sale cross-origin, no hay CORS y no
funciona nada. El dominio `api.` es para los clientes que no tienen CORS que satisfacer: la
app de Android, `curl` y el Swagger UI.

El resto del enrutado vive en el Caddyfile del repositorio de infraestructura, y no se copia
aqui a proposito.

## Android contra produccion

La URL base no esta en el codigo: es un `buildConfigField` por buildType en
`apps/android/data/network/build.gradle.kts`.

| buildType | `BuildConfig.API_BASE_URL`          | Texto claro                          | Log de peticiones |
| --------- | ----------------------------------- | ------------------------------------ | ----------------- |
| `debug`   | `http://10.0.2.2:8080`              | solo 10.0.2.2, 127.0.0.1 y localhost | si                |
| `release` | `https://api.gastonomo.bahoque.com` | no                                   | no                |

`10.0.2.2` es la maquina de desarrollo vista desde el emulador, y es http, asi que debug
necesita permiso explicito. Ese permiso no es global: cada buildType tiene su
`network_security_config.xml` y el de debug solo abre esos tres hosts, asi que una llamada
por http a un servidor real falla igual que fallaria en release.

```
apps/android/app/src/main/res/xml/network_security_config.xml    release: nada de http
apps/android/app/src/debug/res/xml/network_security_config.xml   debug: http solo en local
```

El de debug sustituye al de main por el mecanismo normal de recursos por buildType, asi que
la APK de release no lleva dentro ni la excepcion ni el nombre del host local. Para
comprobarlo:

```bash
source tools/scripts/jdk-env.sh
cd apps/android && ./gradlew assembleRelease
grep API_BASE_URL data/network/build/generated/source/buildConfig/release/com/bahoque/gastonomo/data/network/BuildConfig.java
```

El log de peticiones esta apagado en release a proposito: imprimiria tokens.

## Rollback

Es lo primero que se hace cuando un despliegue sale mal, antes de investigar. Con el sitio
arriba se depura mejor.

Automatico: si `deploy.yml` no consigue que produccion conteste 200, vuelve solo a la
etiqueta anterior.

A mano, desde la maquina. El script de origen es `server/rollback.sh` del repositorio de
infraestructura, y su copia en la maquina hay que actualizarla cuando cambie:

```bash
sudo -u deploy bash -c "cd /srv/apps/gastonomo && ./rollback.sh"            # a la anterior
sudo -u deploy bash -c "cd /srv/apps/gastonomo && ./rollback.sh <sha>"      # a un commit
sudo -u deploy bash -c "cd /srv/apps/gastonomo && ./rollback.sh --list"     # que hay aqui
```

El `cd` va dentro del `sudo` porque `/srv/apps` no lo puede leer el usuario `ubuntu`.

Tarda unos segundos y **no toca la base de datos**. Las migraciones son solo hacia
delante: volver atras cruzando una que borro una columna arranca, y falla en la primera
consulta que la pida. Antes de volver atras cruzando una migracion, mira que cambio.

## Logs y donde mirar

```bash
# La aplicacion, los tres servicios
sudo -u deploy bash -c "cd /srv/apps/gastonomo && docker compose ps"
sudo -u deploy bash -c "cd /srv/apps/gastonomo && docker compose logs --tail 100 api"
sudo -u deploy bash -c "cd /srv/apps/gastonomo && docker compose logs --tail 100 db"

# El proxy: certificados, arranque, upstreams caidos
docker logs --tail 100 caddy

# Los accesos de todas las aplicaciones, JSON, en el volumen de Caddy
docker exec caddy tail -n 100 /var/log/caddy/access.log
```

Los logs de contenedor estan rotados en `/etc/docker/daemon.json` y tambien en cada
servicio del compose: 10 MB por fichero, tres ficheros. Un log sin rotar es la forma
clasica de llenar un disco pequeno.

La API escribe en formato estructurado y el nivel se controla con
`GASTONOMO__TELEMETRY__LOG_LEVEL`. Subirlo a `debug` en produccion es util un rato y
peligroso un dia entero.

## Cuando algo falla

Primero, saber que capa. Son treinta segundos y evitan reiniciar lo que no era.

```bash
curl -sS -o /dev/null -w '%{http_code}\n' https://gastonomo.bahoque.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://api.gastonomo.bahoque.com/health/ready
```

| Lo que ves                                  | Donde esta                                 |
| ------------------------------------------- | ------------------------------------------ |
| SSH tampoco entra                           | la maquina o la red                        |
| SSH entra y curl no contesta                | Caddy, o el cortafuegos de Lightsail       |
| 502 desde Caddy                             | el contenedor esta caido o no esta sano    |
| 503 en `/health/ready`                      | la API vive y la base de datos no contesta |
| 200 con contenido viejo                     | un despliegue a medias: vuelve atras       |
| El navegador bloquea las llamadas de la web | un problema de origen, no de la maquina    |

Casos concretos:

**`/health/ready` responde 503.** El `status` es `degraded` y el `checks` dice cual falla.
Casi siempre es `database`: mira si `gastonomo-db` esta sano y si `.env` sigue teniendo las
credenciales que espera.

**La API no arranca.** Lo mas comun es una variable obligatoria que falta: el compose usa
`${VAR:?mensaje}` para los secretos, asi que compose se niega a arrancar y dice cual es.

**La web carga pero no trae datos y la consola habla de CORS.** No es la maquina: es la
imagen de la web, que se construyo con el dominio de la API en `VITE_API_URL` en vez de con
el suyo. Hay que volver a construirla; ver [Los dos dominios](#los-dos-dominios).

**El certificado no se renueva.** Lo avisa `healthcheck.yml` catorce dias antes. Mira
`docker logs caddy`: casi siempre es un registro DNS que ya no apunta aqui.

**La maquina se queda sin memoria.** 4 GB para dos aplicaciones con dos bases de datos. Hay
2 GB de swap, que convierten un OOM en un minuto lento, no en un sitio donde vivir.
`docker stats` dice quien esta comiendo.

## Lo que no hay que hacer

- **No construir imagenes en la maquina.** No hay toolchain y no debe haberlo.
- **No editar `compose.yaml` a mano en la maquina.** El fichero de verdad es
  `compose.prod.yaml` de este repositorio; un cambio local se pierde en el proximo despliegue
  y, peor, no aparece en ningun sitio donde alguien lo lea.
- **No desplegar la etiqueta `main`.** Ver la seccion del SHA.
- **No publicar puertos** en `compose.prod.yaml`. Se entra por Caddy.
- **No meter la base de datos en la red `edge`.**
- **No poner secretos en el repositorio de infraestructura.** El directorio de
  configuracion de Caddy va montado dentro del contenedor y lo comparten todas las
  aplicaciones.
