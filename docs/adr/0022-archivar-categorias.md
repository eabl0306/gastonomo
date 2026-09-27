# ADR 0022: Archivar una categoria en vez de borrarla

- Estado: aceptado
- Fecha: 2026-09-27

## Contexto

`DELETE /v1/categories/{id}` hacia dos cosas en una transaccion: pasaba todos los movimientos de
esa categoria a **"Otros"** y despues borraba la fila.

La primera parte es una escritura sobre movimientos ya registrados. Un gasto de hace dos anos
dejaba de leerse con la categoria que tenia el dia que se creo, y no quedaba rastro de que habia
cambiado: el movimiento no guarda su categoria anterior.

Eso es exactamente lo que la regla del historico de solo anadir existe para impedir. La regla
estaba escrita, respetada en movimientos, prestamos y abonos, y este endpoint la rompia por la
puerta de atras. Nadie lo habia visto porque el endpoint se llama "borrar una categoria" y suena
a que solo toca categorias.

Un segundo efecto, mas visible: las graficas y el resumen por categoria se calculan en el cliente
sobre los movimientos que le llegan. Borrar una categoria con historia movia todo ese gasto al
bloque de "Otros" y cambiaba el pasado en las pantallas de analisis.

## Decision

El endpoint sigue siendo `DELETE`, pero ya no destruye:

- Si **algun movimiento la usa**, la categoria se **archiva**. Sale de la lista y deja de
  ofrecerse al registrar, pero la fila se queda donde estaba y los movimientos siguen apuntando
  a ella.
- Si **no la usa ningun movimiento**, se borra de verdad.

Se anade `POST /v1/categories/{id}/restore`, que la devuelve a la lista, y
`GET /v1/categories?includeArchived=true`, que es como se ven las archivadas.

En la base de datos es una columna: `archived_at timestamptz null`, con un
`CHECK (archived_at IS NULL OR is_default = FALSE)` y un indice parcial para la lista de siempre.
No hay tabla de archivo ni `deleted_at`: el estado es un campo mas del agregado, y archivar y
desarchivar pasan por el mismo `update` que el resto de las ediciones. Un solo camino de
escritura es un sitio menos donde olvidarse de algo.

### El endpoint dice cual de las dos cosas hizo

Responde **200 con `{"outcome": "archived"|"deleted"}`** en vez del 204 vacio de antes.

El cliente tiene que decirle al usuario si la categoria se archivo o se borro, porque no es lo
mismo. Con un 204 tendria que deducirlo de su `movementCount`, que es de la ultima carga de la
lista y puede estar viejo: bastaria registrar un gasto en otra pantalla para que el aviso dijera
"eliminada" de algo que se archivo. Decir algo que no paso es peor que un campo de mas en la
respuesta.

### Por que se borra la que no usa nadie

Una categoria que nunca se uso no tiene historico que proteger: nada la referencia, asi que
borrarla no puede cambiar como se lee un movimiento. Y guardarla archivada tendria coste real:
quien se equivoca al crear una categoria y la borra en el momento se encontraria la lista de
archivadas llena de intentos.

La condicion no se comprueba solo en el caso de uso, tambien va en el `WHERE` del `DELETE`. Entre
contar y borrar puede entrar un movimiento, y entonces quedaria apuntando a una fila que ya no
esta. Si el `DELETE` no borra ninguna fila, la respuesta es 404, no un 200 que miente.

### Por que la recuperacion no es opcional

El nombre es unico entre todas las categorias del usuario, **incluidas las archivadas**. Eso es
deliberado: si "Mascotas" se archiva y se puede crear otra "Mascotas", ya hay dos cosas distintas
con el mismo nombre en la misma lista y ningun movimiento sabe a cual pertenece.

Consecuencia directa: sin `restore`, archivar por error dejaria el nombre cogido para siempre, sin
forma de arreglarlo desde la aplicacion. Por eso las dos cosas entran juntas.

Recuperar una que ya estaba activa responde igual, 200 con la categoria. Archivar una ya archivada
tambien, con `outcome: archived`. Son idempotentes a proposito: un reintento no es un error, y el
cliente reintenta.

### Renombrar sigue siendo libre

`PATCH` puede cambiar el nombre de una categoria con historia, y eso tambien cambia lo que se lee
en un movimiento viejo. Se queda como esta, por dos razones.

La primera es que renombrar es legitimo y frecuente: arreglar una falta, pasar de "Comida" a
"Alimentacion". Tratarlo como un cambio del historico obligaria a archivar la vieja y crear una
nueva cada vez, y la lista de archivadas se llenaria de erratas.

La segunda es que lo que la regla protege son **los importes y las fechas**, que es lo que hace
que las cuentas cuadren. Renombrar no los toca: el mismo gasto, el mismo dia, la misma cantidad,
con otra etiqueta. Borrar si los tocaba, porque movia el gasto de un grupo a otro y cambiaba los
totales por categoria.

Esta es la parte mas discutible de la decision y se deja escrita para poder cambiarla: si algun
dia hace falta, la forma seria guardar el nombre en el movimiento al registrarlo, no prohibir el
`PATCH`.

## Consecuencias

- Ya no queda ningun `UPDATE movements SET category_id` en la API. Era el ultimo camino por el que
  se podia cambiar como se lee el historico, y ahora el adaptador de categorias no puede escribir
  en movimientos ni por accidente.
- El cliente tiene que saber de categorias archivadas. Se resuelven pidiendo
  `?includeArchived=true`, que es lo que hace falta para pintar un movimiento viejo y para la
  seccion de archivadas de la pantalla de Categorias.
- La lista de categorias de una cuenta con anos de uso puede crecer con archivadas. Es una lista
  de decenas de filas en el peor caso de una aplicacion personal; el indice parcial deja la
  consulta de siempre igual de rapida.
- `movementCount` sigue llegando en las archivadas, y es lo que explica al usuario por que no se
  pudo borrar.

## Alternativas descartadas

**Prohibir el borrado y punto.** Coherente con movimientos y prestamos, pero deja la pantalla de
Categorias sin forma de limpiar una categoria que ya no se usa. El diseno tiene el boton, y el
caso de "cree esto sin querer" es real.

**Guardar el nombre y el color en el movimiento al registrarlo.** Resuelve el problema entero,
incluido el de renombrar: el movimiento se lee siempre como se guardo. Se descarta ahora por
tamano, no por ser peor: obliga a migrar todos los movimientos existentes y a decidir que pasa
cuando se edita una categoria (¿cambia solo lo nuevo?). Es la salida si el renombrado se vuelve
un problema de verdad.

**Una tabla `archived_categories`.** Mover la fila a otra tabla obliga a que los movimientos
apunten a dos sitios posibles, o a romper la clave ajena. Una columna nulable no rompe nada.
