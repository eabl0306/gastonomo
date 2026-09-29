import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { DictationGateway } from '../../application/ports/dictation-gateway';
import type { MovementDraft } from '../../application/ports/movement-gateway';
import type { SpeechListener, SpeechToText } from '../../application/ports/speech';
import type { DictationDraft } from '../../domain/dictation/draft';
import {
  createMemoryCurrencyGateway,
  createMemoryDictationGateway,
  createMemoryLoanGateway,
  createMemoryMovementGateway,
  createMemorySettingsGateway,
  fixedClock,
  silentSpeech,
} from '../../infrastructure/memory/in-memory-ledger';
import {
  SAMPLE_DRAFT,
  SAMPLE_MOVEMENTS,
  SAMPLE_TODAY,
} from '../../infrastructure/memory/sample-ledger';
import { AccountProvider } from '../account/account-context';
import { TodayProvider } from '../clock/today-context';

import { DictationModal, type DictationModalPorts } from './dictation-modal';

interface Sent {
  readonly draft: MovementDraft;
  readonly key: string;
}

function renderModal(
  overrides: Partial<DictationModalPorts> = {},
  sent: Sent[] = [],
  onSaved = vi.fn(),
): void {
  const ports: DictationModalPorts = {
    dictation: createMemoryDictationGateway(),
    speech: silentSpeech,
    movements: {
      ...createMemoryMovementGateway(),
      create: (draft, key) => {
        sent.push({ draft, key });
        return Promise.resolve({ ok: true, value: SAMPLE_MOVEMENTS[0]! });
      },
    },
    loans: createMemoryLoanGateway(),
    ...overrides,
  };
  render(
    <TodayProvider clock={fixedClock(SAMPLE_TODAY)}>
      <AccountProvider
        currencies={createMemoryCurrencyGateway()}
        settings={createMemorySettingsGateway()}
      >
        <DictationModal onClose={vi.fn()} onSaved={onSaved} ports={ports} />
      </AccountProvider>
    </TodayProvider>,
  );
}

describe('Dictar', () => {
  /** Sin reconocimiento no hay callejon sin salida: se arranca escribiendo, con su motivo. */
  it('un navegador sin dictado arranca en el modo escrito y lo explica', () => {
    renderModal();
    expect(screen.getByText(/Tu navegador no puede dictar/u)).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Lo que quieres registrar' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Dictar' })).not.toBeInTheDocument();
  });

  it('un ejemplo rellena el campo', async () => {
    renderModal();
    await userEvent.click(screen.getByRole('button', { name: 'Gasté 45 mil en almuerzo' }));

    expect(screen.getByRole('textbox', { name: 'Lo que quieres registrar' })).toHaveValue(
      'Gasté 45 mil en almuerzo',
    );
  });

  it('interpretar ensena el borrador con lo que se entendio', async () => {
    renderModal();
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Lo que quieres registrar' }),
      'Gasté 45 mil en almuerzo',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Interpretar' }));

    expect(await screen.findByText('Almuerzo')).toBeInTheDocument();
    expect(screen.getByText('Alimentación')).toBeInTheDocument();
    expect(screen.getByLabelText('$ 45.000')).toBeInTheDocument();
    expect(screen.getByText('22 sept 2026')).toBeInTheDocument();
  });

  /** Lo dice la API y no el cliente: asi el movil y la web no discrepan. */
  it('un borrador incompleto no se puede guardar y dice que falta', async () => {
    const incomplete: DictationDraft = { ...SAMPLE_DRAFT, amount: null, missing: ['amount'] };
    renderModal({ dictation: createMemoryDictationGateway(incomplete) });
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Lo que quieres registrar' }),
      'almuerzo',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Interpretar' }));

    expect(await screen.findByText('Falta el monto.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });

  /** El dictado no crea nada: guardar es una llamada aparte, con su clave de idempotencia. */
  it('guardar manda el movimiento con su clave', async () => {
    const sent: Sent[] = [];
    const onSaved = vi.fn();
    renderModal({}, sent, onSaved);
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Lo que quieres registrar' }),
      'Gasté 45 mil en almuerzo',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Interpretar' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Guardar' }));

    expect(sent).toHaveLength(1);
    expect(sent[0]?.draft).toMatchObject({
      kind: 'out',
      categoryId: 'c1',
      note: 'Almuerzo',
      occurredOn: SAMPLE_TODAY,
    });
    expect(sent[0]?.key).not.toBe('');
    expect(onSaved).toHaveBeenCalled();
  });

  it('una cuenta sin permiso recibe 404 y se ve como un fallo', async () => {
    const forbidden: DictationGateway = {
      parse: () => Promise.resolve({ ok: false, error: { kind: 'not-found' } }),
    };
    renderModal({ dictation: forbidden });
    await userEvent.type(screen.getByRole('textbox', { name: 'Lo que quieres registrar' }), 'algo');
    await userEvent.click(screen.getByRole('button', { name: 'Interpretar' }));

    expect(await screen.findByText('Algo no fue bien. Inténtalo de nuevo.')).toBeInTheDocument();
    // Lo tecleado no se pierde: quien reintente no tiene que volver a escribirlo.
    expect(screen.getByRole('textbox', { name: 'Lo que quieres registrar' })).toHaveValue('algo');
  });

  /** Los cinco fallos del microfono acaban en escribir, con su explicacion. */
  it('un microfono denegado cae al modo escrito con su motivo', async () => {
    const denied: SpeechToText = {
      available: true,
      listen: (_language: string, listener: SpeechListener) => {
        listener.onFailed('denied');
        return { stop: () => undefined, cancel: () => undefined };
      },
    };
    renderModal({ speech: denied });

    expect(await screen.findByText(/No diste permiso al micrófono/u)).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Lo que quieres registrar' })).toBeInTheDocument();
  });

  it('lo que se oye se ve mientras se habla, y al terminar se interpreta', async () => {
    let listener: SpeechListener | null = null;
    const speech: SpeechToText = {
      available: true,
      listen: (_language: string, handlers: SpeechListener) => {
        listener = handlers;
        return { stop: () => undefined, cancel: () => undefined };
      },
    };
    renderModal({ speech });

    listener!.onPartial('Gasté 45');
    expect(await screen.findByText('Gasté 45')).toBeInTheDocument();

    listener!.onFinished('Gasté 45 mil en almuerzo');
    expect(await screen.findByText('Almuerzo')).toBeInTheDocument();
  });
});
