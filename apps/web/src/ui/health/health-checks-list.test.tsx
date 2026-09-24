import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { createHealthCheck } from '../../domain/health-check';

import { HealthChecksList } from './health-checks-list';

describe('HealthChecksList', () => {
  it('avisa cuando la API no declara ninguna comprobacion', () => {
    render(<HealthChecksList checks={[]} />);

    expect(screen.getByTestId('health-checks-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('health-checks')).not.toBeInTheDocument();
  });

  it('pinta una entrada por comprobacion con su estado y su latencia', () => {
    render(
      <HealthChecksList
        checks={[createHealthCheck('database', 'ok', 3), createHealthCheck('cache', 'error', 91)]}
      />,
    );

    const items = screen.getAllByTestId('health-check');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveAttribute('data-status', 'ok');
    expect(items[0]).toHaveTextContent('3 ms');
    expect(items[1]).toHaveAttribute('data-status', 'error');
    expect(screen.queryByTestId('health-checks-empty')).not.toBeInTheDocument();
  });
});
