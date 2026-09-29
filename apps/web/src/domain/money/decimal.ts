/**
 * Un decimal exacto, escrito a mano y sin dependencias.
 *
 * Hace falta por dos reglas que chocan. El dinero de esta aplicacion es decimal exacto y
 * **nunca un float**: la API lo manda como cadena justamente para que nadie lo meta en un
 * `number` y pierda centavos. Y `src/domain` no puede importar ni un paquete de npm, asi
 * que `big.js` y compania no son una opcion.
 *
 * Lo que queda es `BigInt`, que si es exacto. Un decimal es un entero de unidades mas
 * cuantos decimales tiene: `186400.0000` son 1864000000 unidades con escala 4. Las
 * operaciones alinean escalas antes de operar, asi que sumar `1.5` y `0.25` da `1.75` y no
 * `1.7499999999999998`.
 *
 * Todo el redondeo es **half-up alejandose del cero**, que es el que usa la app de Android
 * (`RoundingMode.HALF_UP`) y el que espera cualquiera que mire un importe: 0.5 sube a 1 y
 * -0.5 baja a -1.
 */
export interface Decimal {
  /** El numero sin coma: `186400.0000` son 1864000000n. */
  readonly units: bigint;
  /** Cuantos decimales tiene. Nunca negativa. */
  readonly scale: number;
}

/**
 * Tope de decimales que se aceptan al leer.
 *
 * El contrato manda importes con 4 decimales y tasas con 10. Una cadena con cien decimales
 * no es un numero que nadie escribiera a proposito, y aceptarla solo sirve para que una
 * respuesta rara se convierta en operaciones con enteros gigantes.
 */
export const MAX_SCALE = 20;

/** Los decimales con los que se divide. Sobran para tasas de 10 decimales encadenadas. */
export const DIVISION_SCALE = 20;

const TEN = 10n;

export const ZERO: Decimal = { units: 0n, scale: 0 };
export const ONE: Decimal = { units: 1n, scale: 0 };

function powerOfTen(exponent: number): bigint {
  return TEN ** BigInt(exponent);
}

/** Un decimal a partir de un entero. Para constantes del dominio, no para importes. */
export function fromInteger(value: number): Decimal {
  return { units: BigInt(Math.trunc(value)), scale: 0 };
}

/**
 * Lee un decimal de una cadena. Devuelve null si no lo es.
 *
 * No acepta notacion exponencial (`1e3`) a proposito: la API no la manda, y aceptarla seria
 * aceptar tambien `Infinity` y `NaN` por la puerta de atras.
 */
export function parseDecimal(raw: string): Decimal | null {
  const text = raw.trim();
  const match = /^([+-]?)(\d+)(?:\.(\d+))?$/u.exec(text);
  if (match === null) return null;
  const sign = match[1] ?? '';
  const whole = match[2] ?? '0';
  const fraction = match[3] ?? '';
  if (fraction.length > MAX_SCALE) return null;
  const digits = BigInt(`${whole}${fraction}`);
  return { units: sign === '-' ? -digits : digits, scale: fraction.length };
}

/**
 * Lo mismo, pero lanza. Es para constantes escritas en el codigo, donde una cadena que no
 * es un decimal es un error de quien la escribio y no un dato de fuera.
 */
export function decimalOf(raw: string): Decimal {
  const parsed = parseDecimal(raw);
  if (parsed === null) throw new Error(`No es un decimal: '${raw}'`);
  return parsed;
}

/** Sube la escala sin perder nada. Bajarla es redondear, y eso es [roundDecimal]. */
function withScale(value: Decimal, scale: number): Decimal {
  if (scale === value.scale) return value;
  return { units: value.units * powerOfTen(scale - value.scale), scale };
}

function alignedUnits(a: Decimal, b: Decimal): { a: bigint; b: bigint; scale: number } {
  const scale = Math.max(a.scale, b.scale);
  return { a: withScale(a, scale).units, b: withScale(b, scale).units, scale };
}

export function addDecimals(a: Decimal, b: Decimal): Decimal {
  const aligned = alignedUnits(a, b);
  return { units: aligned.a + aligned.b, scale: aligned.scale };
}

