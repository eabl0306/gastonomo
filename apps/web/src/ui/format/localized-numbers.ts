import { decimalToFixed, type Decimal } from '../../domain/money/decimal';

/**
 * Los formateadores numericos del idioma, cacheados por numero de decimales.
 *
 * El importe se le pasa a `Intl.NumberFormat` como **cadena**, nunca como `number`: un
 * importe de quince digitos con cuatro decimales no cabe en un float y perderia cifras, que
 * es justo lo que el contrato prohibe. `decimalToFixed` da esa cadena ya redondeada.
 */
export interface LocalizedNumbers {
  /** El separador decimal del idioma: "," en es-CO y "." en en-US. */
  readonly decimalSeparator: string;
  /** Exactamente [decimals] decimales, con agrupacion de miles. */
  fixed: (value: Decimal, decimals: number) => string;
  /** Hasta [decimals] decimales, sin rellenar con ceros. */
  upTo: (value: Decimal, decimals: number) => string;
  /** Agrupa un entero ya en cadena. Es lo que pinta el campo de importe mientras se teclea. */
  group: (integer: string) => string;
}

const SAMPLE = 1.1;

/**
 * `Intl.NumberFormat.format` acepta una cadena decimal desde ES2023 y la formatea sin pasar
 * por coma flotante, que es justo lo que hace falta aqui. Los tipos de TypeScript la
 * declaran como un literal de plantilla, asi que hay que decirle que la cadena vale: sin
 * esto habria que convertir a `number` y un importe grande perderia cifras.
 */
function formatString(formatter: Intl.NumberFormat, value: string): string {
  return formatter.format(value as unknown as number);
}

function separatorOf(locale: string): string {
  const parts = new Intl.NumberFormat(locale).formatToParts(SAMPLE);
  return parts.find((part) => part.type === 'decimal')?.value ?? '.';
}

export function localizedNumbers(locale: string): LocalizedNumbers {
  const cache = new Map<string, Intl.NumberFormat>();
  const formatter = (minimum: number, maximum: number): Intl.NumberFormat => {
    const key = `${String(minimum)}:${String(maximum)}`;
    const cached = cache.get(key);
    if (cached !== undefined) return cached;
    const created = new Intl.NumberFormat(locale, {
      minimumFractionDigits: minimum,
      maximumFractionDigits: maximum,
      useGrouping: true,
    });
    cache.set(key, created);
    return created;
  };
  return {
    decimalSeparator: separatorOf(locale),
    fixed: (value, decimals) =>
      formatString(formatter(decimals, decimals), decimalToFixed(value, decimals)),
    // Los ceros de relleno los quita el propio formateador con el minimo en cero.
    upTo: (value, decimals) =>
      formatString(formatter(0, decimals), decimalToFixed(value, decimals)),
    group: (integer) => formatString(formatter(0, 0), integer === '' ? '0' : integer),
  };
}
