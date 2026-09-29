# ADR 0008: Access token JWT corto y refresh token opaco rotatorio

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

El rediseno anade cuentas de usuario: la aplicacion anterior era completamente local y no
tenia ni login. Ahora hay pantallas de registro, login, recuperacion de contrasena y
cambio de contrasena, y tres clientes que comparten la misma API: la web, la app de
Android y lo que venga despues.

Lo que el diseno impone y hay que cumplir:

- La sesion sobrevive a cerrar la aplicacion. En el movil, el usuario no espera volver a
  escribir la contrasena cada vez.
- Hay un cerrojo local (PIN y huella) que protege la aplicacion en el dispositivo. Ese
  cerrojo protege **algo que esta guardado ahi**, asi que tiene que haber un secreto de
  larga vida en el dispositivo.
- "Enviar enlace de recuperacion" y una pantalla que dice literalmente "Caduca en 30
  minutos".
- Cambiar la contrasena desde Ajustes tiene que cerrar las demas sesiones. Es lo que
  espera cualquiera que cambie una contrasena porque cree que se la han visto.
- La pantalla de recuperacion ensena el correo que escribio el usuario, no uno que
  confirme la API. El diseno esta evitando a proposito filtrar quien tiene cuenta.

Y lo que el contexto del proyecto impone: la API no tiene estado compartido entre
peticiones mas alla de PostgreSQL, no hay Redis, y la arquitectura hexagonal exige que el
nucleo no sepa que existen JWT ni SQL.

Las opciones consideradas fueron tres: sesiones de servidor con cookie, un solo token de
larga vida, y el par access mas refresh.

Las sesiones de servidor con cookie se descartaron por los clientes nativos: una cookie
en Android obliga a montar un `CookieJar`, y `SameSite` no significa nada fuera de un
navegador. Ademas la web y la API son origenes distintos y hoy se sirven bajo el mismo
origen con un gateway de nginx, lo que haria que el dominio de la cookie dependiera del
despliegue.

Un solo token de larga vida se descarto porque, o se guarda en la base y se consulta en
cada peticion (que es una sesion de servidor con otro nombre), o no se puede revocar.

## Decision

Dos tokens con papeles distintos.

### El access token es un JWT HS256 de 15 minutos

- Se firma con `GASTONOMO__AUTH__ACCESS_TOKEN_SECRET` y **no se guarda en ninguna tabla**.
  Validarlo es comprobar la firma y la caducidad, sin tocar PostgreSQL: es lo que permite
  que cada peticion protegida no cueste una consulta extra.
- Lleva `sub` (el `user_id`), `iat` y `exp`. Nada mas. No hay roles ni permisos porque no
  hay endpoints de administracion: cada consulta filtra por el usuario del token y punto.
- Lo valida el middleware `RequireAccessToken` de `crates/http/src/auth_context.rs`, que
  deja el `UserId` en el `Depot`. Los handlers lo leen con `current_user`, y ninguno sabe
  que hay un JWT detras.
- El puerto es `TokenIssuer`, en `application`. El nucleo declara "algo que emite y
  verifica tokens"; que sea HS256 lo decide el adaptador
  `crates/infrastructure/src/security/jwt_token_issuer.rs`.

**El proveedor HS256 esta escrito a mano con `hmac` y `sha2.`** El crate `jsonwebtoken` se
usa sin su feature `rust_crypto`, porque esa feature arrastra `rsa`, que tiene
RUSTSEC-2023-0071 sin parche y tumbaria `cargo deny`. El fichero es
`crates/infrastructure/src/security/hs256.rs`, solo soporta HS256, y esta al 86% de
cobertura. Si algun dia hace falta RS256 (por ejemplo para federar con un proveedor de
identidad), este es el sitio donde cambiarlo.

### El refresh token es opaco, de 30 dias y rotatorio

- Es un valor aleatorio de 256 bits en base64url, generado por
  `RandomOpaqueTokenFactory`. No es un JWT: no lleva informacion, es un puntero a una
  fila.
- En `refresh_tokens` **solo se guarda su hash**, igual que una contrasena. Si alguien lee
  la base no se lleva sesiones utilizables.
- `POST /v1/auth/refresh` **rota**: marca el token recibido como revocado, apunta
  `replaced_by` al nuevo y devuelve un par nuevo.
- **Reutilizar un token ya rotado revoca la familia entera.** Un refresh token se usa una
  vez; que aparezca dos veces significa que alguien tiene una copia. La respuesta estandar
  es echar a los dos: el ladron y el dueno legitimo se quedan fuera, y el dueno vuelve a
  entrar con su contrasena. Es preferible a dejar la sesion robada viva.
- `POST /v1/auth/logout` revoca el token recibido y responde 204.
- Es el secreto que el PIN y la huella protegen en el dispositivo. Por eso el cerrojo
  biometrico no toca la API y no se sincroniza: activarlo desde otro telefono no
  significaria nada.

### La recuperacion de contrasena tambien usa un token opaco

- `password_reset_tokens` guarda el hash, con 30 minutos de caducidad, y es de un solo
  uso: al usarlo se marca `used_at`.
- **Emitir uno borra los anteriores de esa cuenta**, asi que solo hay uno vivo a la vez.
  Sin esto, tocar "reenviar" tres veces dejaba tres enlaces abriendo la misma cuenta, y no
  hay ninguna pantalla donde verlos ni revocarlos. Como efecto util, la tabla se queda en
  una fila por cuenta y no hace falta ninguna tarea de limpieza.
