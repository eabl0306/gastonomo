# ADR 0011: Cada registro guarda cuatro campos de importe y la tasa es del usuario

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

El rediseno permite apuntar un gasto en una moneda distinta de la principal. La pantalla de
Nuevo movimiento lo hace asi: el usuario teclea el importe, elige la moneda, y si no es su
moneda principal aparece una caja de conversion con la equivalencia calculada a la tasa
actual **y editable**.

Que la caja sea editable no es un detalle de la pantalla: es la regla del dominio. El
usuario cambia 50 dolares en una casa de cambio a una tasa que no es la del BCE, y lo que
quiere apuntar es lo que le costo de verdad, no lo que deberia haberle costado.

Ademas, el usuario puede cambiar su moneda principal en Ajustes. El handoff es explicito:
cambiarla **no toca ningun registro**, solo cambia como se muestran las cifras y que moneda
se propone la proxima vez.

Eso deja tres preguntas que hay que responder juntas, porque cualquier respuesta a una
condiciona las otras dos:

1. Que se guarda de un gasto en otra moneda.
2. Quien convierte, y cuando.
3. Que pasa con los registros viejos cuando cambia la moneda principal.

Las alternativas consideradas fueron estas.

**Guardar solo lo que el usuario tecleo** (`amount` y `currency`) y convertir siempre al
mostrar. Es la mas simple y esta mal: perderia la tasa que el usuario edito a mano, y un
gasto de hace dos anos se recalcularia con la tasa de hoy. El historico cambiaria solo, que
es lo peor que puede hacer una aplicacion de finanzas.

**Guardar solo el convertido** a la moneda principal. Pierde lo que el usuario tecleo, asi
que la pantalla no puede ensenar "50 USD" nunca mas, y al cambiar la moneda principal todos
los registros estarian expresados en una moneda que ya no es la del usuario.

**Guardar lo tecleado mas la tasa** (`amount`, `currency`, `rate`). Funciona, y es lo mismo
que la decision tomada con una multiplicacion de mas en cada lectura. Se descarto porque la
tasa no dice en que moneda esta el resultado: haria falta un cuarto campo igual, y entonces
es mas claro guardar el resultado que la tasa.

## Decision

Cada registro de dinero (movimiento, prestamo y abono) guarda **cuatro campos**:

```
amount + currency        lo que el usuario tecleo, en la moneda que eligio
base   + base_currency   su equivalencia en la moneda principal del momento
```

Y tres reglas alrededor:

### La API guarda los cuatro campos tal cual y no recalcula nada

`base` es lo que el cliente manda: la equivalencia que el usuario acepto o edito en la caja
de conversion. La API no consulta tasas al guardar un movimiento. Ni una vez.

Esto es deliberado y tiene una consecuencia que conviene ver de frente: **la API no puede
comprobar que `base` sea razonable.** Un cliente que mande `amount: "50" currency: "USD"` y
`base: "1" baseCurrency: "COP"` obtiene un registro absurdo y la API lo acepta. La
alternativa seria validar contra la tasa del dia con algun margen, y eso rompe el caso que
justifica todo esto: la tasa de la casa de cambio puede estar lejos de la del BCE, y
decidir cuanto es "demasiado lejos" es inventarse una regla que el diseno no pide.

Lo unico que si se valida es la coherencia interna, y lo hace el value object
`MonetaryAmount` del dominio: los dos importes tienen que ser positivos, y **si las dos
monedas son la misma, los dos importes tienen que coincidir**. Un registro con
`currency = base_currency` y `amount != base` no tiene interpretacion posible, asi que se
rechaza con 422. Lo vigilan tambien los CHECK de las tres tablas, para que no entre por
otra puerta.

### `base` y `baseCurrency` se pueden omitir los dos juntos

Es el caso del 95% de los registros: el usuario teclea en su moneda principal. Omitirlos
significa que la equivalencia es el propio importe, y lo monta
`MonetaryAmount::single_currency`. Mandar uno solo de los dos es un 422 sobre `base`, con el
mensaje "La equivalencia necesita importe y moneda.".

