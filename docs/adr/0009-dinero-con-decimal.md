# ADR 0009: El dinero es decimal exacto, nunca coma flotante

- Estado: aceptado
- Fecha: 2026-09-26

## Contexto

Gastonomo es una aplicacion de finanzas personales. El usuario apunta gastos e ingresos y
compara el balance con lo que ve en su banco. Si los numeros no cuadran al centimo, la
aplicacion no sirve para lo unico que hace.

El prototipo del rediseno usa `number` de JavaScript, que es un IEEE 754 de doble
precision. Eso esta bien en un prototipo y no vale en produccion:

```
0.1 + 0.2                  = 0.30000000000000004
186400.15 + 0.1            se redondea distinto segun el orden de la suma
```

Con importes en pesos colombianos (cifras de seis o siete digitos) el error no aparece a
la primera, pero si aparece: un balance que suma 300 movimientos acumula el error de 300
sumas. Y aqui hay un multiplicador extra: cada registro se convierte a la moneda
principal con una tasa, asi que hay multiplicaciones por numeros como `3316.44` y
divisiones por `0.87732`.

Complicacion adicional del dominio: **el numero de decimales depende de la moneda y de si
es un importe tecleado o una equivalencia.** El peso colombiano se ensena sin decimales;
el euro con dos. Pero un peso colombiano convertido desde dolares no es un entero, y
truncarlo a la moneda de destino al guardar perderia informacion que luego hace falta para
que el saldo de un prestamo cuadre.

## Decision

El dinero es un decimal exacto de extremo a extremo, y cada capa lo representa con el tipo
exacto de su tecnologia.

### PostgreSQL: `NUMERIC(18,4)`

`NUMERIC` en PostgreSQL es decimal exacto, no binario. 18 digitos en total y 4 decimales:

- 14 digitos enteros aguantan 99 billones de pesos colombianos, que sobra para una
  aplicacion personal por mucho que se devalue la moneda.
- **4 decimales, y no 2, por las equivalencias.** La moneda que se ensena puede tener 0 o
  2 decimales, pero el valor convertido que se guarda en `base` necesita mas precision
  para que la suma de abonos cuadre con el prestamo. Cuatro decimales es lo que usan las
  tablas de tasas y es suficiente para que el error de redondeo quede por debajo de lo que
  cualquier pantalla muestra.

Los CHECK de las tablas exigen `amount > 0 AND base > 0`. No hay importes negativos: el
signo lo lleva `kind`, que es `in` u `out`. Un gasto de 100 es `kind = 'out'` con
`amount = 100`, no `amount = -100`. Asi ninguna consulta tiene que acordarse del signo, y
"gastos del mes" es un `WHERE kind = 'out'`.

### Rust: `rust_decimal::Decimal` detras de un value object

`rust_decimal::Decimal` es decimal de 96 bits con escala, y SQLx lo mapea a `NUMERIC`
directamente con la feature `rust_decimal`.

No se usa desnudo. El dominio expone `gastonomo_domain::Money`, que envuelve el `Decimal`
y **no lo deja salir**. Lo unico que se puede hacer con un `Money` es lo que el dominio
permite: leerlo, sumarlo con otro, compararlo, preguntarle si es positivo y pedirle su
texto. No hay `From<f64>` ni `as_f64()`, y esa ausencia es la decision: mientras no exista
la conversion, nadie puede meter un float por descuido.

`rust_decimal` va **sin la feature `serde`**. Es a proposito y lo comprueba un test: el
crate `domain` no puede alcanzar `serde`, porque el nucleo no serializa nada
(`crates/api/tests/architecture.rs` lee `Cargo.lock` y falla si aparece). La serializacion
la hace el adaptador HTTP, a mano, en `crates/http/src/serialization.rs`.

### JSON: string, nunca number

```json
{ "amount": "186400.0000", "currency": "COP", "base": "186400.0000", "baseCurrency": "COP" }
```

Un `186400.0000` sin comillas termina en un `double` en cuanto el cliente lo parsea, y da
igual lo cuidadosa que sea la API: `JSON.parse` en la web y `Gson`/`kotlinx.serialization`
en Android lo convierten a coma flotante antes de que el codigo del cliente lo vea. Con
comillas, el cliente recibe un texto y decide: `BigDecimal` en Kotlin,
`decimal.js` o enteros de centimos en la web.

Los importes salen con los cuatro decimales de la base, tal cual. No se recortan a los
decimales de la moneda porque **cuantos decimales se ensenan es una decision de
presentacion** y depende de la moneda principal del usuario en ese momento, que la API no
usa para nada al responder. El dominio tiene `CurrencyDecimals` para quien lo necesite,
pero el que formatea es el cliente.

Las entradas tambien son string, y se parsean con `Money::parse`. Un cuerpo con
`"amount": 186400` se rechaza con 422.

## Consecuencias

A favor:

- Los numeros cuadran. Se cargaron por HTTP los datos semilla del prototipo (5 prestamos,
  9 abonos, 19 movimientos) y los saldos de prestamos y los totales de septiembre salen
  exactos.
- El tipo hace el trabajo: no existe el camino que convierte a float, asi que no hay que
  recordar no usarlo.
- `NUMERIC` deja que PostgreSQL sume y agregue sin perder precision, si algun dia hace
  falta un endpoint de agregados.

En contra:

- `NUMERIC` es mas lento que `double precision` y ocupa mas. Con los volumenes de una
  aplicacion personal es irrelevante.
- Serializar a mano es mas codigo que poner `#[serde(with = ...)]`. A cambio, el nucleo no
  depende de `serde` y eso lo vigila un test.
- Los cuatro decimales se ven en el JSON (`"186400.0000"`), lo que es algo feo para quien
  lea la respuesta a mano. Recortarlos en la API obligaria a la API a saber la moneda
  principal del usuario en cada respuesta, que es justo lo que este contrato evita.
- Los clientes tienen que usar un tipo decimal de verdad. Si alguno parsea el string a
  `Double`, la precision se pierde igual. Eso no lo puede impedir la API; lo unico que
  puede hacer es no facilitarlo, y por eso el string.
