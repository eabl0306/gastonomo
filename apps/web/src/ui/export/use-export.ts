import { useCallback, useEffect, useMemo, useState } from 'react';

import type {
  ExportFormat,
  ExportWriter,
  FileDownloader,
} from '../../application/ports/file-download';
import type { LoanGateway } from '../../application/ports/loan-gateway';
import type { MovementGateway } from '../../application/ports/movement-gateway';
import { loadPeriodMovements } from '../../application/use-cases/load-period-movements';
import type { Period } from '../../domain/calendar/period';
import { EXPORT_SETS, tableOf, type ExportInput, type ExportSet } from '../../domain/export/tables';
import type { Loan } from '../../domain/ledger/loan';
import type { Movement } from '../../domain/ledger/movement';

type ExportRange = 'all' | 'period';

export interface ExportPorts {
  readonly movements: MovementGateway;
  readonly loans: LoanGateway;
  readonly exports: ExportWriter;
  readonly downloader: FileDownloader;
}

export interface ExportState {
  readonly format: ExportFormat;
  readonly range: ExportRange;
  readonly sets: ReadonlySet<ExportSet>;
  readonly counts: Readonly<Record<ExportSet, number>>;
  readonly ready: boolean;
  setFormat: (format: ExportFormat) => void;
  setRange: (range: ExportRange) => void;
  toggle: (set: ExportSet) => void;
  run: () => void;
}

export interface ExportInputs {
  readonly ports: ExportPorts;
  readonly period: Period;
  readonly today: string;
  readonly display: ExportInput['display'];
  readonly rates: ExportInput['rates'];
}

/** El primer dia posible, para pedir "todo el historial" con el mismo filtro de siempre. */
const DAWN = '0001-01-01';

/** Lo que se ha traido de la API para exportar. */
interface Loaded {
  readonly all: readonly Movement[];
  readonly inPeriod: readonly Movement[];
  readonly loans: readonly Loan[];
}

const NOTHING: Loaded = { all: [], inPeriod: [], loans: [] };

/**
 * La pantalla de exportar.
 *
 * Trae **todo** al abrirse y filtra en local: el conteo de filas de cada conjunto tiene que
 * verse antes de exportar, y pedirlo otra vez al cambiar de rango haria parpadear los numeros.
 */
export function useExport(inputs: ExportInputs): ExportState {
  const [format, setFormat] = useState<ExportFormat>('xlsx');
  const [range, setRange] = useState<ExportRange>('all');
  const [sets, setSets] = useState<ReadonlySet<ExportSet>>(new Set(EXPORT_SETS));
  const loaded = useLoadedData(inputs);

  const input: ExportInput = useMemo(
    () => ({
      movements: range === 'all' ? loaded.all : loaded.inPeriod,
      loans: loaded.loans,
      display: inputs.display,
      rates: inputs.rates,
    }),
    [range, loaded, inputs.display, inputs.rates],
  );

  const toggle = useCallback((set: ExportSet) => {
    setSets((previous) => {
      const next = new Set(previous);
      if (next.has(set)) next.delete(set);
      else next.add(set);
      return next;
    });
  }, []);

  const chosen = useMemo(() => EXPORT_SETS.filter((set) => sets.has(set)), [sets]);
  const { ports, today } = inputs;

  const run = useCallback(() => {
    if (chosen.length === 0) return;
    const tables = chosen.map((set) => tableOf(set, input));
    ports.downloader.download(ports.exports.files(tables, format, today));
  }, [chosen, input, format, ports, today]);

  return {
    format,
    range,
    sets,
    counts: {
      movements: input.movements.length,
      loans: loaded.loans.length,
      payments: loaded.loans.reduce((total, loan) => total + loan.payments.length, 0),
    },
    ready: chosen.length > 0,
    setFormat,
    setRange,
    toggle,
    run,
  };
}

function useLoadedData({ ports, period, today }: ExportInputs): Loaded {
  const [loaded, setLoaded] = useState<Loaded>(NOTHING);
  useEffect(() => {
    let live = true;
    void Promise.all([
      loadPeriodMovements(ports.movements, { kind: 'range', from: DAWN, to: today }),
      loadPeriodMovements(ports.movements, period),
      ports.loans.list(),
    ]).then(([everything, inPeriod, listed]) => {
      if (!live) return;
      setLoaded({
        all: everything.ok ? everything.value : [],
        inPeriod: inPeriod.ok ? inPeriod.value : [],
        loans: listed.ok ? listed.value : [],
      });
    });
    return () => {
      live = false;
    };
  }, [ports, period, today]);
  return loaded;
}
