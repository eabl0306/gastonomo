import type { ReactElement, ReactNode } from 'react';

import type { HealthGateway } from '../../application/ports/health-gateway';

import { HealthGatewayContext } from './health-gateway-context';

export interface HealthGatewayProviderProps {
  readonly gateway: HealthGateway;
  readonly children: ReactNode;
}

/** Punto de inyeccion: la interfaz solo conoce el puerto, nunca el adaptador. */
export function HealthGatewayProvider({
  gateway,
  children,
}: HealthGatewayProviderProps): ReactElement {
  return <HealthGatewayContext value={gateway}>{children}</HealthGatewayContext>;
}
