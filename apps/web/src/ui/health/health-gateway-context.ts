import { createContext, useContext } from 'react';

import type { HealthGateway } from '../../application/ports/health-gateway';

export const HealthGatewayContext = createContext<HealthGateway | null>(null);

const MISSING_PROVIDER =
  'Falta HealthGatewayProvider: la interfaz recibe el adaptador por inyeccion, no lo construye.';

export function useHealthGateway(): HealthGateway {
  const gateway = useContext(HealthGatewayContext);
  if (gateway === null) throw new Error(MISSING_PROVIDER);
  return gateway;
}
