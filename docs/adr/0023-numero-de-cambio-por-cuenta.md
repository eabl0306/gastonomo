# ADR 0023: Un numero de cambio por cuenta, y no una marca de tiempo

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

Con el offline montado ([ADR 0019](0019-leer-de-la-base-local.md),
[ADR 0020](0020-cola-de-escrituras-pendientes.md)), cada arranque tiene que ponerse al dia con lo
que haya pasado en otros dispositivos. Bajarse el historico entero funciona con cien movimientos y
no funciona con tres anos de uso.

Hace falta un endpoint que diga "esto ha cambiado desde la ultima vez", y para eso hace falta una
marca que el cliente guarde entre arranques.

El caso es mas facil de lo normal porque el historico es de **solo anadir**: los movimientos, los
prestamos y los abonos no se editan ni se borran nunca. "Cambios" es casi siempre "cosas creadas
despues de esta marca". No hay conflictos, no hay tombstones, no hay versiones que comparar.

Casi. Mutan las categorias, que se renombran, se archivan y se recuperan
([ADR 0022](0022-archivar-categorias.md)), y los ajustes.

## Decision

`GET /v1/changes?since=<cursor>&limit=` devuelve un lote con las cinco colecciones y el cursor al
que llega.

### El cursor es un contador por cuenta, no una fecha

Una marca de tiempo falla de dos formas, y las dos son silenciosas:

- **Dos filas creadas en el mismo milisegundo.** El cliente pide "desde T", recibe una, guarda T y
  la otra no vuelve a salir nunca.
- **Un reloj que se ajusta hacia atras.** Una fila nueva se guarda con una fecha anterior al cursor
  del cliente y queda por debajo para siempre.

Saltarse un movimiento en una aplicacion de finanzas es un balance que no cuadra y ninguna pista de
por que. No es un fallo que se pueda descubrir mirando: nada avisa.

Asi que cada fila lleva un **numero de cambio** unico por usuario, y el cursor es ese numero. Sale
de un contador por usuario, `user_change_counters`, que se reparte con un `UPDATE ... RETURNING`.

La propiedad que hace que esto funcione no es que sea monotono, es el **cerrojo**: el `UPDATE` toma
un cerrojo de fila, asi que dos escrituras del mismo usuario se serializan y **cuando el numero N
es visible, ninguno menor que N esta todavia por confirmar**. Un cliente que va por N no puede
perderse nada.

Con una secuencia global de PostgreSQL eso no seria cierto. Dos transacciones pueden coger 5 y 6 y
confirmar en orden inverso: un cliente que lee cuando 6 ya esta y 5 aun no, guarda 6 y se acaba de
comer la fila 5. Es el fallo clasico de usar una secuencia como cursor de replicacion, y es
exactamente el fallo que este endpoint no se puede permitir.

### Lo reparte un disparador, no el codigo

Hay cinco caminos de escritura hoy y habra mas. Un disparador no se puede olvidar en el sexto.

Esto no es una regla de negocio metida en la base de datos: es una propiedad de como se guardan las
filas, igual que un `DEFAULT` o un `CHECK`. El nucleo no sabe que existe, y de hecho ni el
repositorio ni el caso de uso mencionan `change_seq` al escribir.

El disparador va en `INSERT` para las cuatro tablas del historico y tambien en `UPDATE` para
categorias y ajustes, que son las dos cosas que mutan. Renombrar una categoria es un cambio que el
cliente tiene que ver.

### El cursor viaja opaco

Es una cadena codificada, con una version dentro. El `nextCursor` de movimientos es legible
(`YYYY-MM-DD_uuid`) y eso invita a construirlo a mano; un cursor fabricado es un cliente que se
salta cambios sin enterarse.

Un cursor que no sea de esta API responde **422**, y no se lee como "desde el principio". Tratarlo
como arranque en frio haria que el cliente se bajara el historico entero sin saber por que, que es
un fallo silencioso disfrazado de lentitud.

### El lote es un tramo cerrado del orden total