Esto vale igual en los tres endpoints que crean dinero. Al principio era asi solo en
`POST /v1/movements` y en prestamos y abonos los dos campos eran obligatorios; esa asimetria
no la pedia el diseno y hacia tropezar a quien implementara un cliente nuevo, asi que se
igualo.

### Convertir para mostrar es del cliente

La API **nunca** devuelve importes ya convertidos. Devuelve los cuatro campos y las tasas
por separado, en `GET /v1/exchange-rates`.

La razon es que el mismo registro se ve distinto segun la moneda principal que tenga el
usuario en ese momento, y eso es presentacion. Un movimiento de 50 USD guardado cuando la
moneda principal era COP tiene `base` en pesos; si manana el usuario pone el euro como
principal, la pantalla tiene que ensenar euros, y quien sabe eso es el cliente que tiene la
moneda principal y las tasas delante.

De ahi salen tambien estas cuatro cosas, que el cliente deriva y la API no guarda ni
calcula:

- El estado de un prestamo (Pagado, Parcial, Pendiente) y su porcentaje.
- Los "Intereses ganados", que son el exceso de los abonos sobre el prestamo.
- El orden de la lista de prestamos (abiertos primero, pagados al final).
- Los totales, el grafico de tendencia y el de gastos por categoria.

Todas dependen de comparar importes en monedas distintas, o sea de las tasas del momento.

### Cambiar la moneda principal no toca ningun registro

`PATCH /v1/settings` con `displayCurrency` cambia una fila de `user_settings` y nada mas. No
hay migracion de datos, no hay recalculo, no hay trabajo de fondo. Los registros viejos
siguen con la `base` que tenian, en la moneda que tenian.

Eso significa que despues de cambiar de COP a EUR hay movimientos con `baseCurrency = COP` y
otros con `baseCurrency = EUR`, y el cliente convierte los primeros al mostrar. Es
exactamente lo que se quiere: el dato historico no se toca, y la presentacion se adapta.

## Consecuencias

A favor:

- Lo que el usuario tecleo se conserva para siempre, con su moneda.
- La tasa que el usuario acepto o edito se conserva para siempre, implicita en `base`.
- El historico no cambia nunca solo. Un gasto de hace dos anos ensena lo que ensenaba.
- Cambiar la moneda principal es instantaneo y reversible: es una fila.
- La API no depende de tener tasas para poder guardar un gasto. Con Frankfurter caido y el
  catalogo vacio, registrar movimientos sigue funcionando.
- El nucleo no necesita conocer tasas para validar un registro.

En contra, y hay que saberlo:

- **La API no puede detectar una `base` absurda.** Es el precio de dejar que el usuario
  ponga su tasa. Si algun dia molesta, el sitio seria una validacion con un margen amplio
  (por ejemplo, rechazar lo que se desvie mas de un orden de magnitud de la tasa del dia),
  pero eso hace que guardar un movimiento dependa de tener tasas, que es justo lo que hoy no
  pasa.
- **Los clientes tienen que implementar la conversion.** Los tres. Es duplicacion real, y la
  alternativa era duplicar la moneda principal en cada respuesta y que la API se hiciera
  responsable de la presentacion.
- **Las cuatro vistas derivadas necesitan el periodo completo.** La tarjeta de balance, la
  tendencia, los gastos por categoria y el resumen de tres columnas se calculan sobre la
  lista de movimientos, y la lista pagina a 500. Para "Este ano" el cliente tiene que agotar
  el cursor antes de dibujar el primer grafico. No hay endpoint de agregados, y anadirlo
  seria un `GET /v1/movements/summary`; se deja fuera porque para una aplicacion personal el
  volumen no lo justifica.
- **Un registro puede quedar en una moneda que ya no se ofrece.** Por eso `currencies` no se
  borra nunca: se marca `offered = false`. El registro sigue mostrandose con la ultima tasa
  conocida.
- Cuatro columnas por importe en tres tablas es mas esquema que dos. Es barato y explicito.
