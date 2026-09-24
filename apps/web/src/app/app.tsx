import type { ReactElement } from 'react';

import type { HealthGateway } from '../application/ports/health-gateway';
import { HealthGatewayProvider } from '../ui/health/health-gateway-provider';
import { HealthPanel } from '../ui/health/health-panel';

export interface AppProps {
  readonly gateway: HealthGateway;
}

export function App({ gateway }: AppProps): ReactElement {
  return (
    <HealthGatewayProvider gateway={gateway}>
      <HealthPanel />
    </HealthGatewayProvider>
  );
}
