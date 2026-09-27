# ADR 0015: Despliegue en una instancia compartida, con las imagenes construidas en CI

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

Hasta ahora este repositorio sabia construir imagenes de produccion (ADR 0007) pero no
tenia donde ponerlas. Habia que decidir el despliegue, y habia un dato que condicionaba
todo: ya existe una maquina, `apps-01` en Lightsail, con 2 vCPU, 4 GB de RAM y 80 GB de
disco, alquilada precisamente con la idea de alojar varias aplicaciones. Ahi corre Grimdex,
que ocupa 80 MB de RAM y deja 3 GB libres.

Las alternativas que se miraron:

**Una instancia por aplicacion.** Aisla de verdad: un OOM en una no toca a la otra, y cada
una tiene su cortafuegos y su ciclo de vida. Cuesta el doble, obliga a aprovisionar y
parchear dos maquinas, y para dos aplicaciones pequenas de un solo desarrollador el
aislamiento que da no se llega a usar.

**Un PaaS** (Fly, Railway, Render). Resuelve TLS, despliegue y escalado sin escribir nada.
Pero el modelo de datos de esta aplicacion es un PostgreSQL con dinero dentro, y en todos
ellos la base de datos gestionada cuesta mas que la maquina entera. Ademas mueve el
conocimiento operativo a un panel de control en vez de a ficheros en git.

**Kubernetes**, en cualquiera de sus formas ligeras. Para dos aplicaciones es mas maquinaria
que aplicacion.

**La misma instancia, con un proxy delante.** Es lo que ya funciona para Grimdex.

Sobre el proxy tambien habia eleccion. **Traefik** descubre los contenedores solo, leyendo
etiquetas de Docker, y evita tener una tabla de enrutado que mantener. El problema es
justamente ese: la tabla queda repartida en etiquetas dentro de los compose de cada
aplicacion, y una etiqueta mal escrita no rompe nada, simplemente no enruta, y eso se
descubre en produccion. **Caddy** con un fichero por aplicacion se lee de arriba abajo, se
valida con `caddy validate` antes de subirlo, y un error es visible en un sitio.

Y quedaba una pregunta que Grimdex ya habia contestado mal: **donde vive la configuracion
del proxy**. Estaba dentro del repositorio de Grimdex, porque era la primera aplicacion.
Anadir gastonomo significaba que un commit en el repositorio de Grimdex decidiera si
gastonomo recibe trafico.

## Decision

**Una sola instancia compartida, con Caddy delante y un proxy inverso como unico proceso
publicado.** Las aplicaciones no publican ningun puerto al host: se alcanzan por nombre de
contenedor sobre una red Docker externa llamada `edge`, que crea la infraestructura y
comparten todos los inquilinos. La base de datos de cada aplicacion se queda fuera de
`edge`, en la red privada de su propio compose, para que ningun contenedor de otra
aplicacion pueda abrirle una conexion.

Cada servicio declara un limite de memoria. Compartir maquina sin limites significa que una
fuga en una aplicacion se lleva a la otra.

**Las imagenes se construyen en CI y nunca en la maquina.** `release.yml` construye las dos,
las publica en GHCR y ahi se acaba su trabajo. La maquina no tiene Rust, ni Node, ni
Gradle, y no debe tenerlo. Tres razones, en orden de importancia:

1. **Compilar es lo que mas recursos consume de todo el ciclo.** Un `cargo build --release`
   en 4 GB compartidos con dos PostgreSQL tumba lo que ya funciona. El servidor sirve
   trafico; construir es otro trabajo y va en otra maquina.
2. **Lo que se prueba es lo que se despliega.** La imagen que sube a produccion es
   exactamente el artefacto que paso la CI, byte por byte. Si se construyera en el servidor,
   seria otra construccion, con otra cache y otro reloj, y "en mi maquina pasaba" vuelve a
   ser una frase posible.
3. **Un despliegue no puede fallar por una compilacion.** Separado, el unico modo de fallo
   del despliegue es bajar una imagen y arrancarla.

**La etiqueta de la imagen es el SHA del commit, nunca `latest`.** Se publica tambien
`main`, pero solo como puntero legible de "lo ultimo que salio verde"; lo que el `.env` del
servidor fija, y lo que se despliega, es el SHA. Con una etiqueta movil no se puede
contestar a la pregunta que importa cuando algo va mal, que es "que version esta corriendo",
y el rollback deja de ser una operacion: no hay nada a lo que volver. Con el SHA, volver
atras es cambiar una linea del `.env` y reiniciar, y el despliegue apunta la etiqueta
anterior en `.previous-tag` antes de tocar nada para que ese cambio sea un comando y no una
excavacion.

**La infraestructura sale a su propio repositorio**, `infra-apps-01`, que no pertenece a
ninguna aplicacion. Ahi van el aprovisionamiento de la maquina, Caddy y su tabla de
enrutado, la red `edge`, las copias de seguridad y el runbook. En este repositorio se queda
lo que es de esta aplicacion: los dos Dockerfile, `compose.prod.yaml` y los tres workflows
de release, despliegue y comprobacion.

El criterio de reparto es quien sufre un cambio. Si toco el enrutado, puedo tirar las dos
aplicaciones, asi que no puede vivir en el repositorio de una de ellas. Si toco
`compose.prod.yaml`, solo puedo tirar gastonomo, asi que vive aqui.

## Consecuencias

Lo bueno:

- Una maquina, un proxy, un sitio donde mirar el enrutado. Anadir la tercera aplicacion es
  un fichero en el repositorio de infraestructura y un directorio en `/srv/apps`.
- El despliegue es reversible en segundos y siempre se sabe que corre.
- Los certificados son problema de Caddy, incluido renovarlos.
- La comprobacion posterior al despliegue se hace desde internet, asi que cubre DNS, TLS,
  proxy y aplicacion a la vez. Es el mismo error que ya se pago una vez en este repositorio
  al probar cada imagen por separado y dejar la API entera inalcanzable.

Lo que se acepta a cambio:

- **Caddy es un punto unico de fallo para todas las aplicaciones de la maquina.** Si se cae,
  se caen todas. Tiene su healthcheck y su limite de memoria, pero el riesgo no desaparece.
- **La maquina es un punto unico de fallo.** No hay alta disponibilidad y no se pretende.
  Una reinstalacion es reejecutar el aprovisionamiento y volver a subir los `.env`.
- **Los 4 GB se reparten.** El dia que una aplicacion necesite de verdad la maquina entera,
  hay que separarla, y esa conversacion llegara por `docker stats`, no por sorpresa.
- **Tres repositorios que hay que mirar juntos** cuando el despliegue cambia de forma: este,
  el de infraestructura y, si toca el patron comun, el de Grimdex. Es el precio de que el
  enrutado no lo decida el repositorio de una aplicacion.
- **El rollback no revierte la base de datos.** Las migraciones son solo hacia delante, y
  volver atras cruzando una que borro una columna arranca y falla en la primera consulta que
  la pida.
