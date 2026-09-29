import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { exportWriter } from '../../infrastructure/export/serialize';
import {
  createMemoryCurrencyGateway,
  createMemoryDownloader,
  createMemoryLoanGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
} from '../../infrastructure/memory/in-memory-ledger';
import { SAMPLE_TODAY } from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';
import { PeriodProvider } from '../period/period-context';

import { ExportScreen } from './export-screen';

function renderScreen(): ReturnType<typeof createMemoryDownloader> {
  const downloader = createMemoryDownloader();
  render(
    <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
      <AccountProvider
        currencies={createMemoryCurrencyGateway()}
        settings={createMemorySettingsGateway()}
      >
        <PeriodProvider>
          <ExportScreen
            onBack={vi.fn()}
            ports={{
              movements: createMemoryMovementGateway(),
              loans: createMemoryLoanGateway(),
              exports: exportWriter,
              downloader,
            }}
          />
        </PeriodProvider>
      </AccountProvider>
    </TodayProvider>,
  );
  return downloader;
}

describe('Exportar datos', () => {
  it('dice cuantas filas trae cada conjunto', async () => {
    renderScreen();
    expect(await screen.findByText('4 filas')).toBeInTheDocument();
    expect(screen.getAllByText('3 filas')).toHaveLength(1);
    expect(screen.getByText('2 filas')).toBeInTheDocument();
  });

  it('Excel sale en un solo libro con una hoja por conjunto', async () => {
    const downloader = renderScreen();
    await screen.findByText('4 filas');

    await userEvent.click(screen.getByRole('button', { name: /Exportar a Excel/u }));
    expect(downloader.saved).toHaveLength(1);
    expect(downloader.saved[0]?.name).toBe(`gastonomo_${SAMPLE_TODAY}.xls`);
    expect(downloader.saved[0]?.content).toContain('ss:Name="Abonos"');
  });

  it('CSV sale en un fichero por conjunto', async () => {
    const downloader = renderScreen();
    await screen.findByText('4 filas');

    await userEvent.click(screen.getByRole('radio', { name: /CSV/u }));
    await userEvent.click(screen.getByRole('button', { name: /Exportar CSV/u }));

    expect(downloader.saved.map((file) => file.name)).toEqual([
      `gastonomo_movimientos_${SAMPLE_TODAY}.csv`,
      `gastonomo_prestamos_${SAMPLE_TODAY}.csv`,
      `gastonomo_abonos_${SAMPLE_TODAY}.csv`,
    ]);
  });

  it('sin ningun conjunto no hay nada que exportar', async () => {
    renderScreen();
    await screen.findByText('4 filas');

    for (const name of ['Movimientos', 'Préstamos', 'Abonos']) {
      await userEvent.click(screen.getByRole('button', { name: new RegExp(name, 'u') }));
    }
    expect(screen.getByRole('button', { name: /Exportar a Excel/u })).toBeDisabled();
  });
});
