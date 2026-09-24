import type { ReactElement } from 'react';

import type { HealthCheck } from '../../domain/health-check';

export interface HealthChecksListProps {
  readonly checks: readonly HealthCheck[];
}

export function HealthChecksList({ checks }: HealthChecksListProps): ReactElement {
  if (checks.length === 0) {
    return <p data-testid="health-checks-empty">La API no declaro comprobaciones.</p>;
  }

  return (
    <ul className="health-checks" data-testid="health-checks">
      {checks.map((check) => (
        <li key={check.name} data-testid="health-check" data-status={check.status}>
          <span className="health-check__name">{check.name}</span>
          <span className="health-check__status">{check.status}</span>
          <span className="health-check__latency">{check.latencyMs} ms</span>
        </li>
      ))}
    </ul>
  );
}
