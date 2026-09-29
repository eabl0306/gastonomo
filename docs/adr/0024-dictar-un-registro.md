# ADR 0024: Dictar un registro devuelve un borrador, y no lo crea

- Estado: aceptado
- Fecha: 2026-09-28

## Contexto

Apuntar un gasto son cinco decisiones: importe, moneda, categoria, fecha y nota. Cada una es
barata por separado, y juntas son suficiente friccion para dejarlo "para luego". Luego es cuando
no se apunta, y una aplicacion de finanzas personales que no se usa no sirve para nada. Dictar
"gasté 45 mil en el almuerzo" es una sola accion.

Pasar la voz a texto ya lo hace el sistema operativo del telefono, gratis, sabiendo el idioma del
usuario y sin subir audio a ninguna parte. Lo que falta es lo otro: convertir una frase en campos.
Eso no se puede hacer con reglas, y se puede hacer razonablemente bien con un modelo de lenguaje.

Esa es la primera vez que este repositorio se apoya en algo con tres propiedades que no habia
tenido antes:

- **Cuesta dinero por peticion.** Frankfurter es gratis y solo la llamamos nosotros, dos veces al
  dia ([ADR 0010](0010-cacheo-de-tasas-de-cambio.md)). Aqui cada vez que alguien pulsa el
  microfono hay una factura, y quien la paga es el dueno de la instancia, no quien pulsa.
- **Puede equivocarse.** Todo lo demas de la API es determinista: con la misma entrada sale la
  misma salida, y si sale mal es un fallo. Un modelo devuelve algo verosimil, que no es lo mismo
  que algo correcto.
- **Puede no estar.** Es una capacidad opcional de la que el resto del producto no debe depender.

Y encima de todo eso esta la regla que manda en esta aplicacion: **el historico es de solo
anadir**. No hay `PATCH` ni `DELETE` de movimientos, prestamos ni abonos, y no los habra. Ni por
la puerta de atras: hasta borrar una categoria archiva en vez de destruir
([ADR 0022](0022-archivar-categorias.md)). Un registro equivocado se queda, y solo se arregla
registrando otro que lo compense.

De ahi sale la pregunta entera de este ADR: si lo que un modelo devuelve puede estar mal, y lo que
se escribe no se puede arreglar, cuanto se le deja escribir.

## Decision

`POST /v1/dictation/parse` recibe el texto y devuelve un **borrador**. No escribe nada.

### Que no cree nada es la decision, no un detalle

La respuesta es un movimiento o un prestamo a medio rellenar, para que el usuario lo vea en una
tarjeta de revision y lo confirme. Confirmar es una llamada aparte a `POST /v1/movements` o
`POST /v1/loans`, las de siempre, con su `Idempotency-Key`.

Un endpoint que creara el movimiento directamente seria una linea menos de codigo en el cliente y
un agujero permanente en el producto: bastaria que el modelo entendiera 450 000 donde se dijo
45 000 para que quedara un gasto diez veces mayor que nadie puede borrar. La friccion que se queria
quitar era rellenar cinco campos, no mirar lo que se va a guardar.

Es tambien lo que mantiene una sola puerta de entrada al historico. Todas las reglas de negocio de
crear un movimiento (categoria de la cuenta, los cuatro campos de importe, la idempotencia) siguen
viviendo en un unico caso de uso, y el dictado no es una segunda forma de escribir con sus propias
reglas que se separarian de las primeras en la tercera semana.

### Por eso no lleva `Idempotency-Key`

La cabecera existe porque un reintento puede duplicar dinero ([ADR
0012](0012-idempotencia-con-cabecera.md)). Aqui no hay nada que duplicar: repetir la peticion
devuelve otro borrador y el usuario sigue teniendo que confirmarlo. Lo unico que cuesta un
reintento es otra llamada al modelo, y eso lo resuelve un tope de uso, no una tabla de claves.

Hay un test de integracion que dicta y despues pide `GET /v1/movements` para comprobar que la lista
sigue vacia. Si algun dia este endpoint empieza a escribir, ese test lo dice.

