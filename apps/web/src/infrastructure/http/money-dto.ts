import { z } from 'zod';

import { parseCurrency, type CurrencyCode } from '../../domain/money/currency';
import { decimalToString, parseDecimal } from '../../domain/money/decimal';
import { singleAmount, type MonetaryAmount } from '../../domain/money/monetary-amount';

/**
 * Los cuatro campos de dinero que lleva todo registro.
 *
 * Los importes viajan como **cadena** a proposito: es lo que evita que un cliente los meta
 * en un numero de coma flotante y pierda centavos. Aqui se leen a decimal exacto.
 *
 * `base` y `baseCurrency` pueden faltar los dos juntos: entonces la equivalencia es el
 * propio importe, que es el caso del registro hecho en la moneda principal.
 */
export const moneyFieldsSchema = z.object({
  amount: z.string(),
  currency: z.string(),
  base: z.string().optional(),
  baseCurrency: z.string().optional(),
});

export type MoneyFieldsDto = z.infer<typeof moneyFieldsSchema>;

/**
 * Pasa los cuatro campos al dominio, o devuelve null si alguno no es legible.
 *
 * Null y no un importe a cero: un movimiento cuyo importe no se entiende no es un
 * movimiento de cero pesos, y sumarlo como tal falsearia el balance en silencio.
 */
export function readMoney(dto: MoneyFieldsDto): MonetaryAmount | null {
  const amount = parseDecimal(dto.amount);
  const currency = parseCurrency(dto.currency);
  if (amount === null || currency === null) return null;
  if (dto.base === undefined || dto.baseCurrency === undefined) {
    return singleAmount(amount, currency);
  }
  const base = parseDecimal(dto.base);
  const baseCurrency: CurrencyCode | null = parseCurrency(dto.baseCurrency);
  if (base === null || baseCurrency === null) return null;
  return { amount, currency, base, baseCurrency };
}

/**
 * Los cuatro campos, listos para el cuerpo de un POST.
 *
 * Los importes salen como cadena, con su escala tal cual: `decimalToString` no recorta ni
 * rellena ceros, asi que lo que se manda es exactamente lo que se leyo.
 */
export function writeMoney(value: MonetaryAmount): MoneyFieldsDto {
  return {
    amount: decimalToString(value.amount),
    currency: value.currency,
    base: decimalToString(value.base),
    baseCurrency: value.baseCurrency,
  };
}
