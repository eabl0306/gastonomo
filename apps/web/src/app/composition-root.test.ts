import { describe, expect, it } from 'vitest';

import { createHealthGateway, resolveApiUrl } from './composition-root';

describe('composition root', () => {
  it('usa la URL configurada', () => {
    expect(resolveApiUrl({ VITE_API_URL: 'http://api.gastonomo.test' })).toBe(
      'http://api.gastonomo.test',
    );
  });

  /**
   * Sin configuracion la web pide a su propio origen, que es como se sirve
   * desplegado: Caddy pone la API bajo el dominio de la web. Poner un host aqui
   * seria hablar con otro origen, y la API no manda CORS a proposito.
   */
  it('sin configuracion pide a su propio origen', () => {
    expect(resolveApiUrl({})).toBe('');
    expect(resolveApiUrl({ VITE_API_URL: '   ' })).toBe('');
  });

  it('construye un adaptador que cumple el puerto', () => {
    const gateway = createHealthGateway({});
    expect(typeof gateway.readReadiness).toBe('function');
  });
});