### La normalizacion la hace la API, y ninguna regla rechaza la peticion

Lo que el modelo devuelve no se sirve tal cual. Se comprueba contra la cuenta y **lo que no encaja
se sustituye por algo que si encaja**:

- Una categoria que no existe cae en "Otros". Se busca por nombre, sin distinguir mayusculas,
  entre las del tipo del movimiento.
- Las reservadas de prestamos (`Préstamo` y `Abono préstamo`) ni se le ofrecen al modelo ni se le
  pueden asignar. Son internas: un movimiento suelto colgado de ellas mentiria en la pantalla de
  Prestamos.
- Una moneda que hoy no se puede equivaler cae en la moneda principal de la cuenta. Guardar un
  importe cuya equivalencia nadie puede calcular rompe la regla de los cuatro campos
  ([ADR 0011](0011-multimoneda-de-cuatro-campos.md)) y el usuario no tendria como arreglarlo.
- Una fecha futura o ilegible cae en hoy. Un gasto de manana no lo ha dictado nadie, y colarse en
  el futuro descoloca los totales del periodo.
- Un importe que no es positivo se devuelve como `null`.

Ninguna de estas reglas responde un error. Un borrador a medias que el usuario completa en dos
toques es mucho mejor que un 422 que le deja sin nada que revisar y con la frase perdida. Lo mismo
vale para `today`: si viene con un formato ilegible se ignora y se usa el dia del servidor, porque
tumbar un dictado por el formato de un campo auxiliar es perder lo que la persona acaba de decir.

Que esto viva en la API y no en los clientes es lo importante. En los clientes habria que
escribirlo dos veces, en Kotlin y en TypeScript, y despues conseguir que no se separaran nunca. Y
hay una razon mas fuerte: la API es la unica que sabe que categorias tiene la cuenta, cual es su
moneda principal y que monedas tienen tasa hoy. El cliente tendria que mandar todo eso en la
peticion, y entonces quien llama podria mentir.

### `missing` lo dice la API, no lo adivina el cliente

El borrador trae una lista de lo que el usuario tiene que completar antes de poder guardar:
`"amount"` si no se entendio el importe, `"person"` si es un prestamo y no se sabe con quien. El
cliente deja el boton de guardar apagado mientras la lista no este vacia.

Podria deducirlo cada cliente mirando que campos vienen vacios. Serian dos implementaciones de la
misma regla, y el dia que se anada un campo obligatorio una de las dos se quedaria atras: el movil
dejaria guardar algo que la web no, o al reves. Es la misma razon por la que el estado de un
prestamo lo deriva el cliente pero los campos obligatorios los decide el servidor.

### El acceso es una lista de aprobados en su propia tabla

El dictado esta disponible solo para las cuentas que estan en `dictation_allowlist`. Conceder es un
`INSERT`, quitar es un `DELETE`, y las dos cosas las hace a mano el dueno de la instancia.

```sql
INSERT INTO dictation_allowlist (user_id) VALUES ('<uuid>');
```

Es una tabla aparte y no una columna en `users` a proposito. La aprobacion **no es un atributo de
la persona**: no la elige, no la ve en su perfil y no viaja en `/v1/me`. Es una decision de quien
paga la factura. Ponerla en `users` obligaria a arrastrar un campo por el dominio, la API y los
tres clientes para algo que nadie mas tiene que leer. El precedente es `user_change_counters`
([ADR 0023](0023-numero-de-cambio-por-cuenta.md)): lo que es de un usuario pero no es suyo vive en
su propia tabla.

Estar en la lista es toda la informacion que hay. No hay columna de estado ni de revocado: quitar
el acceso es borrar la fila. Una bandera se puede leer al reves en una consulta y dejar el dictado
abierto a todo el mundo; una fila que no esta no se lee al reves.

### Quien no esta aprobado recibe 404, no 403