export function subtractDecimals(a: Decimal, b: Decimal): Decimal {
  const aligned = alignedUnits(a, b);
  return { units: aligned.a - aligned.b, scale: aligned.scale };
}

/** El producto exacto: las escalas se suman, asi que no se pierde ni un digito. */
export function multiplyDecimals(a: Decimal, b: Decimal): Decimal {
  return { units: a.units * b.units, scale: a.scale + b.scale };
}

export function negateDecimal(value: Decimal): Decimal {
  return { units: -value.units, scale: value.scale };
}

export function absDecimal(value: Decimal): Decimal {
  return value.units < 0n ? negateDecimal(value) : value;
}

/** -1, 0 o 1. Es lo que se usa para saber si un importe es cero o negativo. */
export function signOf(value: Decimal): number {
  if (value.units === 0n) return 0;
  return value.units < 0n ? -1 : 1;
}

export function isZero(value: Decimal): boolean {
  return value.units === 0n;
}

/** Negativo si a es menor, 0 si son iguales, positivo si a es mayor. */
export function compareDecimals(a: Decimal, b: Decimal): number {
  const aligned = alignedUnits(a, b);
  if (aligned.a === aligned.b) return 0;
  return aligned.a < aligned.b ? -1 : 1;
}

export function decimalsAreEqual(a: Decimal, b: Decimal): boolean {
  return compareDecimals(a, b) === 0;
}

/** Divide dos enteros redondeando half-up alejandose del cero. */
function divideRounding(numerator: bigint, denominator: bigint): bigint {
  const negative = numerator < 0n !== denominator < 0n;
  const top = numerator < 0n ? -numerator : numerator;
  const bottom = denominator < 0n ? -denominator : denominator;
  const quotient = top / bottom;
  const doubled = (top % bottom) * 2n;
  const rounded = doubled >= bottom ? quotient + 1n : quotient;
  return negative ? -rounded : rounded;
}

/**
 * Redondea a [scale] decimales. Subir la escala es exacto; bajarla redondea half-up.
 *
 * Una escala negativa no tiene sentido para un importe y se trata como cero.
 */
export function roundDecimal(value: Decimal, scale: number): Decimal {
  const target = Math.max(0, Math.trunc(scale));
  if (target >= value.scale) return withScale(value, target);
  const divisor = powerOfTen(value.scale - target);
  return { units: divideRounding(value.units, divisor), scale: target };
}

/**
 * `a / b` con [scale] decimales. Devuelve null si [b] es cero, para que quien llama pueda
 * caer a su plan B en vez de recibir un `Infinity` que se cuele en un importe.
 */
export function divideDecimals(a: Decimal, b: Decimal, scale = DIVISION_SCALE): Decimal | null {
  if (b.units === 0n) return null;
  const target = Math.max(0, Math.trunc(scale));
  const numerator = a.units * powerOfTen(b.scale + target);
  const denominator = b.units * powerOfTen(a.scale);
  return { units: divideRounding(numerator, denominator), scale: target };
}

/**
 * La cadena decimal, con tantos decimales como escala tenga. Es lo que se manda a la API,
 * asi que no lleva separador de miles ni simbolo: el formato para leer es otra cosa.
 */
export function decimalToString(value: Decimal): string {
  const negative = value.units < 0n;
  const digits = (negative ? -value.units : value.units).toString().padStart(value.scale + 1, '0');
  const cut = digits.length - value.scale;
  const whole = digits.slice(0, cut);
  const fraction = value.scale === 0 ? '' : `.${digits.slice(cut)}`;
  return `${negative ? '-' : ''}${whole}${fraction}`;
}

/** La cadena con exactamente [scale] decimales, redondeando si hace falta. */
export function decimalToFixed(value: Decimal, scale: number): string {
  return decimalToString(roundDecimal(value, scale));
}

/**
 * Quita los ceros de relleno de la derecha: `12.5000` queda en `12.5` y `45000.0000` en
 * `45000`. Es lo que se mete en un campo de texto, donde los ceros sobran.
 */
export function trimDecimal(value: Decimal): Decimal {
  let result = value;
  while (result.scale > 0 && result.units % TEN === 0n) {
    result = { units: result.units / TEN, scale: result.scale - 1 };
  }
  return result;
}
