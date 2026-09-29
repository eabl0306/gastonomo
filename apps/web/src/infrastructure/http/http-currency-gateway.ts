import { success } from '../../application/ports/api-error';
import type { CurrencyGateway, CurrencyInfo } from '../../application/ports/currency-gateway';
import type { ExchangeRates } from '../../domain/money/exchange-rates';

import type { ApiClient } from './api-client';
import { readCurrencyList, readExchangeRates } from './catalog-dto';
import { malformed } from './malformed';

/** Catalogo de monedas y tabla de tasas contra la API. */
export function createHttpCurrencyGateway(client: ApiClient): CurrencyGateway {
  return {
    catalog: async () => {
      const outcome = await client.request('currencies');
      if (!outcome.ok) return outcome;
      const currencies = readCurrencyList(outcome.value.body);
      return currencies === null ? malformed<readonly CurrencyInfo[]>() : success(currencies);
    },

    rates: async () => {
      const outcome = await client.request('exchange-rates');
      if (!outcome.ok) return outcome;
      const rates = readExchangeRates(outcome.value.body);
      return rates === null ? malformed<ExchangeRates>() : success(rates);
    },
  };
}