Es la misma regla que el resto de la API: un recurso de otra cuenta responde 404 porque un 403
confirmaria que existe. Aqui lo que no se confirma es que la capacidad exista. Un 403 le dice a
cualquiera que se registre que hay un dictado detras y que solo le falta permiso, y eso es una
invitacion a pedirlo, a insistir o a buscarle la vuelta a una funcion que cuesta dinero.

La comprobacion es lo primero que hace el caso de uso, antes de leer los ajustes, las categorias y
las tasas, y antes de llamar al modelo. Una cuenta sin aprobar no puede gastar ni una consulta.

### El importe se le pide al modelo como texto

En el contrato el dinero viaja como string y nunca como numero
([ADR 0009](0009-dinero-con-decimal.md)). Eso se mantiene hasta el ultimo eslabon: al modelo
tambien se le pide el importe como texto con punto decimal y sin separador de miles. Un `"amount"`
de tipo numero en el esquema de la herramienta haria que 186400.50 pasara por un `f64` antes de
llegar a `Decimal`, y la regla es que no pase por coma flotante ni un instante, en ninguna capa.

Y no se le pide que devuelva JSON suelto: se le obliga a rellenar una herramienta con esquema
(`tool_choice`). Sin eso, cuando la frase es rara contesta texto libre y habria que adivinar donde
esta el JSON.

### Sin clave la capacidad esta apagada, no rota

`GASTONOMO__CLAUDE__API_KEY` vacia significa dictado apagado: el endpoint responde **503** y el
resto de la API funciona exactamente igual. El proceso arranca sin clave a proposito, porque una
capacidad opcional no puede impedir que el servicio se levante.

El 503 y no un 500 es el mismo criterio que `GET /v1/exchange-rates` sin ninguna pasada guardada:
no ha fallado nada, falta configuracion, y un 500 invitaria al cliente a tratarlo como un fallo del
servidor. Las otras tres variables (`__MODEL`, `__REQUEST_TIMEOUT_MS`, `__MAX_TOKENS`) tienen
valores por defecto que sirven. El modelo por defecto es el pequeno de la familia: interpretar una
frase y devolver unos pocos campos no necesita mas, y esto se paga por palabra.

El tope de 500 caracteres del texto no es del modelo. Un registro dictado cabe en una frase; el
limite esta para que nadie pueda mandar un libro entero a algo que se cobra por palabra.

### Donde encaja en la arquitectura

Igual que Frankfurter. El caso de uso `ParseDictation` vive en `application` y depende de dos
puertos nuevos, `DictationInterpreter` y `DictationAccess`, mas los repositorios de categorias,
ajustes y tasas que ya existian. El puerto del interprete **habla tipos de dominio**: recibe texto
y un contexto de la cuenta, y devuelve una `Interpretation`. Los prompts, el esquema de la
herramienta y el formato de la respuesta se quedan en el adaptador.

Por eso el nucleo no sabe que detras hay un modelo de lenguaje, y menos de que proveedor. Cambiarlo
es cambiar un adaptador. Y los tests del caso de uso usan un interprete falso: no salen a internet
y no cuestan nada.

El resultado en bruto tiene **todos los campos opcionales** menos uno. Es lo que permite que la
normalizacion de arriba exista: un modelo puede devolver cualquier cosa, y un campo que no se
entiende se descarta solo en vez de tumbar la peticion.

## Consecuencias

A favor:

- Apuntar un gasto pasa de cinco campos a una frase y una confirmacion, que es el objetivo.
- El historico no depende de que un modelo acierte. Lo que se escribe lo confirma una persona, por
  el mismo endpoint y con las mismas reglas que si lo hubiera tecleado.
- Las reglas de que es un borrador valido estan en un sitio. Los dos clientes pintan lo que llega y
  apagan el boton de guardar segun `missing`.
- La factura tiene tres topes duros: quien puede llamar, cuanto texto entra y cuanto sale.
- Sin clave configurada el producto entero sigue en pie, con una capacidad menos.

En contra, y hay que saberlo:

