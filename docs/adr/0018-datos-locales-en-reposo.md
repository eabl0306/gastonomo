# ADR 0018: Los datos locales en reposo: sin SQLCipher, y sin copia de seguridad

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

El [issue 14](https://github.com/eabl0306/gastonomo/issues/14) mete una base de datos local con
Room para que la aplicacion se pueda usar sin conexion. A partir de ahi, el telefono guarda el
historico financiero entero: importes, fechas, personas a las que se debe dinero y cuanto.

Eso obliga a decidir a proposito que proteccion tienen esos datos en reposo, y a escribir por que,
en vez de dejar que se quede como este por descuido.

Lo que ya habia decidido el [ADR 0014](0014-sesion-en-el-cliente-android.md): el refresh token se
guarda cifrado con una clave AES-256-GCM del Android Keystore, y hay un cerrojo local opcional de
PIN o huella que tapa la aplicacion al abrirla.

El `minSdk` del proyecto es 29.

## Decision

### La base de datos no se cifra con SQLCipher

En Android 10 y posteriores el almacenamiento privado de la aplicacion esta cifrado en reposo por
el propio sistema, con cifrado basado en ficheros y una clave derivada del bloqueo de pantalla del
usuario. Con `minSdk` 29 eso vale para todos los dispositivos que pueden instalar la aplicacion.

Contra los ataques que de verdad importan aqui, SQLCipher no anade nada:

- **Telefono robado y bloqueado.** El cifrado del sistema ya lo cubre.
- **Telefono robado y desbloqueado.** SQLCipher no ayuda: la propia aplicacion abre la base de
  datos, asi que quien tenga el telefono desbloqueado ve los datos por la pantalla. Lo que cubre
  ese caso es el cerrojo local del ADR 0014.
- **Otra aplicacion del mismo telefono.** No puede leer el directorio privado; eso lo impone el
  sandbox, no el cifrado del fichero.

Lo que si costaria: una dependencia nativa de varios megas, y una clave que tendria que vivir en
el Keystore y estar en memoria **todo el tiempo que la aplicacion este abierta**, porque la base
de datos se consulta en cada pantalla. Es mas superficie a cambio de poco.

Hay un caso en el que si cambiaria la respuesta, y conviene dejarlo escrito: **un dispositivo
rooteado o un volcado del almacenamiento**. Ahi el cifrado del sistema ya esta abierto y SQLCipher
si aporta. No es el escenario de esta aplicacion hoy, y si algun dia lo es, la decision se
revisa: cambiar a SQLCipher es cambiar el `openHelperFactory` del constructor de Room, no
reescribir nada.

### No se hace copia de seguridad de nada

Esto es lo que de verdad cambia con este ADR, y no estaba decidido: el manifiesto venia con
`android:allowBackup="true"` de la plantilla de Android Studio y con los dos ficheros de reglas
vacios, tal como los genera. Es decir, **en cuanto existiera la base de datos local, el historico
financiero entero se habria subido a la copia de Google sin que nadie lo decidiera**, junto con el
resto del directorio privado.

Ahora `allowBackup` es `false`, y los dos ficheros de reglas excluyen todo de forma explicita,
copia en la nube y transferencia entre dispositivos incluidas. La transferencia entre dispositivos
se excluye aparte porque `allowBackup="false"` no la apaga por su cuenta en Android 12 y
posteriores.

Dos motivos, y el primero es el que hace que esto no cueste nada:

1. **Restaurar la base de datos no aporta nada.** Es un reflejo de la cuenta: al entrar se vuelve
   a bajar. Lo unico que se consigue copiandola es sacar el historico del telefono.
2. **El refresh token no se puede restaurar aunque se copie.** Esta cifrado con una clave del
   Keystore que nunca sale del dispositivo, asi que en otro telefono seria un texto cifrado que no
   se puede abrir. Copiarlo no restaura la sesion, solo mueve datos de sitio.

## Consecuencias

A favor:

- El historico financiero no sale del telefono por un ajuste que nadie eligio.
- No hay una dependencia nativa ni una clave mas que gestionar.
- La proteccion que hay es la que de verdad cubre el caso realista: el cifrado del sistema para el
  telefono bloqueado y el cerrojo local para el desbloqueado.

En contra, y hay que saberlo:

- **Cambiar de telefono obliga a entrar otra vez**, y la base local se reconstruye desde cero.
  Para una aplicacion con cuenta es lo normal, pero es un paso que el usuario nota.
- **Sin copia no hay red de seguridad local.** Si alguien desinstala la aplicacion con escrituras
  pendientes de subir, esas escrituras se pierden. Lo mitiga la cola del
  [issue 16](https://github.com/eabl0306/gastonomo/issues/16), que sube en cuanto hay red; no lo
  elimina. Conviene tenerlo presente al disenar esa cola.
- **En un dispositivo rooteado los datos estan en claro**, dentro de lo que el sistema ya no
  protege. Es el limite de esta decision y esta puesto a proposito.
- **`allowBackup="false"` es una decision que se revierte sola si alguien regenera el manifiesto**
  desde una plantilla. Por eso los dos ficheros de reglas llevan tambien las exclusiones escritas:
  aunque alguien vuelva a activar la copia, no se copia nada.
