# ADR 0014: La sesion del cliente: cerrojo local, refresco serializado y almacenamiento cifrado

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

El [ADR 0008](0008-autenticacion-con-tokens.md) decide como emite la API los tokens: un access
JWT de 15 minutos y un refresh opaco de 30 dias que **rota** en cada uso. Rotar tiene una
consecuencia que el cliente no puede ignorar: presentar un refresh token ya rotado es, para la
API, la senal de que alguien tiene una copia, asi que **revoca la familia entera** y echa al
usuario de todos sus dispositivos.

El rediseno anade dos cosas mas que parecen de sesion y no lo son del todo:

- Una pantalla de **desbloqueo con PIN o huella** (la 4), que sale al abrir la aplicacion
  cuando el usuario la ha activado.
- Un interruptor en Ajustes para activarla.

El handoff no dice que hacen contra la API, porque no hacen nada contra la API: el contrato no
tiene endpoint de PIN ni de huella.

Quedaban entonces tres preguntas sin decidir en el cliente: que protege el PIN, como se evita
la condicion de carrera del refresco, y donde se guarda un refresh token que vale 30 dias.

## Decision

### El PIN y la huella son un cerrojo local

No son un factor de autenticacion contra la API: protegen **el refresh token que ya esta
guardado en este dispositivo**. La API no sabe que existen, no se sincronizan entre
dispositivos y desactivarlos no cambia nada en la cuenta.

Lo que implica, y es lo que hace la implementacion:

- El PIN se guarda hasheado con PBKDF2-HMAC-SHA256 y una sal por dispositivo
  (`PinHasher`). Cuatro digitos son diez mil combinaciones, asi que el hash **no** protege de
  una busqueda exhaustiva; lo unico que evita es que quien lea el almacenamiento lo lea en
  claro. La proteccion de verdad la da el Keystore, que es quien guarda el token que el PIN
  abre.
- La huella se pide con `androidx.biometric`, y por eso la unica actividad es una
  `FragmentActivity`: el dialogo del sistema no se puede ensenar desde una `ComponentActivity`.
- El dialogo acepta **solo biometria**, nunca la credencial del dispositivo. El boton de salida
  dice "Usar PIN" y ese PIN es el de cuatro digitos de la aplicacion, el del teclado que hay
  debajo. Permitir el patron del movil seria una tercera forma de entrar que no es el cerrojo,
  que no pasa por el contador de intentos y que nadie configuro aqui.
- Cerrar sesion borra el cerrojo ademas del token. Un PIN que sigue puesto sin sesion detras
  solo confunde.
- La pantalla de desbloqueo lleva un contador de intentos del **dispositivo**, no de la cuenta:
  la API no ve nada de esto. Al agotarlo se cierra la sesion local y se vuelve a Login.

### Los refrescos van de uno en uno

`TokenRefresher` tiene un `Mutex` y **solo una llamada a `POST /v1/auth/refresh` esta en vuelo
a la vez**. No es una optimizacion, es la unica forma de no auto expulsarse: si dos peticiones
fallan con 401 al mismo tiempo y las dos refrescan, la segunda presenta el token que la
primera acaba de rotar y la API revoca la familia.

El detalle que hace que esperar no cueste un refresco de mas: quien entra al mutex pasa el
token que la API le rechazo. Si mientras esperaba otra peticion ya refresco, el token guardado
sera distinto del rechazado y se usa tal cual, sin gastar otra rotacion.

Y si el refresco falla, se **limpia la sesion**. El refresh token guardado ya no sirve, y
dejarlo solo invita a presentarlo otra vez, que es exactamente lo que revoca la familia.

### El token se guarda con una clave del Keystore

DataStore Preferences para el almacenamiento y una clave AES-256-GCM del Android Keystore para
el contenido (`KeystoreCipher`). La clave nunca sale del Keystore: el proceso solo maneja un
alias. El formato guardado es `base64(iv):base64(cifrado)`, con el IV delante porque GCM
necesita uno distinto por escritura.

Se descarto `EncryptedSharedPreferences` (`androidx.security:security-crypto`): esta obsoleta y
su ultima version estable arrastra Tink. Con DataStore mas Keystore el cifrado son treinta
lineas y no hay una dependencia que se quede vieja.

### El idioma y la moneda si son de la cuenta

Conviene decirlo aqui porque se confunde con lo anterior: la moneda principal y el idioma
**viajan a la API** y se sincronizan entre dispositivos. El ojo que oculta los importes y el
cerrojo se quedan en el telefono. Son dos objetos distintos en el dominio, `UserSettings` y
`DeviceSettings`, para que no haya duda de cual es cual.

## Consecuencias

A favor:

- La condicion de carrera que echaba al usuario de todos sus dispositivos no puede ocurrir, y
  no depende de que quien escriba la siguiente pantalla se acuerde.
- El refresh token en reposo esta cifrado con una clave que no esta en el proceso.
- El cerrojo local se puede activar, desactivar y olvidar sin tocar la cuenta ni pedir red.
- Que sea local se ve en el codigo: `LocalLock` y `BiometricLock` son puertos del dispositivo y
  no pasan por ningun gateway HTTP.

En contra, y hay que saberlo:

- **De `KeystoreCipher` y `AndroidBiometricLock` no se ejecuta todo en la JVM.** Robolectric no
  trae proveedor `AndroidKeyStore` ni sensor de huella, asi que cifrar de verdad y ensenar el
  dialogo hay que verificarlos con `just android-connected` cuando haya un emulador. Hoy no lo
  hay, y las dos clases estan excluidas del calculo de cobertura de `:data:session`. Esto decia
  "no se han ejecutado nunca, en la JVM no hay nada que probar", y esa frase costo un cierre en
  produccion: construir el dialogo valida la combinacion de textos y autenticadores, **no
  necesita sensor**, y la que se pedia era invalida. La frontera de lo que no se puede probar es
  mas estrecha de lo que parece, y lo que cae fuera hay que sacarlo de la clase excluida; el
  dialogo se construye ahora en `UnlockPrompt.kt`, que no esta excluido y tiene su test.
- **Serializar los refrescos serializa tambien la espera.** Varias peticiones que fallan con
  401 a la vez se quedan en cola detras de un solo refresco. Con un access token de 15 minutos
  pasa pocas veces y es el precio correcto.
- **Un PIN de cuatro digitos es debil por definicion.** Quien tenga el dispositivo
  desbloqueado y tiempo lo encuentra. Lo que no puede es leer el token, que es lo que importa.
- **Perder el PIN obliga a entrar con la contrasena.** No hay recuperacion del cerrojo, porque
  no hay nada que recuperar: el cerrojo no guarda datos, solo tapa el token.
- **El contador de intentos vive en memoria**, en el ViewModel de la pantalla de desbloqueo,
  asi que matar la aplicacion y volver a abrirla lo pone a cero. Agotarlo cierra la sesion y
  borra el token del dispositivo, que es la parte que si es permanente; volver a intentarlo
  desde cero solo sirve para seguir adivinando un PIN que no abre nada.