- **Son dos llamadas para guardar un registro**, y la segunda puede fallar cuando la primera fue
  bien. El usuario ve el borrador y el guardado falla, que es lo mismo que le pasa hoy al
  formulario normal, asi que la cola de escrituras pendientes ya lo cubre
  ([ADR 0020](0020-cola-de-escrituras-pendientes.md)).
- **No hay cache.** Dos frases identicas son dos llamadas, porque dos cafes de 45 000 el mismo dia
  son dos gastos legitimos y no hay forma de distinguirlos de un reintento. Es la misma razon por
  la que deduplicar por contenido se descarto en el ADR 0012.
- **Los aprobados se gestionan por consola.** No hay pantalla ni endpoint de administracion, y con
  los usuarios que tiene esta instancia no hace falta. El dia que haga falta, es una tabla que ya
  esta.
- **Cada peticion manda a un tercero el texto dictado y los nombres de las categorias de la
  cuenta.** No manda importes ya guardados, ni el historico, ni el correo, ni el identificador del
  usuario, pero lo que se dicta es informacion personal y sale de la maquina. Es la razon de fondo
  de que esto sea opt-in cuenta a cuenta y no una funcion que aparece sola.
- **El modelo es un tercero mas del que depender**, con su latencia y sus cortes. Si no responde,
  el endpoint da 500 y el formulario normal sigue ahi: el dictado es un atajo, nunca el unico
  camino.
- **La calidad de la interpretacion no esta medida.** No hay suite que diga cuantas frases de cada
  cien salen bien; los tests comprueban el contrato y la normalizacion, no el acierto. Mientras el
  usuario revise antes de guardar, un fallo cuesta un toque; ese es justamente el motivo de que la
  revision no sea opcional.

## Alternativas descartadas

**Crear el registro directamente y ya.** Es la version obvia y la que quita mas friccion. Se
descarta por lo dicho arriba: sin `PATCH` ni `DELETE`, un error del modelo es permanente. La unica
forma de hacerla segura seria anadir esos endpoints, y eso es exactamente lo que el producto no va
a hacer.

**Reglas y expresiones regulares en el cliente.** No cuesta dinero, funciona sin red y no manda
nada a nadie. Cubre "45 mil en almuerzo" y se cae con "le presté a Rosa doscientos para el arriendo
hasta el viernes". Y habria que escribirlo dos veces, en Kotlin y en TypeScript, con dos ideas
distintas de que significa "ayer".

**Mandar el audio a un modelo multimodal** en vez de texto. Una llamada menos y mejor con acentos
raros, pero cuesta mucho mas por peticion, sube la latencia, obliga a mover ficheros de audio por
la API y manda a un tercero la voz de la persona y no solo lo que dijo. El reconocimiento del
sistema ya esta pagado y ya sabe el idioma.

**Que el cliente mande el contexto** (sus categorias, su moneda, sus tasas) y normalice el
resultado, dejando a la API solo hablar con el modelo. Menos consultas por peticion y una API mas
delgada, pero deja que quien llama decida contra que se comprueba lo dictado, pone la misma regla
en dos sitios que acabaran separandose, y expone tal cual lo que el modelo invento, incluidos
nombres de categoria que no existen.

**Una columna `dictation_enabled` en `users`.** Un `ALTER TABLE` y listo, pero mete en el agregado
`User` un campo que no es del usuario, que hay que arrastrar por el dominio y los DTO, y que
alguien acabara devolviendo en `/v1/me`.

**Abrirlo a cualquier cuenta con un limite de peticiones por usuario.** Es lo que haria un producto
con facturacion. Aqui no la hay: el limite lo pagaria el dueno de la instancia multiplicado por el
numero de desconocidos que se registren. Una lista de aprobados es una fila por persona y ningun
riesgo.

**Un endpoint de confirmacion propio**, `POST /v1/dictation/confirm`, que creara el movimiento a
partir del borrador. Ahorra que el cliente rellene el cuerpo, y crea una segunda puerta de entrada
al historico con sus propias validaciones. Las reglas de crear dinero se quedan en un unico sitio.
