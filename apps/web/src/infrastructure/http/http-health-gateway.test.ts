import { describe, expect, it, vi } from 'vitest';

import { HealthGatewayError } from '../../application/ports/health-gateway';

import { createHttpHealthGateway } from './http-health-gateway';

const okBody = {
  status: 'ok',
  checks: [{ name: 'database', status: 'ok', latencyMs: 3 }],
  version: '0.1.0',
};

const degradedBody = {
  status: 'degraded',
  checks: [{ name: 'database', status: 'error', latencyMs: 812 }],
  version: '0.1.0',
};

/**
 * Devuelve un doble de fetch que conserva el tipo de mock, para poder mirar
 * despues con que argumentos exactos se llamo.
 */
function fetchReturning(body: unknown, status: number) {
  return vi.fn((_input: RequestInfo | URL, _init?: RequestInit) =>
    Promise.resolve(
      new Response(typeof body === 'string' ? body : JSON.stringify(body), {
        status,
        headers: { 'content-type': 'application/json' },
      }),
    ),
  );
}

function gatewayFor(
  body: unknown,
  status: number,
  baseUrl = 'http://api.test',
): ReturnType<typeof createHttpHealthGateway> {
  return createHttpHealthGateway({ baseUrl, fetchImpl: fetchReturning(body, status) });
}

describe('createHttpHealthGateway', () => {
  it('mapea una respuesta 200 al modelo de dominio', async () => {
    const report = await gatewayFor(okBody, 200).readReadiness();
    expect(report.status).toBe('ok');
    expect(report.version).toBe('0.1.0');
    expect(report.checks).toEqual([{ name: 'database', status: 'ok', latencyMs: 3 }]);
  });

  it('lee el cuerpo del 503 porque asi anuncia la API que esta degradada', async () => {
    const report = await gatewayFor(degradedBody, 503).readReadiness();
    expect(report.status).toBe('degraded');
  });

  it('quita las barras sobrantes de la URL base', async () => {
    const fetchImpl = fetchReturning(okBody, 200);
    const gateway = createHttpHealthGateway({ baseUrl: 'http://api.test///', fetchImpl });
    await gateway.readReadiness();
    expect(fetchImpl).toHaveBeenCalledWith('http://api.test/health/ready', expect.anything());
  });

  it('rechaza un estado HTTP que no sabe interpretar', async () => {
    await expect(gatewayFor(okBody, 500).readReadiness()).rejects.toThrow(HealthGatewayError);
  });

  it('rechaza un cuerpo que no es JSON', async () => {
    await expect(gatewayFor('<html></html>', 200).readReadiness()).rejects.toThrow(
      'La API devolvio algo que no es JSON',
    );
  });

  it('rechaza un JSON que no cumple el contrato', async () => {
    await expect(gatewayFor({ status: 'raro' }, 200).readReadiness()).rejects.toThrow(
      'La respuesta de salud no cumple el contrato',
    );
  });

  it('convierte un fallo de red en un error del puerto', async () => {
    const fetchImpl = vi.fn(() => Promise.reject(new TypeError('fallo de red')));
    const gateway = createHttpHealthGateway({ baseUrl: 'http://api.test', fetchImpl });
    await expect(gateway.readReadiness()).rejects.toThrow('No se pudo contactar con la API');
  });

  it('usa el fetch global cuando no se le inyecta ninguno', async () => {
    const globalFetch = fetchReturning(okBody, 200);
    vi.stubGlobal('fetch', globalFetch);

    const report = await createHttpHealthGateway({ baseUrl: 'http://api.test' }).readReadiness();

    expect(globalFetch).toHaveBeenCalledOnce();
    expect(report.status).toBe('ok');
    vi.unstubAllGlobals();
  });

  it('respeta el timeout que se le pasa en vez del suyo por defecto', async () => {
    const timeout = vi.spyOn(AbortSignal, 'timeout');
    const fetchImpl = fetchReturning(okBody, 200);

    await createHttpHealthGateway({
      baseUrl: 'http://api.test',
      fetchImpl,
      timeoutMs: 1234,
    }).readReadiness();

    expect(timeout).toHaveBeenCalledWith(1234);
    timeout.mockRestore();
  });

  it('sigue funcionando donde AbortSignal.timeout no existe', async () => {
    const original = AbortSignal.timeout;
    // El adaptador no puede caerse en un runtime sin AbortSignal.timeout:
    // simplemente se queda sin corte por tiempo.
    Reflect.deleteProperty(AbortSignal, 'timeout');
    const fetchImpl = fetchReturning(okBody, 200);

    try {
      const report = await createHttpHealthGateway({
        baseUrl: 'http://api.test',
        fetchImpl,
      }).readReadiness();

      expect(report.status).toBe('ok');
      expect(fetchImpl.mock.calls[0]?.[0]).toBe('http://api.test/health/ready');
      expect(fetchImpl.mock.calls[0]?.[1]).not.toHaveProperty('signal');
    } finally {
      AbortSignal.timeout = original;
    }
  });
});
