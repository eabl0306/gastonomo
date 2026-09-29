import { z } from 'zod';

import type { CurrencyInfo } from '../../application/ports/currency-gateway';
import type { AppLanguage, UserSettings } from '../../domain/account/settings';
import { parseDay } from '../../domain/calendar/day';
import { parseCurrency, type CurrencyCode } from '../../domain/money/currency';
import { parseDecimal, signOf, type Decimal } from '../../domain/money/decimal';
import type { ExchangeRates } from '../../domain/money/exchange-rates';

const settingsSchema = z.object({
  displayCurrency: z.string(),
  language: z.enum(['es', 'en']),
});

const currencySchema = z.object({
  code: z.string(),
  name: z.string(),
  symbol: z.string(),
});

const currencyListSchema = z.object({ items: z.array(z.unknown()) });

const ratesSchema = z.object({
  base: z.string(),
  date: z.string(),
  status: z.enum(['ok', 'stale']),
  rates: z.record(z.string(), z.string()),
});

export function readSettings(body: unknown): UserSettings | null {
  const parsed = settingsSchema.safeParse(body);
  if (!parsed.success) return null;
  const displayCurrency = parseCurrency(parsed.data.displayCurrency);
  if (displayCurrency === null) return null;
  const language: AppLanguage = parsed.data.language;
  return { displayCurrency, language };
}

function readCurrency(raw: unknown): CurrencyInfo | null {
  const parsed = currencySchema.safeParse(raw);
  if (!parsed.success) return null;
  const code = parseCurrency(parsed.data.code);
  if (code === null) return null;
  return { code, name: parsed.data.name, symbol: parsed.data.symbol };
}

export function readCurrencyList(body: unknown): readonly CurrencyInfo[] | null {
  const parsed = currencyListSchema.safeParse(body);
  if (!parsed.success) return null;
  return parsed.data.items.map(readCurrency).filter((currency) => currency !== null);
}

/**
 * La tabla de tasas.
 *
 * Una tasa que no se lee o que no es positiva **se descarta**: con un cero en la tabla, la
 * conversion daria una division por cero o un importe de cero, y un balance de cero pesos
 * parece un dato y no un fallo.
 */
function readRates(raw: Readonly<Record<string, string>>): Map<CurrencyCode, Decimal> {
  const rates = new Map<CurrencyCode, Decimal>();
  for (const [rawCode, rawRate] of Object.entries(raw)) {
    const code = parseCurrency(rawCode);
    const rate = parseDecimal(rawRate);
    if (code !== null && rate !== null && signOf(rate) > 0) rates.set(code, rate);
  }
  return rates;
}

export function readExchangeRates(body: unknown): ExchangeRates | null {
  const parsed = ratesSchema.safeParse(body);
  if (!parsed.success) return null;
  const base = parseCurrency(parsed.data.base);
  if (base === null) return null;
  return {
    base,
    date: parseDay(parsed.data.date),
    status: parsed.data.status,
    rates: readRates(parsed.data.rates),
  };
}
