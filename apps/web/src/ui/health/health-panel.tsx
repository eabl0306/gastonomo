import type { ReactElement } from 'react';

import type { HealthProbe } from '../../application/use-cases/check-api-health';

import { HealthChecksList } from './health-checks-list';
import { useHealthReport, type HealthViewState } from './use-health-report';

const LABELS = {
  loading: 'Consultando la API...',
  ok: 'Servicio operativo',
  degraded: 'Servicio degradado',
  unreachable: 'API inalcanzable',
} as const;

function ProbeDetail({ probe }: { readonly probe: HealthProbe }): ReactElement {
  if (probe.outcome === 'unreachable') {
    return (
      <p className="health-panel__reason" data-testid="health-reason">
        {probe.reason}
      </p>
    );
  }
  return (
    <>
      <p className="health-panel__version" data-testid="health-version">
        Version {probe.report.version}
      </p>
      <HealthChecksList checks={probe.report.checks} />
    </>
  );
}

function stateLabel(state: HealthViewState): string {
  if (state.kind === 'loading') return LABELS.loading;
  if (state.probe.outcome === 'unreachable') return LABELS.unreachable;
  return state.probe.report.status === 'ok' ? LABELS.ok : LABELS.degraded;
}

function stateTone(state: HealthViewState): string {
  if (state.kind === 'loading') return 'loading';
  if (state.probe.outcome === 'unreachable') return 'unreachable';
  return state.probe.report.status;
}

export function HealthPanel(): ReactElement {
  const { state, refresh } = useHealthReport();

  return (
    <section className="health-panel" aria-labelledby="health-panel-title">
      <h1 id="health-panel-title">Estado del servicio</h1>
      <p className="health-panel__status" data-testid="health-status" data-tone={stateTone(state)}>
        {stateLabel(state)}
      </p>
      {state.kind === 'settled' ? <ProbeDetail probe={state.probe} /> : null}
      <button type="button" data-testid="health-refresh" onClick={refresh}>
        Reintentar
      </button>
    </section>
  );
}