- Dos peticiones **muy seguidas** no borran nada: si hay un enlace vivo emitido hace menos
  de un minuto, la segunda no emite ni manda correo. Sin esa espera, cualquiera que supiera
  un correo podia borrar en bucle el enlace que su dueno acababa de recibir y dejarle sin
  recuperar la cuenta. La respuesta sigue siendo 202, asi que tampoco es un oraculo.
- `POST /v1/auth/password/forgot` responde **202 con el cuerpo vacio siempre**, exista o
  no el correo.
- **El envio del correo sale de la peticion a una tarea de fondo.** Con el driver de log
  no se notaba, pero con SMTP real el tiempo de respuesta delataba la cuenta: 50 ms si
  existia frente a 2 ms si no, 24 veces mas, con las dos distribuciones sin solaparse. Un
  202 uniforme no sirve de nada si el reloj contesta la pregunta. Ahora la diferencia es
  de 1,5 ms, el 3% de lo que era.
- Cambiar la contrasena (`password/change` o `password/reset`) revoca todos los refresh
  tokens del usuario menos el que venga en el cuerpo, que es el del dispositivo desde el
  que se hace el cambio.

### Las contrasenas van con Argon2id

Argon2id con los parametros por defecto de `argon2` 0.6, que son los recomendados por
OWASP: 19 MiB de memoria, dos pasadas y un carril. Se escriben de forma explicita en
`Argon2PasswordHashing` para que se vea que son una decision y no un descuido.

**El calculo sale del hilo del runtime con `spawn_blocking`.** Son decenas de
milisegundos de CPU pura; dejarlos en un hilo de trabajo de tokio para todas las
peticiones que ese hilo llevaba a medias, y un pico de logins simultaneos tumbaba la
latencia del resto de la API. Por eso el puerto `PasswordHasher` es asincrono aunque el
algoritmo no espere a ningun IO: lo necesita para poder salirse del hilo.

### Limite de peticiones en las cuatro puertas

`register`, `login`, `password/forgot` y `password/reset` llevan un limite de 10
peticiones por minuto y cliente, configurable con
`GASTONOMO__AUTH__RATE_LIMIT_MAX_REQUESTS` y `RATE_LIMIT_WINDOW_SECONDS`.

El criterio es: endpoint publico que toca credenciales. `register` y `login` prueban
contrasenas contra un hash de Argon2, `forgot` manda correo, y `reset` cambia una
contrasena y revoca todas las sesiones sin pedir token de acceso. `refresh` y `logout`
se quedan fuera porque exigen un refresh token valido en el cuerpo.

Lo que este limite **no** es: una defensa contra adivinar un token. Los tokens opacos
son 256 bits de CSPRNG; no se adivinan ni con limite ni sin el. Lo que acota es el
ruido, la carga y el correo.

El cliente se identifica por la **ultima** entrada de `X-Forwarded-For`, y por la IP del
par si la cabecera no viene. Leer la primera entrada, que es lo que se hizo primero,
dejaba saltarse el limite por completo mandando una cabecera falsa. Y no era solo un
riesgo de exposicion directa: el gateway de nginx de este mismo repositorio usa
`$proxy_add_x_forwarded_for`, que **anade** la IP real al final de lo que mando el
cliente, asi que el bypass funcionaba a traves del proxy previsto.

## Consecuencias

A favor:

- Una peticion protegida no cuesta ninguna consulta de sesion.
- La revocacion existe donde importa: cerrar sesion y cambiar la contrasena invalidan los
  refresh tokens de verdad.
- La rotacion deja rastro de la reutilizacion de un token viejo.
- El nucleo no sabe que existen JWT: los tests de los casos de uso de autenticacion usan
  un emisor falso y no firman nada.

En contra, y hay que saberlo:

- **El access token no se invalida al cerrar sesion ni al cambiar la contrasena.** Sigue
  valido hasta 15 minutos. Es el compromiso habitual de un JWT sin estado y esta aceptado
  a proposito. Cerrarlo pediria una de estas tres cosas, y ninguna es gratis: una lista de
  revocacion consultada en cada peticion (que borra la ventaja de no tocar la base), un
  contador de version de sesion por usuario en el token mas una consulta cacheada, o
  bajar los 15 minutos a uno o dos y asumir mas trafico de refresh. Si algun dia se
  decide, el sitio es `RequireAccessToken`.
- **El limite de peticiones es una ventana fija en memoria del proceso.** Con varias
  replicas cada una lleva su cuenta, asi que el limite efectivo se multiplica por el
  numero de replicas. Un limite compartido necesita Redis o equivalente.
- **Queda una diferencia de tiempo de 1,5 ms en `forgot`**, la del `INSERT` del token
  cuando la cuenta existe. Sobre red es ruido; en local las dos distribuciones todavia no
  se solapan del todo. Igualarlo por completo pediria trabajo ficticio en la rama sin
  cuenta, y no parece que valga la pena.
- **`GASTONOMO__AUTH__ACCESS_TOKEN_SECRET` tiene un valor de desarrollo por defecto** y el
  arranque solo deja un aviso en el log, no aborta. Es para que un `cargo run` recien
  clonado funcione. Hay que fijarlo de verdad antes de cualquier despliegue.
- El secreto es simetrico: cualquier proceso que valide tokens puede firmarlos. Con un
  solo servicio no importa; si algun dia hay mas de uno, toca RS256.
