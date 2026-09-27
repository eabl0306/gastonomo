# ADR 0021: Que se degrada sin conexion, y como se cuenta

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

La fila de Ajustes decia **"Sincronizacion en la nube: al dia"** y ese texto estaba fijo. No miraba
nada. Es lo que destapo que la aplicacion tenia que funcionar sin conexion, porque la frase
prometia algo que no se cumplia.

Con el [ADR 0019](0019-leer-de-la-base-local.md) y el
[ADR 0020](0020-cola-de-escrituras-pendientes.md) ya se lee y se escribe sin red. Queda lo que
faltaba: **decir la verdad** y **degradar lo que no se puede hacer sin red**, en vez de fallar.

## Decision

### La fila dice lo que de verdad pasa

Cuatro estados, derivados de dos cosas que el usuario no puede ver por su cuenta: cuantas
escrituras siguen en el telefono y si hay red.

| Estado        | Cuando                              |
| ------------- | ----------------------------------- |
| Al dia        | Nada pendiente y la API responde    |
| Sincronizando | Hay cola y hay red                  |
| N sin enviar  | Hay cola y no hay por donde subirla |
| Sin conexion  | Sin red y sin nada pendiente        |

**Se deriva, no se guarda.** Guardarlo permitiria que la fila dijera una cosa mientras la cola dice
otra, que es exactamente como empezo esto.

"N sin enviar" va en ambar, no en el gris de los demas: es lo que el usuario querria saber antes de
desinstalar la aplicacion o cambiar de telefono.

La fila se puede tocar siempre, y lleva a la lista de registros sin enviar, que **intenta subir al
abrirse**. Tocarla es, entonces, forzar un intento; y con realimentacion, que es mejor que un
reintento silencioso.

### Si hay red se pregunta al sistema, no a un fallo

`NetworkMonitor` es un puerto y el adaptador mira `NET_CAPABILITY_VALIDATED`, no solo que exista
una red: un wifi de cafeteria con portal cautivo esta conectado y no llega a ninguna parte, y para
el usuario eso es estar sin conexion.

Hace falta preguntarlo, y no deducirlo de un fallo, porque hay dos cosas que tienen que saberlo
**antes** de que el usuario toque nada: la fila de Ajustes y el chip de moneda.

El permiso `ACCESS_NETWORK_STATE` se declara en `:data:session`, el modulo que lo usa, y no en la
aplicacion: asi quien lea el adaptador ve de donde sale, y quitarlo se lleva el permiso con el.

### La multimoneda se degrada, no se rompe

Las tasas vienen de la API. Sin red no hay tasas nuevas, y **guardar un importe en otra moneda sin
conocer su equivalencia romperia la regla de los cuatro campos**, que es la base del modelo de
dinero. Entonces, sin conexion:

- El chip de moneda de los tres formularios sale apagado, fijo en la moneda principal.
- Con una explicacion corta debajo, no simplemente muerto. Esa es la diferencia entre degradar y
  estar roto.
- La caja de conversion no aparece: no hay nada que convertir.
- Lo ya guardado se sigue viendo con las ultimas tasas bajadas, y Ajustes dice de cuando son.

De ahi sale gratis que los cuatro campos queden coherentes: si no se puede elegir otra moneda, la
equivalencia es el propio importe y `base` es igual a `amount`.

**Un 503 de la API se comporta igual que no tener red.** Distinguirlos importa para diagnosticar
(uno es del telefono y el otro del servidor), pero para el usuario acaban en lo mismo: no hay tasas
de confianza con las que convertir.

### La decision se toma al abrir el formulario

No en cada tecla. Es coherente con el modelo: la equivalencia que se guarda es la del momento de
registrar, no la de hoy. Si la red se cae con el formulario abierto, lo que se teclo se guarda con
la equivalencia que se calculo al abrirlo, que es justo lo que dice el ADR 0011.

## Consecuencias

A favor:

- La fila de Ajustes deja de mentir, que era el punto de partida de toda la epica.
- Registrar sin conexion no puede producir un registro con una equivalencia inventada.
- Quien se quede sin red lo ve explicado, no se encuentra un control que no responde.

En contra, y hay que saberlo:

- **El chip no se reactiva solo si vuelve la red con el formulario abierto.** Hay que cerrarlo y
  abrirlo. Es el precio de decidirlo una vez, y es lo coherente con guardar la equivalencia del
  momento de registrar.
- **Con red mala pero existente, el monitor dice que hay conexion.** `VALIDATED` descarta el portal
  cautivo, no una red lenta. Lo que ocurra entonces se vera como un fallo de cada peticion, que es
  el comportamiento de siempre.
- **No se avisa al cerrar sesion con cosas pendientes.** Sigue siendo el agujero que dejo el ADR 0020.
- **Nada de esto se ha visto en un telefono.** Sin emulador aqui, lo que hay son tests: los cuatro
  estados, el chip apagado en modo avion y el 503 comportandose igual.
