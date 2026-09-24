import { describe, expect, it } from 'vitest';

import { createHealthGateway, resolveApiUrl } from './composition-root';

describe('composition root', () => {
  it('usa la URL configurada', () => {
    expect(resolveApiUrl({ VITE_API_URL: 'http://api.gastonomo.test' })).toBe(
      'http://api.gastonomo.test',
    );
  });

  it('cae al puerto 8080 local cuando no hay configuracion', () => {
    expect(resolveApiUrl({})).toBe('http://localhost:8080');
    expect(resolveApiUrl({ VITE_API_URL: '   ' })).toBe('http://localhost:8080');
  });

  it('construye un adaptador que cumple el puerto', () => {
    const gateway = createHealthGateway({});
    expect(typeof gateway.readReadiness).toBe('function');
  });
});