El cursor de la respuesta no es "el numero mas alto que te mande": es el numero **hasta el que el
lote esta completo**. Se calcula juntando los numeros de las cinco consultas y cortando en el que
hace `limit`. Todo lo que tenga numero menor o igual esta dentro.

Eso es lo que permite que un solo cursor sirva para cinco colecciones. Si cada una llevara el suyo,
un cliente con cinco cursores tendria cinco formas de quedarse a medias.

Se pide `limit + 1` filas de cada tabla y se descarta lo que sobra. Es suficiente: si una fila cabe
entre las `limit` primeras del orden global, tambien cabe entre las `limit + 1` primeras de su
propia tabla.

### El cliente avanza su cursor despues de escribir

Nunca al recibir el lote. Si la escritura local falla y el cursor ya subio, ese lote se pierde para
siempre y nada lo vuelve a mandar.

### En el cliente: el cursor vive con los datos

El cursor se guarda en la **misma base local** que los datos, en una tabla de una fila. Si viviera
en otro fichero, los dos podrian quedarse en versiones distintas y el dispositivo creeria tener algo
que no tiene.

Al abrir la aplicacion se pide solo lo nuevo. Un fallo se ignora: la aplicacion pinta de la base
local, asi que no poder sincronizar no impide abrirla.

La **resincronizacion completa** esta en Ajustes, al final del grupo Datos, y pide confirmacion.
Tres cosas la hacen segura:

- **Si hay escrituras sin subir, no borra nada.** Lo comprueba antes de tocar la base y la hoja lo
  dice. Borrar lo local con cosas sin subir es perder movimientos del usuario, que es el peor fallo
  posible de esta aplicacion.
- **Lo que borra es solo lo ya subido.** Los `DELETE` llevan `pending = 0`, igual que el borrado por
  periodo. Es la segunda barrera para lo mismo.
- **Se puede cortar a la mitad.** La marca de "resincronizando" se guarda antes de borrar y solo se
  quita al acabar, asi que si la aplicacion muere nadie confunde medio historico con el historico
  entero: la fila de Ajustes lo dice y el siguiente arranque la termina, que es seguir pidiendo desde
  donde se quedo.

Si la aplicacion muere entre marcar y borrar, el cursor ya es `null`, que significa "bajalo todo": el
caso se arregla solo.

## Consecuencias

- Una migracion numera las filas que ya existen por `(created_at, id)`. Da igual que no sea el
  orden real de creacion al milisegundo: lo unico que importa es que sea un orden total y que nadie
  lo vuelva a calcular.
- Cinco tablas tienen una columna mas y un indice unico por `(user_id, change_seq)`. Sin el indice,
  dos filas podrian compartir numero y el cursor no podria pararse entre ellas.
- Las escrituras de un mismo usuario se serializan en el reparto del numero. Con un usuario y uno o
  dos telefonos el cerrojo no se nota; si algun dia hubiera escrituras concurrentes de verdad, eso
  es lo primero que habria que medir.
- Los huecos son posibles (una transaccion revertida se lleva su numero) y no molestan: el cursor
  compara, no cuenta.
- El endpoint es de **solo lectura**. No hay `PATCH` ni `DELETE` de movimientos, prestamos ni
  abonos, y este endpoint no es una puerta para provocar cambios, solo para verlos.

## Alternativas descartadas

**Cursor compuesto `(created_at, id)`.** Arregla el empate en el mismo milisegundo, que es el caso
frecuente, y no arregla el reloj hacia atras, que es el caso raro y el que deja el balance mal sin
que nada avise. Sale mas barato de implementar (no hay migracion de datos ni disparadores) y por eso
estuvo sobre la mesa.

**Una secuencia global de PostgreSQL.** Mas simple, pero no da la garantia: dos transacciones pueden
confirmar en orden inverso al de la secuencia y el cliente se salta la que confirmo tarde.

**Un log de cambios en su propia tabla.** Es lo que haria falta si hubiera borrados de verdad, para
guardar los tombstones. Aqui no los hay, asi que una columna por tabla cuesta menos y no puede
desincronizarse del dato.
