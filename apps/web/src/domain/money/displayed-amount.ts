import type { CurrencyCode } from './currency';
import { addDecimals, type Decimal } from './decimal';
import { convert, type ExchangeRates } from './exchange-rates';
import type { MonetaryAmount } from './monetary-amount';
import { moneyOf, zeroMoney, type Money } from './money';

/**
 * La regla central de multimoneda. Todo lo que se ensena, se suma o se exporta pasa por
 * aqui: totales, saldos de prestamo, graficos y exportaciones.
 *
 * Las cuatro ramas, en este orden:
 * 1. El registro ya esta en la moneda que se ensena: se usa su importe.
 * 2. La equivalencia guardada ya esta en esa moneda: se usa, y con ella la tasa que el
 *    usuario pudo editar a mano al registrar.
 * 3. Se convierte la equivalencia guardada con las tasas de hoy.
 * 4. Si eso no se puede, se convierte el importe original; y si tampoco, se devuelve la
 *    equivalencia en bruto antes que no ensenar nada.
 */
export function displayedAmount(
  value: MonetaryAmount,
  display: CurrencyCode,
  rates: ExchangeRates,
): Decimal {
  if (value.currency === display) return value.amount;
  if (value.baseCurrency === display) return value.base;
  return (
    convert(rates, value.base, value.baseCurrency, display) ??
    convert(rates, value.amount, value.currency, display) ??
    value.base
  );
}

/** Lo mismo, ya envuelto en [Money]. */
export function displayedMoney(
  value: MonetaryAmount,
  display: CurrencyCode,
  rates: ExchangeRates,
): Money {
  return moneyOf(displayedAmount(value, display, rates), display);
}

/** Suma una lista de registros en la moneda que se ensena. */
export function sumDisplayed(
  values: readonly MonetaryAmount[],
  display: CurrencyCode,
  rates: ExchangeRates,
): Money {
  return values.reduce(
    (total, value) =>
      moneyOf(addDecimals(total.amount, displayedAmount(value, display, rates)), display),
    zeroMoney(display),
  );
}
